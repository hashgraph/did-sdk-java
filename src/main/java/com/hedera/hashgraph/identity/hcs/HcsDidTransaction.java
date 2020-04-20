package com.hedera.hashgraph.identity.hcs;

import com.google.common.base.Strings;
import com.hedera.hashgraph.identity.DidDocumentOperation;
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
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import java.time.Instant;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * The DID document creation, update or deletion transaction.
 * Builds a correct {@link HcsDidMessage} and send it to HCS DID topic.
 */
public class HcsDidTransaction {
  private final ConsensusTopicId topicId;
  private final DidDocumentOperation operation;

  private String didDocument;
  private UnaryOperator<byte[]> encrypter;
  private BiFunction<byte[], Instant, byte[]> decrypter;
  private UnaryOperator<byte[]> signer;
  private Function<ConsensusMessageSubmitTransaction, Transaction> buildTransactionFunction;
  private Consumer<HcsDidPlainMessage> receiver;
  private Consumer<Throwable> errorHandler;
  private MirrorSubscriptionHandle subscriptionHandle;
  private boolean executed;

  /**
   * Instantiates a new transaction object.
   *
   * @param operation The operation to be performed on a DID document.
   * @param topicId   The HCS DID topic ID where message will be submitted.
   */
  protected HcsDidTransaction(final DidDocumentOperation operation, final ConsensusTopicId topicId) {
    this.executed = false;
    this.operation = operation;
    this.topicId = topicId;
  }

  /**
   * Builds and submits the DID message to appnet's DID topic.
   *
   * @param  client       The hedera network client.
   * @param  mirrorClient The hedera mirror node client.
   * @return              Transaction ID.
   */
  public TransactionId execute(final Client client, final MirrorClient mirrorClient) {
    new Validator().checkValidationErrors("HcsDidTransaction execution failed: ", v -> validate(v));

    byte[] messageContent = HcsDidMessageBuilder.fromDidDocument(didDocument)
        .setOperation(operation)
        .buildAndSign(encrypter, signer);

    if (receiver != null) {
      subscriptionHandle = new MirrorConsensusTopicQuery()
          .setTopicId(topicId)
          .subscribe(mirrorClient,
              resp -> handleDidDocumentFromMirrorNode(resp, messageContent),
              err -> handleError(err));
    }

    ConsensusMessageSubmitTransaction tx = new ConsensusMessageSubmitTransaction()
        .setTopicId(topicId)
        .setMessage(messageContent);

    TransactionId transactionId = null;
    try {
      transactionId = buildTransactionFunction
          .apply(tx)
          .execute(client);
      executed = true;
    } catch (HederaNetworkException | HederaStatusException e) {
      handleError(e);
    }

    return transactionId;
  }

  /**
   * Handles the error.
   * If external error handler is defined, passes the error there, otherwise raises RuntimeException.
   *
   * @param  err     The error.
   * @throws Runtime exception with the given error in case external error handler is not defined.
   */
  private void handleError(final Throwable err) {
    // Ignore Status cancelled error. It happens on unsubscribe.
    if (err instanceof StatusRuntimeException
        && Code.CANCELLED.equals(((StatusRuntimeException) err).getStatus().getCode())) {
      return;
    }

    if (errorHandler != null) {
      errorHandler.accept(err);
    } else {
      throw new RuntimeException(err);
    }
  }

  /**
   * Handles incoming DID messages from DID Topic on a mirror node.
   *
   * @param resp            Response message coming from the mirror node for the DID topic.
   * @param originalMessage Original message sent to the DID topic.
   */
  protected void handleDidDocumentFromMirrorNode(final MirrorConsensusTopicResponse resp,
      final byte[] originalMessage) {
    // Get only the message that matches the original one and ignore all others.
    if (!Arrays.equals(originalMessage, resp.message)) {
      return;
    }

    // Stop receiving other messages immediately after matching.
    if (subscriptionHandle != null) {
      subscriptionHandle.unsubscribe();
    }

    HcsDidPlainMessage msg = HcsDidMessage.fromDidTopicMirrorResponse(resp).toPlainDidMessage(decrypter);

    if (!msg.isValid(topicId)) {
      handleError(new IllegalStateException("Message received from the mirror node is invalid."));
      return;
    }

    // Pass received DID and DID document to the receiver for appnet's processing
    receiver.accept(msg);
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
   * Defines a handler for errors when they happen during execution.
   *
   * @param  handler The error handler.
   * @return         This transaction instance.
   */
  public HcsDidTransaction onError(final Consumer<Throwable> handler) {
    this.errorHandler = handler;
    return this;
  }

  /**
   * Defines decryption function that decrypts submitted the DID and DID document after consensus is reached.
   * Decryption function must accept a byte array of encrypted message and an Instant that is its consensus timestamp,
   *
   * @param  decrypter The decrypter to use.
   * @return           This transaction instance.
   */
  public HcsDidTransaction onDecrypt(final BiFunction<byte[], Instant, byte[]> decrypter) {
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
  public HcsDidTransaction onDidDocumentReceived(final Consumer<HcsDidPlainMessage> receiver) {
    this.receiver = receiver;
    return this;
  }

  /**
   * Runs validation logic.
   *
   * @param validator The errors validator.
   */
  protected void validate(final Validator validator) {
    validator.require(!executed, "This transaction has already been executed.");
    validator.require(!Strings.isNullOrEmpty(didDocument), "DID document is mandatory.");
    validator.require(signer != null, "Signing function with DID root key is missing.");
    validator.require(buildTransactionFunction != null, "Transaction builder is missing.");
    validator.require((encrypter != null && decrypter != null) || (encrypter == null && decrypter == null),
        "Either both encrypter and decrypter must be specified or none.");
  }
}
