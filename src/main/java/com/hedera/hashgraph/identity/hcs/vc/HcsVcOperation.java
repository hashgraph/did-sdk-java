package com.hedera.hashgraph.identity.hcs.vc;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * The operation type to be performed on the DID document.
 */
public enum HcsVcOperation implements Serializable {
  @SerializedName("issue")
  ISSUE,

  @SerializedName("revoke")
  REVOKE,

  @SerializedName("suspend")
  SUSPEND,

  @SerializedName("resume")
  RESUME;
}
