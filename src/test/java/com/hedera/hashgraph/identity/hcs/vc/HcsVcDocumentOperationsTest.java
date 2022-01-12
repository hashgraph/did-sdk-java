package com.hedera.hashgraph.identity.hcs.vc;

import com.google.common.collect.Lists;
import com.hedera.hashgraph.identity.hcs.MessageEnvelope;
import com.hedera.hashgraph.identity.hcs.NetworkReadyTestBase;
import com.hedera.hashgraph.identity.hcs.did.HcsDid;
import com.hedera.hashgraph.sdk.PrivateKey;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.threeten.bp.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests operations on verifiable credentials and their status resolution.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class HcsVcDocumentOperationsTest extends NetworkReadyTestBase {
  private HcsDid issuer;
  private HcsDid owner;
  private HcsVcDocumentBase<DemoAccessCredential> vc;
  private String credentialHash;
  private PrivateKey issuersPrivateKey;

  @Override
  protected void beforeAll() {
    issuer = didNetwork.generateDid(false);
    issuersPrivateKey = issuer.getPrivateDidRootKey().get();

    owner = didNetwork.generateDid(false);

    // For tests only we do not need to submit DID documents, as we will not validate them.
    // final DidMethodOperation op = DidMethodOperation.CREATE;
    // sendDidTransaction(issuer, issuer.generateDidDocument().toJson(), op, EXPECT_NO_ERROR);
    // sendDidTransaction(owner, owner.generateDidDocument().toJson(), op, EXPECT_NO_ERROR);

    // Create an example Verifiable Credential.
    vc = new HcsVcDocument<>();
    vc.setIssuer(issuer);
    vc.setIssuanceDate(Instant.now());
    vc.addCredentialSubject(new DemoAccessCredential(owner.toDid(), true, false, false));

    credentialHash = vc.toCredentialHash();
  }

  private void testVcOperation(HcsVcOperation op) {
    MessageEnvelope<HcsVcMessage> envelope = sendVcTransaction(op, credentialHash, issuersPrivateKey, EXPECT_NO_ERROR);
    assertNotNull(envelope);

    HcsVcMessage msg = envelope.open();

    // Check results
    assertNotNull(msg);
    assertTrue(msg.isValid());
    assertEquals(op, msg.getOperation());
    assertEquals(credentialHash, msg.getCredentialHash());
  }

  private void testVcStatusResolution(HcsVcOperation expectedOperation) {
    MessageEnvelope<HcsVcMessage> envelope = resolveVcStatus(
            credentialHash,
            m -> Lists.newArrayList(issuersPrivateKey.getPublicKey()),
            EXPECT_NO_ERROR);

    assertNotNull(envelope);

    HcsVcMessage msg = envelope.open();

    assertNotNull(msg);
    assertTrue(msg.isValid());
    assertEquals(credentialHash, msg.getCredentialHash());
    assertEquals(expectedOperation, msg.getOperation());
  }

  @Test
  @Order(1)
  void testIssue() {
    testVcOperation(HcsVcOperation.ISSUE);
    testVcStatusResolution(HcsVcOperation.ISSUE);
  }

  @Test
  @Order(2)
  void testSuspend() {
    testVcOperation(HcsVcOperation.SUSPEND);
    testVcStatusResolution(HcsVcOperation.SUSPEND);
  }

  @Test
  @Order(3)
  void testResume() {
    testVcOperation(HcsVcOperation.RESUME);
    testVcStatusResolution(HcsVcOperation.RESUME);
  }

  @Test
  @Order(4)
  void testRevoke() {
    testVcOperation(HcsVcOperation.REVOKE);
    testVcStatusResolution(HcsVcOperation.REVOKE);
  }

  @Test
  @Order(5)
  void testInvalidResumeAfterRevoke() {
    testVcOperation(HcsVcOperation.RESUME);
    // Status should still be revoked
    testVcStatusResolution(HcsVcOperation.REVOKE);

    testVcOperation(HcsVcOperation.SUSPEND);
    // Status should still be revoked
    testVcStatusResolution(HcsVcOperation.REVOKE);
  }
}
