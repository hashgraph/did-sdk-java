package com.hedera.hashgraph.identity.hcs.vc;

import com.google.gson.annotations.Expose;

/**
 * Example Credential.
 */
class DemoAccessCredential extends CredentialSubject {
  public static final String ACCESS_GRANTED = "granted";
  public static final String ACCESS_DENIED = "denied";

  @Expose
  private String blueLevel;
  @Expose
  private String greenLevel;
  @Expose
  private String redLevel;

  /**
   * Creates a new credential instance.
   *
   * @param did   Credential Subject DID.
   * @param blue  Access to blue level granted or denied.
   * @param green Access to green level granted or denied.
   * @param red   Access to red level granted or denied.
   */
  public DemoAccessCredential(String did, boolean blue, boolean green, boolean red) {
    this.id = did;
    this.blueLevel = blue ? ACCESS_GRANTED : ACCESS_DENIED;
    this.greenLevel = green ? ACCESS_GRANTED : ACCESS_DENIED;
    this.redLevel = red ? ACCESS_GRANTED : ACCESS_DENIED;
  }

  public String getBlueLevel() {
    return blueLevel;
  }

  public String getGreenLevel() {
    return greenLevel;
  }

  public String getRedLevel() {
    return redLevel;
  }
}
