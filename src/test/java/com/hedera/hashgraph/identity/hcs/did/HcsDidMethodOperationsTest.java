package com.hedera.hashgraph.identity.hcs.did;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hedera.hashgraph.identity.DidDocumentJsonProperties;
import com.hedera.hashgraph.identity.DidMethodOperation;
import com.hedera.hashgraph.identity.hcs.MessageEnvelope;
import com.hedera.hashgraph.identity.hcs.NetworkReadyTestBase;
import com.hedera.hashgraph.identity.utils.JsonUtils;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Tests DID method operations on a DID document.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class HcsDidMethodOperationsTest extends NetworkReadyTestBase {
  private HcsDid hcsDid;
  private String didDocument;

  @Override
  protected void beforeAll() {
    hcsDid = didNetwork.generateDid(false);
    didDocument = hcsDid.generateDidDocument().toJson();
  }

  @Test
  @Order(1)
  void testCreate() {
    final DidMethodOperation op = DidMethodOperation.CREATE;

    MessageEnvelope<HcsDidMessage> envelope = sendDidTransaction(hcsDid, didDocument, op, EXPECT_NO_ERROR);
    assertNotNull(envelope);

    HcsDidMessage msg = envelope.open();

    // Check results
    assertNotNull(msg);
    assertNotNull(msg.getDidDocument());

    assertEquals(hcsDid.toDid(), msg.getDid());
    assertTrue(msg.isValid());
    assertEquals(op, msg.getOperation());
  }

  @Test
  @Order(2)
  void testResolveAfterCreate() {
    String didString = hcsDid.toDid();

    MessageEnvelope<HcsDidMessage> envelope = resolveDid(didString, EXPECT_NO_ERROR);
    assertNotNull(envelope);

    HcsDidMessage msg = envelope.open();

    assertNotNull(msg);
    assertEquals(didString, msg.getDid());
    assertTrue(msg.isValid());
    assertEquals(DidMethodOperation.CREATE, msg.getOperation());
  }

  @Test
  @Order(3)
  void testUpdate() {
    JsonElement jsonElement = JsonParser.parseString(didDocument);
    JsonObject rootObject = jsonElement.getAsJsonObject();

    // Add an additional public key.
    JsonArray publicKeys = null;
    publicKeys = rootObject.getAsJsonArray(DidDocumentJsonProperties.PUBLIC_KEY);
    publicKeys.add(JsonParser.parseString("{"
        + "\"id\": \"did:example:123456789abcdefghi#keys-2\","
        + "\"type\": \"Ed25519VerificationKey2018\","
        + "\"controller\": \"did:example:pqrstuvwxyz0987654321\","
        + "\"publicKeyBase58\": \"H3C2AVvLMv6gmMNam3uVAjZpfkcJCwDwnZn6z3wXmqPV\""
        + "}"));

    String newDoc = JsonUtils.getGson().toJson(jsonElement);

    final DidMethodOperation operation = DidMethodOperation.UPDATE;
    MessageEnvelope<HcsDidMessage> envelope = sendDidTransaction(hcsDid, newDoc, operation, EXPECT_NO_ERROR);
    assertNotNull(envelope);

    HcsDidMessage msg = envelope.open();

    // Check results
    assertNotNull(msg);
    assertEquals(newDoc, msg.getDidDocument());
    assertEquals(operation, msg.getOperation());
  }

  @Test
  @Order(4)
  void testResolveAfterUpdate() {
    String didString = hcsDid.toDid();

    MessageEnvelope<HcsDidMessage> envelope = resolveDid(didString, EXPECT_NO_ERROR);
    assertNotNull(envelope);

    HcsDidMessage msg = envelope.open();

    assertNotNull(msg);
    assertEquals(didString, msg.getDid());
    assertTrue(msg.isValid());
    assertEquals(DidMethodOperation.UPDATE, msg.getOperation());
    assertNotEquals(didDocument, msg.getDidDocument());
  }

  @Test
  @Order(5)
  void testDelete() {
    JsonElement jsonElement = JsonParser.parseString(didDocument);
    JsonObject rootObject = jsonElement.getAsJsonObject();

    // Remove authentication keys
    // If authentication is defined remove it and replace with empty array
    if (rootObject.has(DidDocumentJsonProperties.AUTHENTICATION)) {
      rootObject.remove(DidDocumentJsonProperties.AUTHENTICATION);
      rootObject.add(DidDocumentJsonProperties.AUTHENTICATION, new JsonArray());
    }

    String deletedDoc = JsonUtils.getGson().toJson(jsonElement);

    final DidMethodOperation operation = DidMethodOperation.DELETE;
    MessageEnvelope<HcsDidMessage> envelope = sendDidTransaction(hcsDid, deletedDoc, operation, EXPECT_NO_ERROR);
    assertNotNull(envelope);

    HcsDidMessage msg = envelope.open();

    // Check results
    assertNotNull(msg);
    assertEquals(deletedDoc, msg.getDidDocument());
    assertEquals(operation, msg.getOperation());
  }

  @Test
  @Order(6)
  void testResolveAfterDelete() {
    String didString = hcsDid.toDid();

    MessageEnvelope<HcsDidMessage> envelope = resolveDid(didString, EXPECT_NO_ERROR);
    assertNotNull(envelope);

    HcsDidMessage msg = envelope.open();

    assertNotNull(msg);
    assertEquals(didString, msg.getDid());
    assertTrue(msg.isValid());
    assertEquals(DidMethodOperation.DELETE, msg.getOperation());
    assertNotEquals(didDocument, msg.getDidDocument());
  }

  @Test
  @Order(7)
  void testResolveAfterDeleteAndAnotherInvalidSubmit() {
    sendDidTransaction(hcsDid, didDocument, DidMethodOperation.UPDATE, EXPECT_NO_ERROR);

    String didString = hcsDid.toDid();
    MessageEnvelope<HcsDidMessage> envelope = resolveDid(didString, EXPECT_NO_ERROR);
    assertNotNull(envelope);

    HcsDidMessage msg = envelope.open();

    assertNotNull(msg);
    assertEquals(didString, msg.getDid());
    assertTrue(msg.isValid());
    assertEquals(DidMethodOperation.DELETE, msg.getOperation());
    assertNotEquals(didDocument, msg.getDidDocument());
  }

  // TODO implement encryption decryption tests.
}
