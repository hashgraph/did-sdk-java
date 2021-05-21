package com.hedera.hashgraph.identity.hcs.vc;

import com.hedera.hashgraph.identity.hcs.MessageEnvelope;
import com.hedera.hashgraph.identity.hcs.MessageListener;
import com.hedera.hashgraph.identity.hcs.MessageTransaction;
import com.hedera.hashgraph.identity.hcs.did.HcsDidMessage;
import com.hedera.hashgraph.identity.utils.Validator;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.TopicId;
import java.util.function.UnaryOperator;
import java8.util.Lists;
import org.apache.commons.lang3.StringUtils;

/**
 * The DID document creation, update or deletion transaction.
 * Builds a correct {@link HcsDidMessage} and send it to HCS DID topic.
 */
public class HcsVcTransaction extends MessageTransaction<HcsVcMessage> {
  private final HcsVcOperation operation;
  private final String credentialHash;
  private final PublicKey signerPublicKey;

  /**
   * Instantiates a new transaction object.
   *
   * @param topicId         The HCS VC topic ID where message will be submitted.
   * @param operation       The operation to be performed on a verifiable credential.
   * @param credentialHash  The hash of a credential.
   * @param signerPublicKey Public key of the signer of this operation.
   */
  public HcsVcTransaction(final TopicId topicId, final HcsVcOperation operation,
                          final String credentialHash, final PublicKey signerPublicKey) {
    super(topicId);
    this.operation = operation;
    this.credentialHash = credentialHash;
    this.signerPublicKey = signerPublicKey;
  }

  /**
   * Instantiates a new transaction object from a message that was already prepared.
   *
   * @param topicId         The HCS VC topic ID where message will be submitted.
   * @param message         The message envelope.
   * @param signerPublicKey Public key of the signer of this operation.
   */
  public HcsVcTransaction(final TopicId topicId, final MessageEnvelope<HcsVcMessage> message,
                          final PublicKey signerPublicKey) {
    super(topicId, message);
    this.signerPublicKey = signerPublicKey;
    this.operation = null;
    this.credentialHash = null;
  }

  @Override
  protected void validate(final Validator validator) {
    super.validate(validator);

    // If built message was provided credential hash and operation are not mandatory
    validator.require(!StringUtils.isEmpty(credentialHash) || message != null,
            "Verifiable credential hash is null or empty.");
    validator.require(operation != null || message != null, "Operation on verifiable credential is not defined.");
  }

  @Override
  protected MessageEnvelope<HcsVcMessage> buildMessage() {
    return HcsVcMessage.fromCredentialHash(credentialHash, operation);
  }

  @Override
  protected MessageListener<HcsVcMessage> provideTopicListener(final TopicId topicIdToListen) {
    return new HcsVcTopicListener(topicIdToListen, s -> Lists.of(signerPublicKey));
  }

  @Override
  protected UnaryOperator<HcsVcMessage> provideMessageEncrypter(final UnaryOperator<byte[]> encryptionFunction) {
    return HcsVcMessage.getEncrypter(encryptionFunction);
  }
}
