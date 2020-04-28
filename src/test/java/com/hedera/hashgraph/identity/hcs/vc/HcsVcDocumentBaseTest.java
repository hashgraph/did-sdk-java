package com.hedera.hashgraph.identity.hcs.vc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.Strings;
import com.hedera.hashgraph.identity.hcs.NetworkReadyTestBase;
import com.hedera.hashgraph.identity.hcs.did.HcsDid;
import java.time.Instant;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Tests creation, serialization and deserialization of a VC document base.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class HcsVcDocumentBaseTest extends NetworkReadyTestBase {
  private HcsDid issuer;
  private HcsDid owner;

  @Override
  protected void beforeAll() {
    issuer = didNetwork.generateDid(false);
    owner = didNetwork.generateDid(false);
  }

  @Test
  void testVcDocumentConstruction() {
    HcsVcDocumentBase<DemoAccessCredential> vc = new HcsVcDocumentBase<DemoAccessCredential>();

    // Should fail as no issuer is set.
    assertFalse(vc.isComplete());

    vc.setIssuer(issuer);

    // Should fail as no issuance date is set.
    assertFalse(vc.isComplete());

    vc.setIssuanceDate(Instant.now());

    // Should fail as no credential subject is set.
    assertFalse(vc.isComplete());

    // Default VC type should be set.
    assertNotNull(vc.getType());
    assertEquals(1, vc.getType().size());

    // Add a custom type
    vc.addType("TestVC");
    assertEquals(2, vc.getType().size());

    // Default context should be set
    assertNotNull(vc.getContext());
    assertEquals(1, vc.getContext().size());

    // Add a custom context
    vc.addContext("https://www.example.com/testContext");
    assertEquals(2, vc.getContext().size());

    // Add a credential subject.
    assertNull(vc.getCredentialSubject());
    DemoAccessCredential credential = new DemoAccessCredential(owner.toDid(), true, false, false);
    vc.addCredentialSubject(credential);

    // Make sure it's there
    assertNotNull(vc.getCredentialSubject());
    assertEquals(1, vc.getCredentialSubject().size());

    // Now all mandatory fields should be set
    assertTrue(vc.isComplete());
  }

  @Test
  void testVcJsonConversion() {
    HcsVcDocumentBase<DemoAccessCredential> vc = new HcsVcDocumentBase<DemoAccessCredential>();
    vc.setId("example:test:vc:id");
    vc.setIssuer(new Issuer(issuer.toDid(), "My Company Ltd."));
    vc.setIssuanceDate(Instant.now());

    DemoAccessCredential subject = new DemoAccessCredential(owner.toDid(), true, false, false);
    vc.addCredentialSubject(subject);

    // Convert to JSON
    String json = vc.toJson();
    assertFalse(Strings.isNullOrEmpty(json));

    // Convert back to VC document and compare
    HcsVcDocumentBase<DemoAccessCredential> vcFromJson = HcsVcDocumentBase.fromJson(json, DemoAccessCredential.class);
    // Test simple properties
    assertNotNull(vcFromJson);
    assertEquals(vc.getType(), vcFromJson.getType());
    assertEquals(vc.getContext(), vcFromJson.getContext());
    assertEquals(vc.getIssuanceDate(), vcFromJson.getIssuanceDate());
    assertEquals(vc.getId(), vcFromJson.getId());

    // Test issuer object
    assertNotNull(vcFromJson.getIssuer());
    assertEquals(vc.getIssuer().getId(), vcFromJson.getIssuer().getId());
    assertEquals(vc.getIssuer().getName(), vcFromJson.getIssuer().getName());

    // Test credential subject
    assertNotNull(vcFromJson.getCredentialSubject());

    DemoAccessCredential subjectFromJson = vcFromJson.getCredentialSubject().get(0);
    assertEquals(subject.getId(), subjectFromJson.getId());
    assertEquals(subject.getBlueLevel(), subjectFromJson.getBlueLevel());
    assertEquals(subject.getGreenLevel(), subjectFromJson.getGreenLevel());
    assertEquals(subject.getRedLevel(), subjectFromJson.getRedLevel());
  }

  @Test
  void testCredentialHash() {
    DemoVerifiableCredentialDocument vc = new DemoVerifiableCredentialDocument();
    vc.setId("example:test:vc:id");
    vc.setIssuer(issuer);
    vc.setIssuanceDate(Instant.now());
    vc.addCredentialSubject(new DemoAccessCredential(owner.toDid(), true, false, false));
    vc.setCustomProperty("Custom property value 1");

    String credentialHash = vc.toCredentialHash();
    assertFalse(Strings.isNullOrEmpty(credentialHash));

    // Recalculation should give the same value
    assertEquals(credentialHash, vc.toCredentialHash());

    // Hash shall not change if we don't change anything in the document
    vc.setCustomProperty("Another value for custom property");
    vc.addCredentialSubject(new DemoAccessCredential(owner.toDid(), false, false, true));

    assertEquals(credentialHash, vc.toCredentialHash());
  }
}
