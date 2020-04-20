package com.hedera.hashgraph.identity;

import com.google.gson.annotations.SerializedName;

/**
 * The mode in which DID document is submitted.
 * It can be a plain document encoded with Base64 or encoded and then encrypted with custom encryption algorithm.
 */
public enum DidDocumentPublishingMode {
  @SerializedName("plain")
  PLAIN,

  @SerializedName("encrypted")
  ENCRYPTED
}
