package com.hedera.hashgraph.identity.hcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hedera.hashgraph.identity.DidDocumentJsonProperties;
import com.hedera.hashgraph.identity.DidDocumentOperation;
import com.hedera.hashgraph.identity.HederaNetwork;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.HederaNetworkException;
import com.hedera.hashgraph.sdk.HederaStatusException;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.file.FileId;
import com.hedera.hashgraph.sdk.mirror.MirrorClient;
import io.github.cdimascio.dotenv.Dotenv;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Tests HCS identity network creation, instantiation and DID operations.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class HcsDidNetworkCrudTest {
  private static final Duration MIRROR_NODE_TIMEOUT = Duration.ofSeconds(10);
  private static final long NO_MORE_MESSAGES_TIMEOUT = Duration.ofSeconds(5).toMillis();
  private static final Hbar FEE = new Hbar(2);

  private Client client;
  private AccountId operatorId;
  private Ed25519PrivateKey operatorKey;
  private MirrorClient mirrorClient;
  private HcsDidNetwork didNetwork;
  private HcsDid hcsDid;
  private String didDocument;

  /**
   * Initialize hedera clients and accounts.
   */
  @BeforeAll
  void setup() throws HederaNetworkException, HederaStatusException {
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().load();

    // Grab the OPERATOR_ID and OPERATOR_KEY from environment variable
    operatorId = AccountId.fromString(Objects.requireNonNull(dotenv.get("OPERATOR_ID")));
    operatorKey = Ed25519PrivateKey.fromString(Objects.requireNonNull(dotenv.get("OPERATOR_KEY")));

    // Build Hedera testnet client
    client = Client.forTestnet();

    // Set the operator account ID and operator private key
    client.setOperator(operatorId, operatorKey);

    // Grab the mirror node address MIRROR_NODE_ADDRESS from environment variable
    final String mirrorNodeAddress = Objects.requireNonNull(Dotenv.load().get("MIRROR_NODE_ADDRESS"));

    // Build the mirror node client
    mirrorClient = new MirrorClient(mirrorNodeAddress);

    // If identity network is provided as environment variable read from there, otherwise setup new one:
    String abJson = Dotenv.load().get("EXISTING_ADDRESS_BOOK_JSON");
    String abFileId = Dotenv.load().get("EXISTING_ADDRESS_BOOK_FILE_ID");
    if (Strings.isNullOrEmpty(abJson) || Strings.isNullOrEmpty(abFileId)) {
      setupIdentityNetwork();
    } else {
      HcsDidNetworkAddressBook addressBook = HcsDidNetworkAddressBook.fromJson(abJson, FileId.fromString(abFileId));
      didNetwork = HcsDidNetwork.fromAddressBook(HederaNetwork.TESTNET, addressBook);
    }

    hcsDid = didNetwork.generateDid(false);
    didDocument = hcsDid.generateDidDocument().toJson();
  }

  void setupIdentityNetwork() throws HederaNetworkException, HederaStatusException {
    final String appnetName = "Test Identity SDK appnet";
    final String didServerUrl = "http://localhost:3000/api/v1";
    final String didTopicMemo = "Test Identity SDK appnet DID topic";
    final String vcTopicMemo = "Test Identity SDK appnet VC topic";

    didNetwork = new HcsDidNetworkBuilder()
        .setNetwork(HederaNetwork.TESTNET)
        .setAppnetName(appnetName)
        .addAppnetDidServer(didServerUrl)
        .buildAndSignAddressBookCreateTransaction(tx -> tx
            .addKey(operatorKey.publicKey)
            .setMaxTransactionFee(FEE)
            .build(client))
        .buildAndSignDidTopicCreateTransaction(tx -> tx
            .setAdminKey(operatorKey.publicKey)
            .setMaxTransactionFee(FEE)
            // .setSubmitKey(operatorKey.publicKey)
            .setTopicMemo(didTopicMemo)
            .build(client))
        .buildAndSignVcTopicCreateTransaction(tx -> tx
            .setAdminKey(operatorKey.publicKey)
            .setMaxTransactionFee(FEE)
            // .setSubmitKey(operatorKey.publicKey)
            .setTopicMemo(vcTopicMemo)
            .build(client))
        .execute(client);
  }

  @Test
  @Order(1)
  void testCreate() {
    final DidDocumentOperation operation = DidDocumentOperation.CREATE;
    HcsDidPlainMessage message = sendDidTransaction(hcsDid, didDocument, operation, err -> assertNull(err));

    // Check results
    assertNotNull(message);
    assertNotNull(message.getDidDocument());

    assertEquals(hcsDid.toDid(), message.getDid());
    assertTrue(message.isValid());
    assertEquals(operation, message.getDidOperation());
  }

  @Test
  @Order(2)
  void testResolveAfterCreate() {
    String didString = hcsDid.toDid();

    HcsDidPlainMessage msg = resolveDid(didString, err -> assertNull(err));

    assertNotNull(msg);
    assertEquals(didString, msg.getDid());
    assertTrue(msg.isValid());
    assertEquals(DidDocumentOperation.CREATE, msg.getDidOperation());
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

    GsonBuilder gsonBuilder = new GsonBuilder();
    Gson gson = gsonBuilder.disableHtmlEscaping().excludeFieldsWithoutExposeAnnotation().create();
    String newDoc = gson.toJson(jsonElement);

    final DidDocumentOperation operation = DidDocumentOperation.UPDATE;
    HcsDidPlainMessage message = sendDidTransaction(hcsDid, newDoc, operation, err -> assertNull(err));

    // Check results
    assertNotNull(message);
    assertEquals(newDoc, message.getDidDocument());
    assertEquals(operation, message.getDidOperation());
  }

  @Test
  @Order(4)
  void testResolveAfterUpdate() {
    String didString = hcsDid.toDid();

    HcsDidPlainMessage msg = resolveDid(didString, err -> assertNull(err));

    assertNotNull(msg);
    assertEquals(didString, msg.getDid());
    assertTrue(msg.isValid());
    assertEquals(DidDocumentOperation.UPDATE, msg.getDidOperation());
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

    GsonBuilder gsonBuilder = new GsonBuilder();
    Gson gson = gsonBuilder.disableHtmlEscaping().excludeFieldsWithoutExposeAnnotation().create();
    String deletedDoc = gson.toJson(jsonElement);

    final DidDocumentOperation operation = DidDocumentOperation.DELETE;
    HcsDidPlainMessage message = sendDidTransaction(hcsDid, deletedDoc, operation, err -> assertNull(err));

    // Check results
    assertNotNull(message);
    assertEquals(deletedDoc, message.getDidDocument());
    assertEquals(operation, message.getDidOperation());
  }

  @Test
  @Order(6)
  void testResolveAfterDelete() {
    String didString = hcsDid.toDid();

    HcsDidPlainMessage msg = resolveDid(didString, err -> assertNull(err));

    assertNotNull(msg);
    assertEquals(didString, msg.getDid());
    assertTrue(msg.isValid());
    assertEquals(DidDocumentOperation.DELETE, msg.getDidOperation());
    assertNotEquals(didDocument, msg.getDidDocument());
  }

  @Test
  @Order(7)
  void testResolveAfterDeleteAndAnotherInvalidSubmit() {
    sendDidTransaction(hcsDid, didDocument, DidDocumentOperation.UPDATE, err -> assertNull(err));

    String didString = hcsDid.toDid();
    HcsDidPlainMessage msg = resolveDid(didString, err -> assertNull(err));

    assertNotNull(msg);
    assertEquals(didString, msg.getDid());
    assertTrue(msg.isValid());
    assertEquals(DidDocumentOperation.DELETE, msg.getDidOperation());
    assertNotEquals(didDocument, msg.getDidDocument());
  }

  private HcsDidPlainMessage sendDidTransaction(HcsDid did, String didDocumentJson, DidDocumentOperation operation,
      Consumer<Throwable> onError) {
    AtomicReference<HcsDidPlainMessage> messageRef = new AtomicReference<>(null);

    // Build and execute transaction
    didNetwork.createDidTransaction(operation)
        .setDidDocument(didDocumentJson)
        .signDidDocument(doc -> did.getPrivateDidRootKey().orElseThrow(() -> new NullPointerException()).sign(doc))
        .buildAndSignTransaction(tx -> tx.setMaxTransactionFee(FEE).build(client))
        .onDidDocumentReceived(msg -> messageRef.set(msg))
        .onError(onError)
        .execute(client, mirrorClient);

    // Wait until consensus is reached and mirror node received the DID document, but with max. time limit.
    Awaitility.await().atMost(MIRROR_NODE_TIMEOUT).until(() -> messageRef.get() != null);

    return messageRef.get();
  }

  private HcsDidPlainMessage resolveDid(String didString, Consumer<Throwable> onError) {
    AtomicReference<Map<String, HcsDidPlainMessage>> mapRef = new AtomicReference<>(null);

    // Now resolve the DID.
    didNetwork.getResolver()
        .setTimeout(NO_MORE_MESSAGES_TIMEOUT)
        .addDid(didString)
        .onError(onError)
        .whenFinished(m -> mapRef.set(m))
        .execute(mirrorClient);

    // Wait until mirror node resolves the DID.
    Awaitility.await().atMost(MIRROR_NODE_TIMEOUT).until(() -> mapRef.get() != null);

    Map<String, HcsDidPlainMessage> map = mapRef.get();

    return map.get(didString);
  }
  // TODO implement encryption decryption tests.

}
