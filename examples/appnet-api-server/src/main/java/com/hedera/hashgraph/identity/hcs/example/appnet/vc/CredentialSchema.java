package com.hedera.hashgraph.identity.hcs.example.appnet.vc;

import com.google.gson.annotations.Expose;

/**
 * An object representing a simple credential schema reference in a verifiable document.
 */
public class CredentialSchema {
  @Expose
  public String id;

  @Expose
  public String type;

  public CredentialSchema(final String id, final String type) {
    this.type = type;
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public String getType() {
    return type;
  }
}
