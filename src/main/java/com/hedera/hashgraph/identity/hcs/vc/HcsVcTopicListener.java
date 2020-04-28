package com.hedera.hashgraph.identity.hcs.vc;

import com.hedera.hashgraph.identity.hcs.MessageEnvelope;
import com.hedera.hashgraph.identity.hcs.MessageListener;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PublicKey;
import com.hedera.hashgraph.sdk.mirror.MirrorConsensusTopicResponse;
import java.time.Instant;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A listener of confirmed {@link HcsVcMessage} messages from a VC topic.
 * Messages are received from a given mirror node, parsed and validated.
 */
public class HcsVcTopicListener extends MessageListener<HcsVcMessage> {

  /**
   * A function providing a collection of public keys accepted for a given credential hash.
   * If the function is not supplied, the listener will not validate signatures.
   */
  private Function<String, Collection<Ed25519PublicKey>> publicKeysProvider;

  /**
   * Creates a new instance of a VC topic listener for the given consensus topic.
   * By default, invalid messages are ignored and errors are not.
   * Listener without a public key provider will not validate message signatures.
   *
   * @param vcTopicId The VC consensus topic ID.
   */
  public HcsVcTopicListener(final ConsensusTopicId vcTopicId) {
    this(vcTopicId, null);
  }

  /**
   * Creates a new instance of a VC topic listener for the given consensus topic.
   * By default, invalid messages are ignored and errors are not.
   *
   * @param vcTopicId          The VC consensus topic ID.
   * @param publicKeysProvider Provider of a public keys acceptable for a given VC hash.
   */
  public HcsVcTopicListener(final ConsensusTopicId vcTopicId,
      final Function<String, Collection<Ed25519PublicKey>> publicKeysProvider) {
    super(vcTopicId);
    this.publicKeysProvider = publicKeysProvider;
  }

  @Override
  protected MessageEnvelope<HcsVcMessage> extractMessage(final MirrorConsensusTopicResponse response) {
    MessageEnvelope<HcsVcMessage> result = null;
    try {
      result = MessageEnvelope.fromMirrorResponse(response, HcsVcMessage.class);
    } catch (Exception err) {
      handleError(err);
    }

    return result;
  }

  @Override
  protected boolean isMessageValid(final MessageEnvelope<HcsVcMessage> envelope,
      final MirrorConsensusTopicResponse response) {
    try {
      BiFunction<HcsVcMessage, Instant, HcsVcMessage> msgDecrypter = decrypter == null ? null
          : HcsVcMessage.getDecrypter(decrypter);

      HcsVcMessage message = envelope.open(msgDecrypter);
      if (message == null) {
        reportInvalidMessage(response, "Empty message received when opening envelope");
        return false;
      }

      if (!message.isValid()) {
        reportInvalidMessage(response, "Message content validation failed.");
        return false;
      }

      // Validate signature only if public key provider has been supplied.
      if (publicKeysProvider != null && !isSignatureAccepted(envelope)) {
        reportInvalidMessage(response, "Signature validation failed");
        return false;
      }

      return true;
    } catch (Exception err) {
      handleError(err);
      reportInvalidMessage(response, "Exception while validating message: " + err.getMessage());
      return false;
    }
  }

  /**
   * Checks if the signature on the envelope is accepted by any public key supplied for the credential hash.
   *
   * @param  envelope The message envelope.
   * @return          True if signature is accepted, false otherwise.
   */
  private boolean isSignatureAccepted(final MessageEnvelope<HcsVcMessage> envelope) {
    if (publicKeysProvider == null) {
      return false;
    }

    Collection<Ed25519PublicKey> acceptedKeys = publicKeysProvider.apply(envelope.open().getCredentialHash());
    if (acceptedKeys == null || acceptedKeys.isEmpty()) {
      return false;
    }

    for (Ed25519PublicKey publicKey : acceptedKeys) {
      if (envelope.isSignatureValid(e -> publicKey)) {
        return true;
      }
    }

    return false;
  }
}
