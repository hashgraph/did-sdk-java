package com.hedera.hashgraph.identity.hcs.did;

import com.google.common.base.Strings;
import com.hedera.hashgraph.identity.DidMethodOperation;
import com.hedera.hashgraph.identity.hcs.MessageEnvelope;
import com.hedera.hashgraph.identity.hcs.MessageListener;
import com.hedera.hashgraph.identity.hcs.MessageTransaction;
import com.hedera.hashgraph.identity.utils.Validator;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import java.util.function.UnaryOperator;

/**
 * The DID document creation, update or deletion transaction.
 * Builds a correct {@link HcsDidMessage} and send it to HCS DID topic.
 */
public class HcsDidTransaction extends MessageTransaction<HcsDidMessage> {
  private final DidMethodOperation operation;
  private String didDocument;

  /**
   * Instantiates a new transaction object.
   *
   * @param operation The operation to be performed on a DID document.
   * @param topicId   The HCS DID topic ID where message will be submitted.
   */
  public HcsDidTransaction(final DidMethodOperation operation, final ConsensusTopicId topicId) {
    super(topicId);
    this.operation = operation;
  }

  /**
   * Instantiates a new transaction object from a message that was already prepared.
   *
   * @param topicId The HCS DID topic ID where message will be submitted.
   * @param message The message envelope.
   */
  public HcsDidTransaction(final MessageEnvelope<HcsDidMessage> message, final ConsensusTopicId topicId) {
    super(topicId, message);
    this.operation = null;
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

  @Override
  protected void validate(final Validator validator) {
    super.validate(validator);
    // if message was not provided, check if we have all information to build it.
    validator.require(!Strings.isNullOrEmpty(didDocument) || message != null, "DID document is mandatory.");
    validator.require(operation != null || message != null, "DID method operation is not defined.");
  }

  @Override
  protected MessageEnvelope<HcsDidMessage> buildMessage() {
    return HcsDidMessage.fromDidDocumentJson(didDocument, operation);
  }

  @Override
  protected MessageListener<HcsDidMessage> provideTopicListener(final ConsensusTopicId topicIdToListen) {
    return new HcsDidTopicListener(topicIdToListen);
  }

  @Override
  protected UnaryOperator<HcsDidMessage> provideMessageEncrypter(final UnaryOperator<byte[]> encryptionFunction) {
    return HcsDidMessage.getEncrypter(encryptionFunction);
  }
}
