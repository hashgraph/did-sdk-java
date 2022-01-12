package com.hedera.hashgraph.identity.hcs.vc;

import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hedera.hashgraph.identity.utils.JsonUtils;
import org.bitcoinj.core.Base58;
import org.threeten.bp.Instant;

/**
 * The part of the VC document that is used for hash calculation.
 */
public abstract class HcsDocumentHashBase {
  @Expose
  @SerializedName(HcsVcDocumentJsonProperties.ID)
  protected String id;

  @Expose
  @SerializedName(HcsVcDocumentJsonProperties.TYPE)
  protected List<String> type;

  @Expose
  @SerializedName(HcsVcDocumentJsonProperties.ISSUER)
  @JsonAdapter(IssuerTypeAdapterFactory.class)
  protected Issuer issuer;

  @Expose
  @SerializedName(HcsVcDocumentJsonProperties.ISSUANCE_DATE)
  protected Instant issuanceDate;

  /**
   * Creates a new document instance.
   */
  protected HcsDocumentHashBase() {
    this.type = Lists.newArrayList(HcsVcDocumentJsonProperties.VERIFIABLE_CREDENTIAL_TYPE);
  }

  /**
   * Constructs a credential hash that uniquely identifies this verifiable credential.
   * This is not a credential ID, but a hash composed of the properties included in HcsVcDocumentHashBase class
   * (excluding issuer name).
   * Credential hash is used to find the credential on Hedera VC registry.
   * Due to the nature of the VC document the hash taken from the base mandatory fields in this class
   * and shall produce a unique constant.
   * W3C specification defines ID field of a verifiable credential as not mandatory, however Hedera requires issuers to
   * define this property for each VC.
   *
   * @return The credential hash uniquely identifying this verifiable credential.
   */
  public final String toCredentialHash() {
    LinkedHashMap<String, Object> map = new LinkedHashMap<>();
    map.put(HcsVcDocumentJsonProperties.ID, this.id);
    map.put(HcsVcDocumentJsonProperties.TYPE, this.type);
    map.put(HcsVcDocumentJsonProperties.ISSUER, this.issuer.getId());
    map.put(HcsVcDocumentJsonProperties.ISSUANCE_DATE, this.issuanceDate);

    Map<String, Object> customHashableProperties = getCustomHashableFieldsHook();
    map.putAll(customHashableProperties);

    String json = JsonUtils.getGson().toJson(map);
    byte[] hash = Hashing.sha256().hashBytes(json.getBytes(StandardCharsets.UTF_8)).asBytes();

    return Base58.encode(hash);
  }

  /**
   * It's used to add any other field to the credential hash computation.
   *
   * @return A map containing the elements to be added in the credential hash computation.
   */
  protected Map<String, Object> getCustomHashableFieldsHook() {
    return new LinkedHashMap<>();
  }

  public void setId(final String id) {
    this.id = id;
  }

  public void setType(final List<String> type) {
    this.type = type;
  }

  public void setIssuer(final Issuer issuer) {
    this.issuer = issuer;
  }

  public void setIssuanceDate(final Instant issuanceDate) {
    this.issuanceDate = issuanceDate;
  }

  public String getId() {
    return id;
  }
}
