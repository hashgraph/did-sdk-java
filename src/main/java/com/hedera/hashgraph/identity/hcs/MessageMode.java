package com.hedera.hashgraph.identity.hcs;

import com.google.gson.annotations.SerializedName;

/**
 * The mode in which HCS message with DID document is submitted.
 * It can be a plain with document encoded with Base64 or encrypted with custom encryption algorithm.
 */
public enum MessageMode {
  @SerializedName("plain")
  PLAIN,

  @SerializedName("encrypted")
  ENCRYPTED
}
