package com.hedera.hashgraph.identity.hcs.vc;

import com.google.gson.annotations.SerializedName;

/**
 * The operation type to be performed on the DID document.
 */
public enum HcsVcOperation {
  @SerializedName("issue")
  ISSUE,

  @SerializedName("revoke")
  REVOKE,

  @SerializedName("suspend")
  SUSPEND,

  @SerializedName("resume")
  RESUME;
}
