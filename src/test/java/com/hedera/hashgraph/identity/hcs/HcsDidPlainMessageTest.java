package com.hedera.hashgraph.identity.hcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hedera.hashgraph.identity.DidDocumentBase;
import com.hedera.hashgraph.identity.DidDocumentOperation;
import com.hedera.hashgraph.identity.HederaNetwork;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.file.FileId;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Test of plain message validation.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HcsDidPlainMessageTest {
  private static final FileId ADDRESS_BOOK_FID = FileId.fromString("0.0.1");
  private static final ConsensusTopicId DID_TOPIC_ID1 = ConsensusTopicId.fromString("0.0.2");
  private static final ConsensusTopicId DID_TOPIC_ID2 = ConsensusTopicId.fromString("0.0.3");

  @Test
  public void testValidMessage() {
    Ed25519PrivateKey privateKey = HcsDid.generateDidRootKey();
    HcsDid did = new HcsDid(HederaNetwork.TESTNET, privateKey.publicKey,ADDRESS_BOOK_FID);
    DidDocumentBase doc = did.generateDidDocument();

    String didJson = doc.toJson();
    byte[] message = HcsDidMessageBuilder.fromDidDocument(didJson)
        .setOperation(DidDocumentOperation.CREATE)
        .buildAndSign(msg -> privateKey.sign(msg));

    HcsDidPlainMessage msg = HcsDidMessage
        .fromJson(new String(message, StandardCharsets.UTF_8))
        .toPlainDidMessage(null);

    assertTrue(msg.isValid());
    // Test below should be true, as the did does not contain tid parameter
    assertTrue(msg.isValid(DID_TOPIC_ID1));
  }

  @Test
  public void testInvalidDid() {
    Ed25519PrivateKey privateKey = HcsDid.generateDidRootKey();
    HcsDid did = new HcsDid(HederaNetwork.TESTNET, privateKey.publicKey,ADDRESS_BOOK_FID);
    DidDocumentBase doc = did.generateDidDocument();

    String didJson = doc.toJson();
    byte[] message = HcsDidMessageBuilder.fromDidDocument(didJson)
        .setOperation(DidDocumentOperation.CREATE)
        .buildAndSign(msg -> privateKey.sign(msg));

    HcsDidPlainMessage msg = HcsDidMessage
        .fromJson(new String(message, StandardCharsets.UTF_8))
        .toPlainDidMessage(null);

    HcsDid differentDid = new HcsDid(HederaNetwork.TESTNET, HcsDid.generateDidRootKey().publicKey, ADDRESS_BOOK_FID);
    msg.did = differentDid.toDid();

    assertFalse(msg.isValid());
  }

  @Test
  void testInvalidTopic() {
    Ed25519PrivateKey privateKey = HcsDid.generateDidRootKey();
    // Include topic ID in the DID.
    HcsDid did = new HcsDid(HederaNetwork.TESTNET, privateKey.publicKey,ADDRESS_BOOK_FID, DID_TOPIC_ID1);
    DidDocumentBase doc = did.generateDidDocument();

    String didJson = doc.toJson();
    byte[] message = HcsDidMessageBuilder.fromDidDocument(didJson)
        .setOperation(DidDocumentOperation.CREATE)
        .buildAndSign(msg -> privateKey.sign(msg));

    HcsDidPlainMessage msg = HcsDidMessage
        .fromJson(new String(message, StandardCharsets.UTF_8))
        .toPlainDidMessage(null);

    assertTrue(msg.isValid(DID_TOPIC_ID1));
    assertFalse(msg.isValid(DID_TOPIC_ID2));
  }

  @Test
  void testMissingData() {
    Ed25519PrivateKey privateKey = HcsDid.generateDidRootKey();
    HcsDid did = new HcsDid(HederaNetwork.TESTNET, privateKey.publicKey,ADDRESS_BOOK_FID);
    DidDocumentBase doc = did.generateDidDocument();
    final DidDocumentOperation operation = DidDocumentOperation.CREATE;
    final Instant timestamp = Instant.now();

    String didJson = doc.toJson();
    byte[] message = HcsDidMessageBuilder.fromDidDocument(didJson)
        .setOperation(DidDocumentOperation.CREATE)
        .buildAndSign(msg -> privateKey.sign(msg));

    HcsDidPlainMessage validMsg = HcsDidMessage
        .fromJson(new String(message, StandardCharsets.UTF_8))
        .toPlainDidMessage(null);

    HcsDidPlainMessage msg = new HcsDidPlainMessage(operation, null, validMsg.getDidDocumentBase64(),
        validMsg.getSignature(), timestamp);
    assertFalse(msg.isValid());

    msg = new HcsDidPlainMessage(operation, validMsg.getDid(), validMsg.getDidDocumentBase64(), "",timestamp);
    assertFalse(msg.isValid());
    assertNotNull(msg.getDidDocument());
    assertNotNull(msg.getDidDocumentBase64());

    msg = new HcsDidPlainMessage(operation, validMsg.getDid(), null, validMsg.getSignature(), timestamp);
    assertFalse(msg.isValid());
    assertNull(msg.getDidDocument());
    assertNotNull(msg.getDid());
    assertNotNull(msg.getSignature());
    assertEquals(timestamp, msg.getConsensusTimestamp());
    assertEquals(operation, msg.getDidOperation());
  }

  @Test
  public void testInvalidSignature() {
    Ed25519PrivateKey privateKey = HcsDid.generateDidRootKey();
    HcsDid did = new HcsDid(HederaNetwork.TESTNET, privateKey.publicKey,ADDRESS_BOOK_FID);
    DidDocumentBase doc = did.generateDidDocument();

    String didJson = doc.toJson();
    // Sign message with different key.
    byte[] message = HcsDidMessageBuilder.fromDidDocument(didJson)
        .setOperation(DidDocumentOperation.CREATE)
        .buildAndSign(msg -> HcsDid.generateDidRootKey().sign(msg));

    HcsDidPlainMessage msg = HcsDidMessage
        .fromJson(new String(message, StandardCharsets.UTF_8))
        .toPlainDidMessage(null);

    assertFalse(msg.isValid());
  }
}
