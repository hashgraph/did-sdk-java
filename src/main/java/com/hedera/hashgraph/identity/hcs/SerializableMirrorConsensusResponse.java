package com.hedera.hashgraph.identity.hcs;

import com.hedera.hashgraph.sdk.TopicMessage;
import java.io.Serializable;
import java.util.Arrays;
import org.threeten.bp.Instant;

/**
 * This class is a serializable copy of the MirrorConsensusResponse class from the Java SDK.
 */
public class SerializableMirrorConsensusResponse implements Serializable {

  private static final long serialVersionUID = 1L;

  public final Instant consensusTimestamp;

  public final byte[] message;

  public final byte[] runningHash;

  public final long sequenceNumber;

  SerializableMirrorConsensusResponse(final TopicMessage response) {
    this.consensusTimestamp = response.consensusTimestamp;
    this.message = response.contents;
    this.runningHash = response.runningHash;
    this.sequenceNumber = response.sequenceNumber;
  }

  // TODO: Use a standard debug serialization
  @Override
  public String toString() {
    return "ConsensusMessage{"
            + "consensusTimestamp=" + consensusTimestamp
            + ", message=" + Arrays.toString(message)
            + ", runningHash=" + Arrays.toString(runningHash)
            + ", sequenceNumber=" + sequenceNumber
            + '}';
  }
}



