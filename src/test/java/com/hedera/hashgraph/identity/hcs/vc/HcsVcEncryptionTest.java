package com.hedera.hashgraph.identity.hcs.vc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import com.hedera.hashgraph.identity.hcs.AesEncryptionUtil;
import com.hedera.hashgraph.identity.hcs.MessageEnvelope;
import com.hedera.hashgraph.identity.hcs.NetworkReadyTestBase;
import com.hedera.hashgraph.identity.hcs.did.HcsDid;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Tests operations on verifiable credentials and their status resolution.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class HcsVcEncryptionTest extends NetworkReadyTestBase {
  private static final String SECRET = "Secret message used for encryption";
  private static final String INVALID_SECRET = "Invalid secret message used for decryption";

  private HcsDid issuer;
  private HcsDid owner;
  private HcsVcDocumentBase<DemoAccessCredential> vc;
  private String credentialHash;
  private Ed25519PrivateKey issuersPrivateKey;

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
    vc = new HcsVcDocumentBase<DemoAccessCredential>();
    vc.setIssuer(issuer);
    vc.setIssuanceDate(Instant.now());
    vc.addCredentialSubject(new DemoAccessCredential(owner.toDid(), true, false, false));

    credentialHash = vc.toCredentialHash();
  }

  @Test
  @Order(1)
  void testIssueValidEncryptedMessage() {
    AtomicReference<MessageEnvelope<HcsVcMessage>> messageRef = new AtomicReference<>(null);

    // Build and execute transaction with encrypted message
    didNetwork.createVcTransaction(HcsVcOperation.ISSUE, credentialHash, issuersPrivateKey.publicKey)
        .signMessage(doc -> issuersPrivateKey.sign(doc))
        .buildAndSignTransaction(tx -> tx.setMaxTransactionFee(FEE).build(client))
        .onMessageConfirmed(msg -> messageRef.set(msg))
        .onError(EXPECT_NO_ERROR)
        .onEncrypt(m -> AesEncryptionUtil.encrypt(m, SECRET))
        .onDecrypt((m, i) -> AesEncryptionUtil.decrypt(m, SECRET))
        .execute(client, mirrorClient);

    // Wait until consensus is reached and mirror node received the DID document, but with max. time limit.
    Awaitility.await().atMost(MIRROR_NODE_TIMEOUT).until(() -> messageRef.get() != null);

    MessageEnvelope<HcsVcMessage> envelope = messageRef.get();

    assertNotNull(envelope);

    HcsVcMessage msg = envelope.open();

    // Check results
    assertNotNull(msg);
    assertTrue(msg.isValid());
    assertEquals(credentialHash, msg.getCredentialHash());
  }

  @Test
  @Order(2)
  void testResolveWithValidDecrypter() {
    AtomicReference<Map<String, MessageEnvelope<HcsVcMessage>>> mapRef = new AtomicReference<>(null);

    // Resolve encrypted message
    didNetwork.getVcStatusResolver(m -> Lists.newArrayList(issuersPrivateKey.publicKey))
        .addCredentialHash(credentialHash)
        .setTimeout(NO_MORE_MESSAGES_TIMEOUT)
        .onError(EXPECT_NO_ERROR)
        .onDecrypt((m, i) -> AesEncryptionUtil.decrypt(m, SECRET))
        .whenFinished(m -> mapRef.set(m))
        .execute(mirrorClient);

    // Wait until mirror node resolves the DID.
    Awaitility.await().atMost(MIRROR_NODE_TIMEOUT).until(() -> mapRef.get() != null);

    MessageEnvelope<HcsVcMessage> envelope = mapRef.get().get(credentialHash);

    assertNotNull(envelope);

    HcsVcMessage msg = envelope.open();

    // Check results
    assertNotNull(msg);
    assertTrue(msg.isValid());
    assertEquals(credentialHash, msg.getCredentialHash());
  }

  @Test
  @Order(3)
  void testResolveWithInvalidDecrypter() {
    AtomicReference<Map<String, MessageEnvelope<HcsVcMessage>>> mapRef = new AtomicReference<>(null);

    // Try to resolve encrypted message with a wrong secret
    didNetwork.getVcStatusResolver(m -> Lists.newArrayList(issuersPrivateKey.publicKey))
        .addCredentialHash(credentialHash)
        .setTimeout(NO_MORE_MESSAGES_TIMEOUT)
        .onError(e -> assertNotNull(e))
        .onDecrypt((m, i) -> AesEncryptionUtil.decrypt(m, INVALID_SECRET))
        .whenFinished(m -> mapRef.set(m))
        .execute(mirrorClient);

    // Wait until mirror node resolves the DID.
    Awaitility.await().atMost(MIRROR_NODE_TIMEOUT).until(() -> mapRef.get() != null);

    MessageEnvelope<HcsVcMessage> envelope = mapRef.get().get(credentialHash);

    assertNull(envelope);
  }

  @Test
  void testMessageEncryptionDecryption() {
    MessageEnvelope<HcsVcMessage> msg = HcsVcMessage.fromCredentialHash(credentialHash, HcsVcOperation.ISSUE);

    MessageEnvelope<HcsVcMessage> encryptedMsg = msg
        .encrypt(HcsVcMessage.getEncrypter(m -> AesEncryptionUtil.encrypt(m, SECRET)));

    assertNotNull(encryptedMsg);

    MessageEnvelope<HcsVcMessage> encryptedSignedMsg = MessageEnvelope
        .fromJson(new String(encryptedMsg.sign(m -> issuersPrivateKey.sign(m)), StandardCharsets.UTF_8),
            HcsVcMessage.class);

    assertNotNull(encryptedSignedMsg);
    // Throw error if decrypter is not provided
    assertThrows(IllegalArgumentException.class, () -> encryptedSignedMsg.open());

    HcsVcMessage decryptedMsg = encryptedSignedMsg
        .open(HcsVcMessage.getDecrypter((m, i) -> AesEncryptionUtil.decrypt(m, SECRET)));

    assertNotNull(decryptedMsg);
    assertEquals(credentialHash, decryptedMsg.getCredentialHash());
    assertEquals(encryptedSignedMsg.open().getTimestamp(), decryptedMsg.getTimestamp());
  }

}
