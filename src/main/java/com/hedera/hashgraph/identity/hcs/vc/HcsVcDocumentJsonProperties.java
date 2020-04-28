package com.hedera.hashgraph.identity.hcs.vc;

/**
 * Key property names in VC document standard.
 */
public final class HcsVcDocumentJsonProperties {
  public static final String CONTEXT = "@context";
  public static final String FIRST_CONTEXT_ENTRY = "https://www.w3.org/2018/credentials/v1";

  public static final String ID = "id";
  public static final String CREDENTIAL_SUBJECT = "credentialSubject";

  public static final String TYPE = "type";
  public static final String VERIFIABLE_CREDENTIAL_TYPE = "VerifiableCredential";

  public static final String ISSUER = "issuer";
  public static final String ISSUANCE_DATE = "issuanceDate";
  public static final String CREDENTIAL_STATUS = "credentialStatus";
  public static final String PROOF = "proof";

  /**
   * This class is not to be instantiated.
   */
  private HcsVcDocumentJsonProperties() {
    // Empty on purpose.
  }
}