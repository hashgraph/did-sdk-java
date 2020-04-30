package com.hedera.hashgraph.identity.hcs.example.appnet.vc;

import com.google.gson.annotations.Expose;
import java.time.Instant;

/**
 * A simple DTO for manually constructed example of a linked data proof.
 */
public class LinkedDataProof {
  @Expose
  private String type;
  @Expose
  private String creator;
  @Expose
  private Instant created;
  @Expose
  private String domain;
  @Expose
  private String nonce;
  @Expose
  private String proofPurpose;
  @Expose
  private String verificationMethod;
  @Expose
  private String jws;

  public String getCreator() {
    return creator;
  }

  public void setCreator(final String creator) {
    this.creator = creator;
  }

  public Instant getCreated() {
    return created;
  }

  public void setCreated(final Instant created) {
    this.created = created;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(final String domain) {
    this.domain = domain;
  }

  public String getNonce() {
    return nonce;
  }

  public void setNonce(final String nonce) {
    this.nonce = nonce;
  }

  public String getProofPurpose() {
    return proofPurpose;
  }

  public void setProofPurpose(final String proofPurpose) {
    this.proofPurpose = proofPurpose;
  }

  public String getVerificationMethod() {
    return verificationMethod;
  }

  public void setVerificationMethod(final String verificationMethod) {
    this.verificationMethod = verificationMethod;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public String getJws() {
    return jws;
  }

  public void setJws(final String jws) {
    this.jws = jws;
  }
}
