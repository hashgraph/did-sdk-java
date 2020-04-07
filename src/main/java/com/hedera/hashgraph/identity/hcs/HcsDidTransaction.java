package com.hedera.hashgraph.identity.hcs;

import com.google.common.base.Strings;
import com.hedera.hashgraph.identity.DidDocumentBase;
import com.hedera.hashgraph.identity.DidDocumentOperation;
import com.hedera.hashgraph.identity.DidDocumentPublishingMode;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.HederaNetworkException;
import com.hedera.hashgraph.sdk.HederaStatusException;
import com.hedera.hashgraph.sdk.Transaction;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.consensus.ConsensusMessageSubmitTransaction;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.mirror.MirrorClient;
import com.hedera.hashgraph.sdk.mirror.MirrorConsensusTopicQuery;
import com.hedera.hashgraph.sdk.mirror.MirrorConsensusTopicResponse;
import com.hedera.hashgraph.sdk.mirror.MirrorSubscriptionHandle;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * The DID document creation, update or deletion transaction.
 * Builds a correct {@link HcsDidMessage} and send it to HCS DID topic.
 */
public class HcsDidTransaction extends TransactionValidator {
  private String didDocument;
  private UnaryOperator<byte[]> encrypter;
  private UnaryOperator<byte[]> decrypter;
  private UnaryOperator<byte[]> signer;
  private final ConsensusTopicId topicId;
  private Function<ConsensusMessageSubmitTransaction, Transaction> buildTransactionFunction;
  private BiConsumer<String, String> receiver;
  private final HcsDidMessage message;
  private MirrorSubscriptionHandle subscriptionHandle;
  private boolean executed;

  /**
   * Instantiates a new transaction object.
   *
   * @param operation The operation to be performed on a DID document.
   * @param topicId   The HCS DID topic ID where message will be submitted.
   */
  protected HcsDidTransaction(final DidDocumentOperation operation, final ConsensusTopicId topicId) {
    this.message = new HcsDidMessage();
    this.message.setDidOperation(operation);
    this.executed = false;
    this.topicId = topicId;
  }

  // TODO provide other execution methods as in standard Hedera SDK
  /**
   * Builds and submits the DID message to appnet's DID topic.
   *
   * @param  client                 The hedera network client.
   * @param  mirrorClient           The hedera mirror node client.
   * @return                        Transaction ID.
   * @throws HederaStatusException  In case querying Hedera File Service fails.
   * @throws HederaNetworkException In case of querying Hedera File Service fails due to transport calls.
   */
  public TransactionId execute(final Client client, final MirrorClient mirrorClient)
      throws HederaNetworkException, HederaStatusException {
    checkValidationErrors("HcsDidTransaction execution failed: ");

    Encoder base64Enc = Base64.getEncoder();
    DidDocumentBase didDocumentBase = DidDocumentBase.fromJson(didDocument);

    message.setDid(didDocumentBase.getId());
    message.setDidDocumentBase64(base64Enc.encodeToString(didDocument.getBytes(StandardCharsets.UTF_8)));

    byte[] signature = signer.apply(didDocument.getBytes(StandardCharsets.UTF_8));
    message.setSignature(base64Enc.encodeToString(signature));

    // TODO validate did and signature of the message.

    if (encrypter != null) {
      String encryptedDid = base64Enc
          .encodeToString(encrypter.apply(message.getDid().getBytes(StandardCharsets.UTF_8)));
      String encryptedDoc = base64Enc
          .encodeToString(encrypter.apply(message.getDidDocumentBase64().getBytes(StandardCharsets.UTF_8)));

      message.setDid(encryptedDid);
      message.setDidDocumentBase64(encryptedDoc);
      message.setMode(DidDocumentPublishingMode.ENCRYPTED);
    } else {
      message.setMode(DidDocumentPublishingMode.PLAIN);
    }

    if (receiver != null) {
      subscriptionHandle = new MirrorConsensusTopicQuery()
          .setTopicId(topicId)
          .subscribe(mirrorClient,
              resp -> handleDidDocumentFromMirrorNode(resp),
              err -> {
                // TODO investigate io.grpc.StatusRuntimeException: CANCELLED: unsubscribed error on unsubscribe() call
                if (!(err instanceof StatusRuntimeException)) {
                  throw new RuntimeException(err);
                }

                StatusRuntimeException e = (StatusRuntimeException) err;
                if (!Status.CANCELLED.equals(e.getStatus())) {
                  throw e;
                }
              }
              );
    }

    ConsensusMessageSubmitTransaction tx = new ConsensusMessageSubmitTransaction()
        .setTopicId(topicId)
        .setMessage(message.toJson());

    TransactionId transactionId = buildTransactionFunction
        .apply(tx)
        .execute(client);

    executed = true;

    return transactionId;
  }

