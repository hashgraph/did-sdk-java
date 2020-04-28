package com.hedera.hashgraph.identity.hcs.vc;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Issuer {
  @Expose(serialize = true, deserialize = true)
  @SerializedName(HcsVcDocumentJsonProperties.ID)
  protected String id;

  @Expose(serialize = true, deserialize = true)
  protected String name;

  public Issuer(final String id, final String name) {
    this.id = id;
    this.name = name;
  }

  public Issuer(final String id) {
    this(id, null);
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
