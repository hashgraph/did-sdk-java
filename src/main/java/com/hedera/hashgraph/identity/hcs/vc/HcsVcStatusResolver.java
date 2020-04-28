package com.hedera.hashgraph.identity.hcs.vc;

import com.hedera.hashgraph.identity.hcs.MessageEnvelope;
import com.hedera.hashgraph.identity.hcs.MessageListener;
import com.hedera.hashgraph.identity.hcs.MessageResolver;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PublicKey;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

/**
 * Resolves the VC status from Hedera network.
 */
public class HcsVcStatusResolver extends MessageResolver<HcsVcMessage> {

  /**
   * A function providing a collection of public keys accepted for a given credential hash.
   * If the function is not supplied, the listener will not validate signatures.
   */
  private Function<String, Collection<Ed25519PublicKey>> publicKeysProvider;

  /**
   * Instantiates a new status resolver for the given VC topic.
   *
   * @param topicId The HCS VC topic ID.
   */
  public HcsVcStatusResolver(final ConsensusTopicId topicId) {
    this(topicId, null);
  }

  /**
   * Instantiates a new status resolver for the given VC topic with signature validation.
   *
   * @param topicId            The VC consensus topic ID.
   * @param publicKeysProvider Provider of a public keys acceptable for a given VC hash.
   */
  public HcsVcStatusResolver(final ConsensusTopicId topicId,
      final Function<String, Collection<Ed25519PublicKey>> publicKeysProvider) {
    super(topicId);
    this.publicKeysProvider = publicKeysProvider;
  }

  /**
   * Adds a credential hash to resolve its status.
   *
   * @param  credentialHash The credential hash string.
   * @return                This resolver instance.
   */
  public HcsVcStatusResolver addCredentialHash(final String credentialHash) {
    if (credentialHash != null) {
      results.put(credentialHash, null);
    }
    return this;
  }

  /**
   * Adds multiple VC hashes to resolve.
   *
   * @param  hashes The set of VC hash strings.
   * @return        This resolver instance.
   */
  public HcsVcStatusResolver addCredentialHashes(final Set<String> hashes) {
    if (hashes != null) {
      hashes.forEach(d -> addCredentialHash(d));
    }

    return this;
  }

  @Override
  protected boolean matchesSearchCriteria(final HcsVcMessage message) {
    return results.containsKey(message.getCredentialHash());
  }

  @Override
  protected MessageListener<HcsVcMessage> supplyMessageListener() {
    return new HcsVcTopicListener(topicId, publicKeysProvider);
  }

  @Override
  protected void processMessage(final MessageEnvelope<HcsVcMessage> envelope) {
    HcsVcMessage message = envelope.open();

    // Skip messages that are older than the once collected or if we already have a REVOKED message
    MessageEnvelope<HcsVcMessage> existing = results.get(message.getCredentialHash());
    if (existing != null
        && (envelope.getConsensusTimestamp().isBefore(existing.getConsensusTimestamp())
            || (HcsVcOperation.REVOKE.equals(existing.open().getOperation())
                && !HcsVcOperation.REVOKE.equals(message.getOperation())))) {
      return;
    }

    // Add valid message to the results
    results.put(message.getCredentialHash(), envelope);
  }
}
