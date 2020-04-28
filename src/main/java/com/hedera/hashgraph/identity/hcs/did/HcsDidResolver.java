package com.hedera.hashgraph.identity.hcs.did;

import com.hedera.hashgraph.identity.DidMethodOperation;
import com.hedera.hashgraph.identity.hcs.MessageEnvelope;
import com.hedera.hashgraph.identity.hcs.MessageListener;
import com.hedera.hashgraph.identity.hcs.MessageResolver;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import java.util.Set;

/**
 * Resolves the DID from Hedera network.
 */
public class HcsDidResolver extends MessageResolver<HcsDidMessage> {

  /**
   * Instantiates a new DID resolver for the given DID topic.
   *
   * @param topicId The HCS DID topic ID.
   */
  public HcsDidResolver(final ConsensusTopicId topicId) {
    super(topicId);
  }

  /**
   * Adds a DID to resolve.
   *
   * @param  did The DID string.
   * @return     This resolver instance.
   */
  public HcsDidResolver addDid(final String did) {
    if (did != null) {
      results.put(did, null);
    }
    return this;
  }

  /**
   * Adds multiple DIDs to resolve.
   *
   * @param  dids The set of DID strings.
   * @return      This resolver instance.
   */
  public HcsDidResolver addDids(final Set<String> dids) {
    if (dids != null) {
      dids.forEach(d -> addDid(d));
    }

    return this;
  }

  @Override
  protected boolean matchesSearchCriteria(final HcsDidMessage message) {
    return results.containsKey(message.getDid());
  }

  @Override
  protected void processMessage(final MessageEnvelope<HcsDidMessage> envelope) {
    HcsDidMessage message = envelope.open();
    // Also skip messages that are older than the once collected or if we already have a DELETE message
    MessageEnvelope<HcsDidMessage> existing = results.get(message.getDid());
    if (existing != null
        && (envelope.getConsensusTimestamp().isBefore(existing.getConsensusTimestamp())
            || (DidMethodOperation.DELETE.equals(existing.open().getOperation())
                && !DidMethodOperation.DELETE.equals(message.getOperation())))) {
      return;
    }

    // Preserve created and updated timestamps
    message.setUpdated(envelope.getConsensusTimestamp());
    if (DidMethodOperation.CREATE.equals(message.getOperation())) {
      message.setCreated(envelope.getConsensusTimestamp());
    } else if (existing != null) {
      message.setCreated(existing.open().getCreated());
    }

    // Add valid message to the results
    results.put(message.getDid(), envelope);
  }

  @Override
  protected MessageListener<HcsDidMessage> supplyMessageListener() {
    return new HcsDidTopicListener(topicId);
  }
}
