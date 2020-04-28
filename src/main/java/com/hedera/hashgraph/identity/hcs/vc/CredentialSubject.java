package com.hedera.hashgraph.identity.hcs.vc;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public abstract class CredentialSubject {
  @Expose(serialize = true, deserialize = true)
  @SerializedName(HcsVcDocumentJsonProperties.ID)
  protected String id;

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

}
