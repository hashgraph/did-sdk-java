package com.hedera.hashgraph.identity.hcs.vc;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.hedera.hashgraph.identity.hcs.MessageEnvelope;
import com.hedera.hashgraph.identity.hcs.MessageListener;
import com.hedera.hashgraph.identity.hcs.MessageTransaction;
import com.hedera.hashgraph.identity.hcs.did.HcsDidMessage;
import com.hedera.hashgraph.identity.utils.Validator;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PublicKey;
import java.util.function.UnaryOperator;

/**
 * The DID document creation, update or deletion transaction.
 * Builds a correct {@link HcsDidMessage} and send it to HCS DID topic.
 */
public class HcsVcTransaction extends MessageTransaction<HcsVcMessage> {
  private final HcsVcOperation operation;
  private final String credentialHash;
  private final Ed25519PublicKey signerPublicKey;

  /**
   * Instantiates a new transaction object.
   *
   * @param topicId         The HCS VC topic ID where message will be submitted.
   * @param operation       The operation to be performed on a verifiable credential.
   * @param credentialHash  The hash of a credential.
   * @param signerPublicKey Public key of the signer of this operation.
   */
  public HcsVcTransaction(final ConsensusTopicId topicId, final HcsVcOperation operation,
      final String credentialHash, final Ed25519PublicKey signerPublicKey) {
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
  public HcsVcTransaction(final ConsensusTopicId topicId, final MessageEnvelope<HcsVcMessage> message,
      final Ed25519PublicKey signerPublicKey) {
    super(topicId, message);
    this.signerPublicKey = signerPublicKey;
    this.operation = null;
    this.credentialHash = null;
  }

  @Override
  protected void validate(final Validator validator) {
    super.validate(validator);

    // If built message was provided credential hash and operation are not mandatory
    validator.require(!Strings.isNullOrEmpty(credentialHash) || message != null,
        "Verifiable credential hash is null or empty.");
    validator.require(operation != null || message != null, "Operation on verifiable credential is not defined.");
  }

  @Override
  protected MessageEnvelope<HcsVcMessage> buildMessage() {
    return HcsVcMessage.fromCredentialHash(credentialHash, operation);
  }

  @Override
  protected MessageListener<HcsVcMessage> provideTopicListener(final ConsensusTopicId topicIdToListen) {
    return new HcsVcTopicListener(topicIdToListen, s -> Lists.newArrayList(signerPublicKey));
  }

  @Override
  protected UnaryOperator<HcsVcMessage> provideMessageEncrypter(final UnaryOperator<byte[]> encryptionFunction) {
    return HcsVcMessage.getEncrypter(encryptionFunction);
  }
}
