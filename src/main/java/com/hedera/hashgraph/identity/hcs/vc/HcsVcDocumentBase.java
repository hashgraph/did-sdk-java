package com.hedera.hashgraph.identity.hcs.vc;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.hedera.hashgraph.identity.hcs.did.HcsDid;
import com.hedera.hashgraph.identity.utils.JsonUtils;
import com.hedera.hashgraph.identity.utils.SingleToArrayTypeAdapterFactory;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.bitcoinj.core.Base58;

/**
 * The base for a VC document generation in JSON-LD format.
 * VC documents according to W3C draft specification must be compatible with JSON-LD version 1.1 Up until now there is
 * no Java implementation library of JSON-LD version 1.1. For that reason this object represents only the most basic and
 * mandatory attributes from the VC specification and Hedera HCS DID method specification point of view. Applications
 * shall extend it with any VC document properties or custom properties they require.
 */
public class HcsVcDocumentBase<T extends CredentialSubject> extends HcsVcDocumentHashBase {

  @Expose(serialize = true, deserialize = false)
  @SerializedName(HcsVcDocumentJsonProperties.CONTEXT)
  protected List<String> context;

  @Expose(serialize = true, deserialize = true)
  @SerializedName(HcsVcDocumentJsonProperties.CREDENTIAL_SUBJECT)
  @JsonAdapter(SingleToArrayTypeAdapterFactory.class)
  protected List<T> credentialSubject;

  /**
   * Converts a VC document in JSON format into a {@link HcsVcDocumentBase} object.
   * Please note this conversion respects only the fields of the base VC document. All other fields are ignored.
   *
   * @param  <U>                    The type of the credential subject.
   * @param  json                   The VC document as JSON string.
   * @param  credentialSubjectClass The type of the credential subject inside.
   * @return                        The {@link HcsVcDocumentBase} object.
   */
  public static <U extends CredentialSubject> HcsVcDocumentBase<U> fromJson(final String json,
      final Class<U> credentialSubjectClass) {
    Type envelopeType = TypeToken.getParameterized(HcsVcDocumentBase.class, credentialSubjectClass).getType();
    return JsonUtils.getGson().fromJson(json, envelopeType);
  }

  /**
   * Creates a new VC Document instance.
   */
  public HcsVcDocumentBase() {
    super();
    this.context = Lists.newArrayList(HcsVcDocumentJsonProperties.FIRST_CONTEXT_ENTRY);
  }

  /**
   * Converts this document into a JSON string.
   *
   * @return The JSON representation of this document.
   */
  public String toJson() {
    return JsonUtils.getGson().toJson(this);
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

    String json = JsonUtils.getGson().toJson(map);
    byte[] hash = Hashing.sha256().hashBytes(json.getBytes(StandardCharsets.UTF_8)).asBytes();

    return Base58.encode(hash);
  }

  /**
   * Adds an additional context to @context field of the VC document.
   *
   * @param context The context to add.
   */
  public void addContext(final String context) {
    this.context.add(context);
  }

  /**
   * Adds an additional type to `type` field of the VC document.
   *
   * @param type The type to add.
   */
  public void addType(final String type) {
    this.type.add(type);
  }

  /**
   * Adds a credential subject.
   *
   * @param credentialSubject The credential subject to add.
   */
  public void addCredentialSubject(final T credentialSubject) {
    if (this.credentialSubject == null) {
      this.credentialSubject = new ArrayList<>();
    }

    this.credentialSubject.add(credentialSubject);
  }

  /**
   * Checks if all mandatory fields of a VC document are filled in.
   *
   * @return True if the document is complete and false otherwise.
   */
  public boolean isComplete() {
    return context != null && !context.isEmpty()
        && HcsVcDocumentJsonProperties.FIRST_CONTEXT_ENTRY.equals(context.get(0))
        && type != null && !type.isEmpty() && type.contains(HcsVcDocumentJsonProperties.VERIFIABLE_CREDENTIAL_TYPE)
        && issuanceDate != null
        && issuer != null && !Strings.isNullOrEmpty(issuer.getId())
        && credentialSubject != null && !credentialSubject.isEmpty();
  }

  public List<String> getContext() {
    return context;
  }

  public String getId() {
    return id;
  }

  public List<String> getType() {
    return type;
  }

  public Issuer getIssuer() {
    return issuer;
  }

  public Instant getIssuanceDate() {
    return issuanceDate;
  }

  public List<T> getCredentialSubject() {
    return credentialSubject;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public void setIssuer(final Issuer issuer) {
    this.issuer = issuer;
  }

  public void setIssuer(final String issuerDid) {
    this.issuer = new Issuer(issuerDid);
  }

  public void setIssuer(final HcsDid issuerDid) {
    setIssuer(issuerDid.toDid());
  }

  public void setIssuanceDate(final Instant issuanceDate) {
    this.issuanceDate = issuanceDate;
  }

}
