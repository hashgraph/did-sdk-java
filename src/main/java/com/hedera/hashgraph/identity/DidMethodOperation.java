package com.hedera.hashgraph.identity;

import com.google.gson.annotations.SerializedName;

/**
 * The operation type to be performed on the DID document.
 */
public enum DidMethodOperation {
  @SerializedName("create")
  CREATE,

  @SerializedName("update")
  UPDATE,

  @SerializedName("delete")
  DELETE,
}
