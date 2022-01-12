package com.hedera.hashgraph.identity.hcs.vc;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.hedera.hashgraph.identity.hcs.did.HcsDid;
import com.hedera.hashgraph.identity.utils.JsonUtils;
import com.hedera.hashgraph.identity.utils.SingleToArrayTypeAdapterFactory;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.threeten.bp.Instant;

/**
 * The base for a VC document generation in JSON-LD format.
 * VC documents according to W3C draft specification must be compatible with JSON-LD version 1.1 Up until now there is
 * no Java implementation library of JSON-LD version 1.1. For that reason this object represents only the most basic and
 * mandatory attributes from the VC specification and Hedera HCS DID method specification point of view. Applications
 * shall extend it with any VC document properties or custom properties they require.
 */
public abstract class HcsVcDocumentBase<T extends CredentialSubject> extends HcsDocumentHashBase {

  @Expose(deserialize = false)
  @SerializedName(HcsVcDocumentJsonProperties.CONTEXT)
  protected List<String> context;

  @Expose
  @SerializedName(HcsVcDocumentJsonProperties.CREDENTIAL_SUBJECT)
  @JsonAdapter(SingleToArrayTypeAdapterFactory.class)
  protected List<T> credentialSubject;

  /**
   * Creates a new VC Document instance.
   */
  public HcsVcDocumentBase() {
    super();
    this.context = Lists.newArrayList(HcsVcDocumentJsonProperties.FIRST_CONTEXT_ENTRY);
  }

  /**
   * Converts a VC document in JSON format into a {@link HcsVcDocumentBase} object.
   * Please note this conversion respects only the fields of the base VC document. All other fields are ignored.
   *
   * @param <U>                    The type of the credential subject.
   * @param <E>                    The type of the vc document.
   * @param json                   The VC document as JSON string.
   * @param credentialSubjectClass The type of the credential subject inside.
   * @return The {@link HcsVcDocumentBase} object.
   */
  public static <U extends CredentialSubject, E extends HcsVcDocumentBase<U>> HcsVcDocumentBase<U> fromJson(final String json,
                                                                            final Class<E> vcDocumentClass,
                                                                            final Class<U> credentialSubjectClass) {
    Type envelopeType = TypeToken.getParameterized(vcDocumentClass, credentialSubjectClass).getType();
    return JsonUtils.getGson().fromJson(json, envelopeType);
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

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public List<String> getType() {
    return type;
  }

  public Issuer getIssuer() {
    return issuer;
  }

  @Override
  public void setIssuer(final Issuer issuer) {
    this.issuer = issuer;
  }

  public void setIssuer(final String issuerDid) {
    this.issuer = new Issuer(issuerDid);
  }

  public void setIssuer(final HcsDid issuerDid) {
    setIssuer(issuerDid.toDid());
  }

  public Instant getIssuanceDate() {
    return issuanceDate;
  }

  @Override
  public void setIssuanceDate(final Instant issuanceDate) {
    this.issuanceDate = issuanceDate;
  }

  public List<T> getCredentialSubject() {
    return credentialSubject;
  }

  public void setCredentialSubject(final List<T> credentialSubject) {
    this.credentialSubject = credentialSubject;
  }

  public void setContext(final List<String> context) {
    this.context = context;
  }
}
