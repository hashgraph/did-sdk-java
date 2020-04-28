package com.hedera.hashgraph.identity;

/**
 * Key property names in DID document standard.
 */
public final class DidDocumentJsonProperties {
  public static final String CONTEXT = "@context";
  public static final String ID = "id";
  public static final String AUTHENTICATION = "authentication";
  public static final String PUBLIC_KEY = "publicKey";
  public static final String SERVICE = "service";
  public static final String CREATED = "created";
  public static final String UPDATED = "updated";
  public static final String PROOF = "proof";

  /**
   * This class is not to be instantiated.
   */
  private DidDocumentJsonProperties() {
    // Empty on purpose.
  }
}