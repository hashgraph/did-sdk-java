package com.hedera.hashgraph.identity.hcs.did;

import com.hedera.hashgraph.identity.hcs.MessageEnvelope;
import com.hedera.hashgraph.identity.hcs.MessageListener;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.mirror.MirrorConsensusTopicResponse;
import java.time.Instant;
import java.util.function.BiFunction;

/**
 * A listener of confirmed {@link HcsDidMessage} messages from a DID topic.
 * Messages are received from a given mirror node, parsed and validated.
 */
public class HcsDidTopicListener extends MessageListener<HcsDidMessage> {

  /**
   * Creates a new instance of a DID topic listener for the given consensus topic.
   * By default, invalid messages are ignored and errors are not.
   *
   * @param didTopicId The DID consensus topic ID.
   */
  public HcsDidTopicListener(final ConsensusTopicId didTopicId) {
    super(didTopicId);
  }

  @Override
  protected MessageEnvelope<HcsDidMessage> extractMessage(final MirrorConsensusTopicResponse response) {
    MessageEnvelope<HcsDidMessage> result = null;
    try {
      result = MessageEnvelope.fromMirrorResponse(response, HcsDidMessage.class);
    } catch (Exception err) {
      handleError(err);
    }

    return result;
  }

  @Override
  protected boolean isMessageValid(final MessageEnvelope<HcsDidMessage> envelope,
      final MirrorConsensusTopicResponse response) {
    try {
      BiFunction<HcsDidMessage, Instant, HcsDidMessage> msgDecrypter = decrypter == null ? null
          : HcsDidMessage.getDecrypter(decrypter);

      HcsDidMessage message = envelope.open(msgDecrypter);
      if (message == null) {
        reportInvalidMessage(response, "Empty message received when opening envelope");
        return false;
      }

      if (!envelope.isSignatureValid(e -> message.extractDidRootKey())) {
        reportInvalidMessage(response, "Signature validation failed");
        return false;
      }

      if (!message.isValid(topicId)) {
        reportInvalidMessage(response, "Message content validation failed.");
        return false;
      }

      return true;
    } catch (Exception err) {
      handleError(err);
      reportInvalidMessage(response, "Exception while validating message: " + err.getMessage());
      return false;
    }

  }
}
