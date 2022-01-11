package com.hedera.hashgraph.identity;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.hedera.hashgraph.identity.hcs.did.HcsDidRootKey;
import com.hedera.hashgraph.identity.hcs.did.HcsDidRootZkKey;
import com.hedera.hashgraph.identity.utils.JsonUtils;
import java.util.Iterator;

/**
 * The base for a DID document generation in JSON-LD format.
 * DID documents according to W3C draft specification must be compatible with JSON-LD version 1.1 Up until now there is
 * no Java implementation library of JSON-LD version 1.1. For that reason this object represents only the most basic and
 * mandatory attributes from the DID specification and Hedera HCS DID method specification point of view. Applications
 * shall extend it with any DID document properties or custom properties they require.
 */
public class DidDocumentBase {

  @Expose(deserialize = false)
  @SerializedName(DidDocumentJsonProperties.CONTEXT)
  protected String context;

  @Expose
  @SerializedName(DidDocumentJsonProperties.ID)
  protected String id;

  @Expose(serialize = false, deserialize = false)
  protected HcsDidRootKey didRootKey;

  @Expose(serialize = false, deserialize = false)
  protected HcsDidRootZkKey didRootZkKey;

  /**
   * Creates a new DID Document for the specified DID string.
   *
   * @param did The DID string.
   */
  public DidDocumentBase(final String did) {
    this.id = did;
    this.context = DidSyntax.DID_DOCUMENT_CONTEXT;
  }

  /**
   * Converts a DID document in JSON format into a {@link DidDocumentBase} object.
   * Please note this conversion respects only the fields of the base DID document. All other fields are ignored.
   *
   * @param json The DID document as JSON string.f
   * @return The {@link DidDocumentBase}.
   */
  public static DidDocumentBase fromJson(final String json) {
    Gson gson = JsonUtils.getGson();

    DidDocumentBase result;

    try {
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();
      result = gson.fromJson(root, DidDocumentBase.class);

      if (root.has(DidDocumentJsonProperties.PUBLIC_KEY)) {
        for (JsonElement jsonElement : root.getAsJsonArray(DidDocumentJsonProperties.PUBLIC_KEY)) {
          JsonObject publicKeyObj = jsonElement.getAsJsonObject();
          if (publicKeyObj.has(DidDocumentJsonProperties.ID)
                  && publicKeyObj.get(DidDocumentJsonProperties.ID).getAsString()
                  .equals(result.getId() + HcsDidRootKey.DID_ROOT_KEY_NAME)) {
            result.setDidRootKey(gson.fromJson(publicKeyObj, HcsDidRootKey.class));
          }

          if (publicKeyObj.has(DidDocumentJsonProperties.ID)
                  && publicKeyObj.get(DidDocumentJsonProperties.ID).getAsString()
                  .equals(result.getId() + HcsDidRootZkKey.DID_ROOT_KEY_NAME)) {
            result.setDidRootZkKey(gson.fromJson(publicKeyObj, HcsDidRootZkKey.class));
            break;
          }
        }
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("Given JSON string is not a valid DID document", e);
    }

    return result;
  }

  /**
   * Converts this DID document into JSON string.
   *
   * @return The JSON representation of this document.
   */
  public String toJson() {
    Gson gson = JsonUtils.getGson();

    JsonElement jsonElement = gson.toJsonTree(this);
    JsonObject rootObject = jsonElement.getAsJsonObject();

    addDidRootKeyToPublicKeys(rootObject);
    addDidRootKeyToAuthentication(rootObject);

    return gson.toJson(jsonElement);
  }

  /**
   * Adds #did-root-key to authentication section of the DID document if it is not defined.
   *
   * @param rootObject The root object of DID Document as JsonObject.
   */
  private void addDidRootKeyToAuthentication(final JsonObject rootObject) {
    if (!rootObject.has(DidDocumentJsonProperties.AUTHENTICATION)) {
      rootObject.add(DidDocumentJsonProperties.AUTHENTICATION, new JsonArray());
    }

    JsonElement authElement = rootObject.get(DidDocumentJsonProperties.AUTHENTICATION);
    if (authElement.isJsonArray() && authElement.getAsJsonArray().size() == 0) {
      authElement.getAsJsonArray().add(didRootKey.getId());
    }
  }

  /**
   * Adds a #did-root-key to public keys of the DID document.
   *
   * @param rootObject The root object of DID Document as JsonObject.
   */
  protected void addDidRootKeyToPublicKeys(final JsonObject rootObject) {
    JsonArray publicKeys;
    if (rootObject.has(DidDocumentJsonProperties.PUBLIC_KEY)) {
      publicKeys = rootObject.getAsJsonArray(DidDocumentJsonProperties.PUBLIC_KEY);
    } else {
      publicKeys = new JsonArray(1);
      rootObject.add(DidDocumentJsonProperties.PUBLIC_KEY, publicKeys);
    }

    publicKeys.add(JsonUtils.getGson().toJsonTree(didRootKey));

    if (didRootZkKey != null) {
      publicKeys.add(JsonUtils.getGson().toJsonTree(didRootZkKey));
    }
  }

  public String getContext() {
    return context;
  }

  public String getId() {
    return id;
  }

  public HcsDidRootKey getDidRootKey() {
    return didRootKey;
  }

  public void setDidRootKey(final HcsDidRootKey didRootKey) {
    this.didRootKey = didRootKey;
  }

  public HcsDidRootZkKey getDidRootZkKey() {
    return didRootZkKey;
  }

  public void setDidRootZkKey(final HcsDidRootZkKey didRootZkKey) {
    this.didRootZkKey = didRootZkKey;
  }
}
