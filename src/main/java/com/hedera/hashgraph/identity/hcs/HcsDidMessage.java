package com.hedera.hashgraph.identity.hcs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.hedera.hashgraph.identity.DidDocumentOperation;
import com.hedera.hashgraph.identity.DidDocumentPublishingMode;

/**
 * The DID document message submitted to appnet's DID Topic.
 */
public class HcsDidMessage {

  @Expose(serialize = true, deserialize = true)
  private DidDocumentOperation didOperation;

  @Expose(serialize = true, deserialize = true)
  private DidDocumentPublishingMode mode;

  @Expose(serialize = true, deserialize = true)
  private String did;

  @Expose(serialize = true, deserialize = true)
  private String didDocumentBase64;

  @Expose(serialize = true, deserialize = true)
  private String signature;

  /**
   * Converts this DID document operation message into JSON string.
   *
   * @return The JSON representation of this message.
   */
  public String toJson() {
    Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().create();
    return gson.toJson(this);
  }

  /**
   * Converts a DID message JSON string into object instance.
   *
   * @param  json DID message as JSON string.
   * @return      The {@link HcsDidMessage}.
   */
  public static HcsDidMessage fromJson(final String json) {
    Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    return gson.fromJson(json, HcsDidMessage.class);
  }

  public DidDocumentOperation getDidOperation() {
    return didOperation;
  }

  public DidDocumentPublishingMode getMode() {
    return mode;
  }

  public String getDid() {
    return did;
  }

  public String getDidDocumentBase64() {
    return didDocumentBase64;
  }

  public String getSignature() {
    return signature;
  }

  public void setDidOperation(final DidDocumentOperation didOperation) {
    this.didOperation = didOperation;
  }

  public void setMode(final DidDocumentPublishingMode mode) {
    this.mode = mode;
  }

  public void setDid(final String did) {
    this.did = did;
  }

  public void setDidDocumentBase64(final String didDocumentBase64) {
    this.didDocumentBase64 = didDocumentBase64;
  }

  public void setSignature(final String signature) {
    this.signature = signature;
  }
}
