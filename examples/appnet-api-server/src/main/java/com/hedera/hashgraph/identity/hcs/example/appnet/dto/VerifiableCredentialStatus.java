package com.hedera.hashgraph.identity.hcs.example.appnet.dto;

import com.google.gson.annotations.Expose;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcMessage;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcOperation;
import java.time.Instant;

/**
 * A status of verifiable credential in VC registry.
 * Includes consensus timestamp of the latest valid message.
 */
public class VerifiableCredentialStatus {

  @Expose
  private HcsVcOperation operation;

  @Expose
  private String credentialHash;

  @Expose
  private Instant timestamp;

  @Expose
  private Instant updated;

  /**
   * Creates a new instance of this DTO from the given VC message and its consensus timestamp.
   *
   * @param  message            VC message received from the mirror node.
   * @param  consensusTimestamp Consensus timestamp of the message.
   * @return                    The DTO instance.
   */
  public static VerifiableCredentialStatus fromHcsVcMessage(final HcsVcMessage message,
      final Instant consensusTimestamp) {
    VerifiableCredentialStatus result = new VerifiableCredentialStatus();

    result.setOperation(message.getOperation());
    result.setCredentialHash(message.getCredentialHash());
    result.setTimestamp(message.getTimestamp());
    result.setUpdated(consensusTimestamp);

    return result;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(final Instant timestamp) {
    this.timestamp = timestamp;
  }

  public Instant getUpdated() {
    return updated;
  }

  public void setUpdated(final Instant updated) {
    this.updated = updated;
  }

  public HcsVcOperation getOperation() {
    return operation;
  }

  public String getCredentialHash() {
    return credentialHash;
  }

  public void setOperation(final HcsVcOperation operation) {
    this.operation = operation;
  }

  public void setCredentialHash(final String credentialHash) {
    this.credentialHash = credentialHash;
  }
}