  /**
   * Handles incoming DID messages from DID Topic on a mirror node.
   *
   * @param resp Response message coming from the mirror node for the DID topic.
   */
  protected void handleDidDocumentFromMirrorNode(final MirrorConsensusTopicResponse resp) {
    String messageAsString = new String(resp.message, StandardCharsets.UTF_8);

    // Get only the message with the signature calculated above and ignore all others.
    if (Strings.isNullOrEmpty(messageAsString) || !messageAsString.contains(message.getSignature())) {
      return;
    }

    // Stop receiving other messages immediately after matching.
    if (subscriptionHandle != null) {
      subscriptionHandle.unsubscribe();
    }

    Decoder base64Dec = Base64.getDecoder();

    HcsDidMessage confirmedMsg = HcsDidMessage.fromJson(messageAsString);
    String confirmedDid = confirmedMsg.getDid();
    String confirmedDidDoc = null;
    byte[] confirmedDidDocBytes = confirmedMsg.getDidDocumentBase64()
        .getBytes(StandardCharsets.ISO_8859_1);

    // If message was encrypted decrypt the DID and its document.
    if (DidDocumentPublishingMode.ENCRYPTED.equals(confirmedMsg.getMode()) && decrypter != null) {
      byte[] encryptedDid = base64Dec.decode(confirmedDid.getBytes(StandardCharsets.ISO_8859_1));
      byte[] decryptedDid = decrypter.apply(encryptedDid);
      confirmedDid = new String(decryptedDid, StandardCharsets.UTF_8);

      byte[] decryptedDidDoc = decrypter.apply(base64Dec.decode(confirmedDidDocBytes));
      confirmedDidDoc = new String(decryptedDidDoc, StandardCharsets.UTF_8);
    } else {
      confirmedDidDoc = new String(base64Dec.decode(confirmedDidDocBytes), StandardCharsets.UTF_8);
    }

    // Pass received DID and DID document to the receiver for appnet's processing
    receiver.accept(confirmedDid, confirmedDidDoc);
  }

  /**
   * Sets a DID document as JSON string that will be submitted to HCS.
   *
   * @param  didDocument The didDocument to be published.
   * @return             This transaction instance.
   */
  public HcsDidTransaction setDidDocument(final String didDocument) {
    this.didDocument = didDocument;
    return this;
  }

  /**
   * Defines encryption function that encrypts the DID and DID document before submission.
   *
   * @param  encrypter The encrypter to use.
   * @return           This transaction instance.
   */
  public HcsDidTransaction onEncrypt(final UnaryOperator<byte[]> encrypter) {
    this.encrypter = encrypter;
    return this;
  }

  /**
   * Defines decryption function that decrypts submitted the DID and DID document after consensus is reached.
   *
   * @param  decrypter The decrypter to use.
   * @return           This transaction instance.
   */
  public HcsDidTransaction onDecrypt(final UnaryOperator<byte[]> decrypter) {
    this.decrypter = decrypter;
    return this;
  }

  /**
   * Defines a function that signs DID document with DID root key.
   *
   * @param  signer The signing function to set.
   * @return        This transaction instance.
   */
  public HcsDidTransaction signDidDocument(final UnaryOperator<byte[]> signer) {
    this.signer = signer;
    return this;
  }

  /**
   * Sets {@link ConsensusMessageSubmitTransaction} parameters, builds and signs it without executing it.
   * Topic ID and transaction message content are already set in the incoming transaction.
   *
   * @param  builderFunction The transaction builder function.
   * @return                 This transaction instance.
   */
  public HcsDidTransaction buildAndSignTransaction(
      final Function<ConsensusMessageSubmitTransaction, Transaction> builderFunction) {
    this.buildTransactionFunction = builderFunction;
    return this;
  }

  /**
   * Handles event from a mirror node when DID document was submitted and consensus was reached.
   *
   * @param  receiver The receiver handling incoming DID document message.
   * @return          This transaction instance.
   */
  public HcsDidTransaction onDidDocumentReceived(final BiConsumer<String, String> receiver) {
    this.receiver = receiver;
    return this;
  }

  @Override
  protected void validate() {
    require(!executed, "This transaction has already been executed.");
    require(!Strings.isNullOrEmpty(didDocument), "DID document is mandatory.");
    require(signer != null, "Signing function with DID root key is missing.");
    require(buildTransactionFunction != null, "Transaction builder is missing.");
    require((encrypter != null && decrypter != null) || (encrypter == null && decrypter == null),
        "Either both encrypter and decrypter must be specified or none.");
  }
}
