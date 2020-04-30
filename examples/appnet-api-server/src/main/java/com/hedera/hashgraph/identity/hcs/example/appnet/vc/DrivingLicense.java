package com.hedera.hashgraph.identity.hcs.example.appnet.vc;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.hedera.hashgraph.identity.hcs.vc.CredentialSubject;
import com.hedera.hashgraph.identity.utils.JsonUtils;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * A simple, manual example of a credential subject in verifiable credential - a driving license.
 */
public class DrivingLicense extends CredentialSubject {
  private static final String[] JSON_PROPERTIES_ORDER = { "id", "firstName", "lastName", "drivingLicenseCategories" };

  @Expose
  private final String firstName;

  @Expose
  private final String lastName;

  @Expose
  private final List<String> drivingLicenseCategories;

  /**
   * Builds a new driving license credential.
   *
   * @param did                      Driving license owner's DID.
   * @param firstName                Owner's first name.
   * @param lastName                 Owner's last name.
   * @param drivingLicenseCategories A list of categories granted to the owner.
   */
  public DrivingLicense(final String did, final String firstName, final String lastName,
      final List<String> drivingLicenseCategories) {
    this.id = did;
    this.firstName = firstName;
    this.lastName = lastName;
    this.drivingLicenseCategories = drivingLicenseCategories;
  }

  /**
   * Note: this is a manual implementation of ordered JSON items for this simple non-nested schema.
   * In a real-world application it is recommended to use a JSON-LD compatible library to handle normalization.
   * However at this point the only available one in Java support JSON-LD version 1.0, but 1.1 is required by W3C
   * Verifiable Credentials.
   *
   * @return A normalized JSON Element representation of this document.
   */
  public JsonElement toNormalizedJsonElement() {
    Gson gson = JsonUtils.getGson();

    // First turn to normal JSON
    JsonObject root = gson.toJsonTree(this).getAsJsonObject();
    // Then put JSON properties in ordered map
    LinkedHashMap<String, JsonElement> map = new LinkedHashMap<>();

    for (String property : JSON_PROPERTIES_ORDER) {
      if (root.has(property)) {
        map.put(property, root.get(property));
      }
    }
    // Turn map to JSON
    return gson.toJsonTree(map);
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public List<String> getDrivingLicenseCategories() {
    return drivingLicenseCategories;
  }
}
