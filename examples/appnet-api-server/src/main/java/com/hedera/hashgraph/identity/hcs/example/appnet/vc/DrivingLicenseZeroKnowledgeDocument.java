package com.hedera.hashgraph.identity.hcs.example.appnet.vc;

import com.google.gson.annotations.Expose;
import com.hedera.hashgraph.zeroknowledge.vc.HcsVcDocumentZeroKnowledge;

import java.util.UUID;

/**
 * A simple, manually constructed example of a driving license verifiable credential document.
 */
public class DrivingLicenseZeroKnowledgeDocument extends HcsVcDocumentZeroKnowledge<DrivingLicense> {
  private static final String DOCUMENT_TYPE = "DrivingLicense";
  private static final String EXAMPLE_ID_PREFIX = "https://example.appnet.com/driving-license/";
  @Expose
  private CredentialSchema credentialSchema;

  @Expose
  private Ed25519CredentialProof proof;

  /**
   * Creates a new verifiable credential document instance with predefined types and auto-generated ID.
   */
  public DrivingLicenseZeroKnowledgeDocument() {
    super();
    addType(DOCUMENT_TYPE);

    // Generate a unique identifier for this credential
    this.id = EXAMPLE_ID_PREFIX + UUID.randomUUID();
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
