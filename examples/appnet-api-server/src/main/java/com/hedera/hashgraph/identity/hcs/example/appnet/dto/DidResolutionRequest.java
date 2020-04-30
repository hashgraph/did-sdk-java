package com.hedera.hashgraph.identity.hcs.example.appnet.dto;

import com.google.gson.annotations.Expose;

/**
 * DTO that represents a request body sent from clients asking for DID resolution.
 */
public class DidResolutionRequest {

  @Expose
  private String did;

  public String getDid() {
    return did;
  }

  public void setDid(final String did) {
    this.did = did;
  }
}
