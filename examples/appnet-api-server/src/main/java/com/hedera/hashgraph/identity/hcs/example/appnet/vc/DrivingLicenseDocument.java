package com.hedera.hashgraph.identity.hcs.example.appnet.vc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcDocumentBase;
import com.hedera.hashgraph.identity.utils.JsonUtils;
import java.util.LinkedHashMap;
import java.util.UUID;

/**
 * A simple, manually constructed example of a driving license verifiable credential document.
 */
public class DrivingLicenseDocument extends HcsVcDocumentBase<DrivingLicense> {
  private static final String DOCUMENT_TYPE = "DrivingLicense";
  private static final String EXAMPLE_ID_PREFIX = "https://example.appnet.com/driving-license/";
  private static final String JSON_PROPERTY_CREDENTIAL_SUBJECT = "credentialSubject";
  private static final String JSON_PROPERTY_PROOF = "proof";
  private static final String[] JSON_PROPERTIES_ORDER = { "@context", "id", "type", "credentialSchema",
      "credentialSubject", "issuer", "issuanceDate", "proof" };

  public static final String CREDENTIAL_SCHEMA_TYPE = "JsonSchemaValidator2018";

  @Expose
  private CredentialSchema credentialSchema;

  @Expose
  private Ed25519CredentialProof proof;

  /**
   * Creates a new verifiable credential document instance with predefined types and auto-generated ID.
   */
  public DrivingLicenseDocument() {
    super();
    addType(DOCUMENT_TYPE);

    // Generate a unique identifier for this credential
    this.id = EXAMPLE_ID_PREFIX + UUID.randomUUID();
  }

  /**
   * Note: this is a manual implementation of order JSON items.
   * In a real-world application it is recommended to use a JSON-LD compatible library to handle normalization.
   * However at this point the only available one in Java support JSON-LD version 1.0, but 1.1 is required by W3C
   * Verifiable Credentials.
   * 
   * @param  withoutProof Will skip 'proof' attribute if True.
   * @return              A normalized JSON string representation of this document.
   */
  public String toNormalizedJson(final boolean withoutProof) {
    Gson gson = JsonUtils.getGson();

    // First turn to normal JSON
    JsonObject root = gson.toJsonTree(this).getAsJsonObject();
    // Then put JSON properties in ordered map
    LinkedHashMap<String, JsonElement> map = new LinkedHashMap<>();

    JsonArray credentialSubjectsArray = new JsonArray();
    for (String property : JSON_PROPERTIES_ORDER) {
      if (JSON_PROPERTY_CREDENTIAL_SUBJECT.equals(property) && getCredentialSubject() != null) {
        for (DrivingLicense dl : getCredentialSubject()) {
          credentialSubjectsArray.add(dl.toNormalizedJsonElement());
        }

        map.put(property, credentialSubjectsArray);
      } else if (JSON_PROPERTY_PROOF.equals(property) && withoutProof) {
        continue;
      } else if (root.has(property)) {
        map.put(property, root.get(property));
      }
    }
    // Turn map to JSON
    return gson.toJson(map);
  }

  public CredentialSchema getCredentialSchema() {
    return credentialSchema;
  }

  public void setCredentialSchema(final CredentialSchema credentialSchema) {
    this.credentialSchema = credentialSchema;
  }

  public Ed25519CredentialProof getProof() {
    return proof;
  }

  public void setProof(final Ed25519CredentialProof proof) {
    this.proof = proof;
  }
}
