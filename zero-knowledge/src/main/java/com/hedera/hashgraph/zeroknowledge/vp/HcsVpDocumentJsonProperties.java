package com.hedera.hashgraph.zeroknowledge.vp;

/**
 * Key property names in VC document standard.
 */
public final class HcsVpDocumentJsonProperties {
  public static final String CONTEXT = "@context";
  public static final String FIRST_CONTEXT_ENTRY = "https://www.w3.org/2018/credentials/v1";

  public static final String ID = "id";
  public static final String VERIFIABLE_CREDENTIAL = "verifiableCredential";

  public static final String TYPE = "type";

  public static final String HOLDER = "holder";
  public static final String ISSUANCE_DATE = "issuanceDate";

  public static final String PROOF = "proof";
  public static final String VERIFIABLE_CREDENTIAL_TYPE = "VerifiableCredentialPresentation";

    /**
   * This class is not to be instantiated.
   */
  private HcsVpDocumentJsonProperties() {
    // Empty on purpose.
  }
}
