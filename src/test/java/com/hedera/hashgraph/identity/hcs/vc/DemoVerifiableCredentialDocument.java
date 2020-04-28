package com.hedera.hashgraph.identity.hcs.vc;

import com.google.gson.annotations.Expose;

/**
 * Custom VC document for tests.
 */
class DemoVerifiableCredentialDocument extends HcsVcDocumentBase<DemoAccessCredential> {

  @Expose(serialize = true, deserialize = true)
  private String customProperty;

  public String getCustomProperty() {
    return customProperty;
  }

  public void setCustomProperty(String customProperty) {
    this.customProperty = customProperty;
  }
}
