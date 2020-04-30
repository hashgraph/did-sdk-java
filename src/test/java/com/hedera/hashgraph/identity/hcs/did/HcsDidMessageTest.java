package com.hedera.hashgraph.identity.hcs.did;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hedera.hashgraph.identity.DidDocumentBase;
import com.hedera.hashgraph.identity.DidMethodOperation;
import com.hedera.hashgraph.identity.HederaNetwork;
import com.hedera.hashgraph.identity.hcs.AesEncryptionUtil;
import com.hedera.hashgraph.identity.hcs.MessageEnvelope;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.file.FileId;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Tests DID message construction and validation.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HcsDidMessageTest {
  private static final FileId ADDRESS_BOOK_FID = FileId.fromString("0.0.1");
  private static final ConsensusTopicId DID_TOPIC_ID1 = ConsensusTopicId.fromString("0.0.2");
  private static final ConsensusTopicId DID_TOPIC_ID2 = ConsensusTopicId.fromString("0.0.3");

  @Test
  void testValidMessage() {
    Ed25519PrivateKey privateKey = HcsDid.generateDidRootKey();
    HcsDid did = new HcsDid(HederaNetwork.TESTNET, privateKey.publicKey, ADDRESS_BOOK_FID);
    DidDocumentBase doc = did.generateDidDocument();
    String didJson = doc.toJson();
    MessageEnvelope<HcsDidMessage> originalEnvelope = HcsDidMessage.fromDidDocumentJson(didJson,
        DidMethodOperation.CREATE);
    byte[] message = originalEnvelope.sign(msg -> privateKey.sign(msg));

    MessageEnvelope<HcsDidMessage> envelope = MessageEnvelope
        .fromJson(new String(message, StandardCharsets.UTF_8), HcsDidMessage.class);

    assertTrue(envelope.isSignatureValid(e -> e.open().extractDidRootKey()));
    // Test below should be true, as the did does not contain tid parameter
    assertTrue(envelope.open().isValid(DID_TOPIC_ID1));
    assertEquals(originalEnvelope.open().getTimestamp(), envelope.open().getTimestamp());
  }

  @Test
  void testEncryptedMessage() {
    final String secret = "Secret encryption password";

    Ed25519PrivateKey privateKey = HcsDid.generateDidRootKey();
    HcsDid did = new HcsDid(HederaNetwork.TESTNET, privateKey.publicKey, ADDRESS_BOOK_FID);
    DidDocumentBase doc = did.generateDidDocument();
    String didJson = doc.toJson();

    MessageEnvelope<HcsDidMessage> originalEnvelope = HcsDidMessage.fromDidDocumentJson(didJson,
        DidMethodOperation.CREATE);

    MessageEnvelope<HcsDidMessage> encryptedMsg = originalEnvelope
        .encrypt(HcsDidMessage.getEncrypter(m -> AesEncryptionUtil.encrypt(m, secret)));

    MessageEnvelope<HcsDidMessage> encryptedSignedMsg = MessageEnvelope
        .fromJson(new String(encryptedMsg.sign(m -> privateKey.sign(m)), StandardCharsets.UTF_8),
            HcsDidMessage.class);

    assertNotNull(encryptedSignedMsg);
    // Throw error if decrypter is not provided
    assertThrows(IllegalArgumentException.class, () -> encryptedSignedMsg.open());

    // Decrypt and open message
    HcsDidMessage decryptedMsg = encryptedSignedMsg
        .open(HcsDidMessage.getDecrypter((m, i) -> AesEncryptionUtil.decrypt(m, secret)));

    // Check if it's properties are correct after decryption
    assertNotNull(decryptedMsg);
    assertEquals(originalEnvelope.open().getDidDocumentBase64(), decryptedMsg.getDidDocumentBase64());
    assertEquals(originalEnvelope.open().getDid(), decryptedMsg.getDid());
  }

  @Test
  void testInvalidDid() {
    Ed25519PrivateKey privateKey = HcsDid.generateDidRootKey();
    HcsDid did = new HcsDid(HederaNetwork.TESTNET, privateKey.publicKey, ADDRESS_BOOK_FID);
    DidDocumentBase doc = did.generateDidDocument();

    String didJson = doc.toJson();
    byte[] message = HcsDidMessage
        .fromDidDocumentJson(didJson, DidMethodOperation.CREATE)
        .sign(msg -> privateKey.sign(msg));

    HcsDidMessage msg = MessageEnvelope
        .fromJson(new String(message, StandardCharsets.UTF_8), HcsDidMessage.class)
        .open();

    HcsDid differentDid = new HcsDid(HederaNetwork.TESTNET, HcsDid.generateDidRootKey().publicKey, ADDRESS_BOOK_FID);
    msg.did = differentDid.toDid();

    assertFalse(msg.isValid());
  }

  @Test
  void testInvalidTopic() {
    Ed25519PrivateKey privateKey = HcsDid.generateDidRootKey();
    // Include topic ID in the DID.
    HcsDid did = new HcsDid(HederaNetwork.TESTNET, privateKey.publicKey, ADDRESS_BOOK_FID, DID_TOPIC_ID1);
    DidDocumentBase doc = did.generateDidDocument();

    String didJson = doc.toJson();
    byte[] message = HcsDidMessage
        .fromDidDocumentJson(didJson, DidMethodOperation.CREATE)
        .sign(msg -> privateKey.sign(msg));

    HcsDidMessage msg = MessageEnvelope
        .fromJson(new String(message, StandardCharsets.UTF_8), HcsDidMessage.class)
        .open();

    assertTrue(msg.isValid(DID_TOPIC_ID1));
    assertFalse(msg.isValid(DID_TOPIC_ID2));
  }

  @Test
  void testMissingData() {
    Ed25519PrivateKey privateKey = HcsDid.generateDidRootKey();
    HcsDid did = new HcsDid(HederaNetwork.TESTNET, privateKey.publicKey, ADDRESS_BOOK_FID);
    DidDocumentBase doc = did.generateDidDocument();
    final DidMethodOperation operation = DidMethodOperation.CREATE;

    String didJson = doc.toJson();
    byte[] message = HcsDidMessage
        .fromDidDocumentJson(didJson, DidMethodOperation.CREATE)
        .sign(msg -> privateKey.sign(msg));

    HcsDidMessage validMsg = MessageEnvelope
        .fromJson(new String(message, StandardCharsets.UTF_8), HcsDidMessage.class)
        .open();

    HcsDidMessage msg = new HcsDidMessage(operation, null, validMsg.getDidDocumentBase64());
    assertFalse(msg.isValid());

    msg = new HcsDidMessage(operation, validMsg.getDid(), null);
    assertFalse(msg.isValid());
    assertNull(msg.getDidDocument());
    assertNotNull(msg.getDid());
    assertEquals(operation, msg.getOperation());
  }

  @Test
  void testInvalidSignature() {
    Ed25519PrivateKey privateKey = HcsDid.generateDidRootKey();
    HcsDid did = new HcsDid(HederaNetwork.TESTNET, privateKey.publicKey, ADDRESS_BOOK_FID);
    DidDocumentBase doc = did.generateDidDocument();

    String didJson = doc.toJson();
    // Sign message with different key.
    byte[] message = HcsDidMessage
        .fromDidDocumentJson(didJson, DidMethodOperation.CREATE)
        .sign(msg -> HcsDid.generateDidRootKey().sign(msg));

    MessageEnvelope<HcsDidMessage> envelope = MessageEnvelope
        .fromJson(new String(message, StandardCharsets.UTF_8), HcsDidMessage.class);

    assertFalse(envelope.isSignatureValid(e -> e.open().extractDidRootKey()));
  }
}
