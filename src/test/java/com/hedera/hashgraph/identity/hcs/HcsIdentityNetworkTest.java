package com.hedera.hashgraph.identity.hcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.Charsets;
import com.hedera.hashgraph.identity.hcs.did.HcsDid;
import com.hedera.hashgraph.identity.utils.MirrorNodeAddress;
import com.hedera.hashgraph.sdk.*;
import com.sun.tools.javac.util.List;
import io.github.cdimascio.dotenv.Dotenv;

import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Tests HCS identity network creation, instantiation and DID operations.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HcsIdentityNetworkTest {
  private static final Hbar FEE = new Hbar(2);
  private static final String ADDRESS_BOOK_JSON = "{\"appnetName\":\"Test Identity SDK appnet\",\"didTopicId\":\"0.0.214919\",\"vcTopicId\":\"0.0.214920\",\"appnetDidServers\":[\"http://localhost:3000/api/v1\"]}";

  private Client client;
  private AccountId operatorId;
  private PrivateKey operatorKey;
  private FileId addressBookFileId;
  private String network;

  /**
   * Initialize hedera clients and accounts.
   */
  @BeforeAll
  void setup() throws Exception {
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().load();

    // Grab the OPERATOR_ID and OPERATOR_KEY from environment variable
    operatorId = AccountId.fromString(Objects.requireNonNull(dotenv.get("OPERATOR_ID")));
    operatorKey = PrivateKey.fromString(Objects.requireNonNull(dotenv.get("OPERATOR_KEY")));

    // Grab the network to use from environment variables
    network = Objects.requireNonNull(dotenv.get("NETWORK"));

    // Build Hedera testnet client
    client = Client.forTestnet();
    client.setMirrorNetwork(List.of(MirrorNodeAddress.getAddress()));

    // Set the operator account ID and operator private key
    client.setOperator(operatorId, operatorKey);

    // Create address book file
    TransactionResponse response = new FileCreateTransaction()
            .setContents(ADDRESS_BOOK_JSON.getBytes(Charsets.UTF_8))
            .setKeys(operatorKey.getPublicKey())
            .setMaxTransactionFee(FEE)
            .execute(client);
    TransactionReceipt receipt = response.getReceipt(client);

    addressBookFileId = receipt.fileId;
  }

  /**
   * Tests creation of a new identity network.
   *
   * @throws PrecheckStatusException In case a transaction fails precheck.
   * @throws ReceiptStatusException  In case a receipt contains an error.
   * @throws TimeoutException        In case the client fails to communicate with the network in a timely manner
   */
  @Test
  void testCreateIdentityNetwork() throws PrecheckStatusException, ReceiptStatusException, TimeoutException {
    final String appnetName = "Test Identity SDK appnet";
    final String didServerUrl = "http://localhost:3000/api/v1";
    final String didTopicMemo = "Test Identity SDK appnet DID topic";
    final String vcTopicMemo = "Test Identity SDK appnet VC topic";

    HcsIdentityNetwork didNetwork = new HcsIdentityNetworkBuilder()
            .setNetwork(network)
            .setAppnetName(appnetName)
            .addAppnetDidServer(didServerUrl)
            .setPublicKey(operatorKey.getPublicKey())
            .setMaxTransactionFee(FEE)
            .setDidTopicMemo(didTopicMemo)
            .setVCTopicMemo(vcTopicMemo)
            .execute(client);

    assertNotNull(didNetwork);
    assertNotNull(didNetwork.getAddressBook());

    AddressBook addressBook = didNetwork.getAddressBook();

    assertNotNull(addressBook.getDidTopicId());
    assertNotNull(addressBook.getVcTopicId());
    assertNotNull(addressBook.getAppnetDidServers());
    assertNotNull(addressBook.getFileId());
    assertEquals(addressBook.getAppnetName(), appnetName);
    assertEquals(didNetwork.getNetwork(), network);

    // Test if HCS topics exist.

    // DID topic
    TopicInfo didTopicInfo = new TopicInfoQuery()
            .setTopicId(didNetwork.getDidTopicId())
            .execute(client);

    assertNotNull(didTopicInfo);
    assertEquals(didTopicInfo.topicMemo, didTopicMemo);

    // VC topic
    TopicInfo vcTopicInfo = new TopicInfoQuery()
            .setTopicId(didNetwork.getVcTopicId())
            .execute(client);

    assertNotNull(vcTopicInfo);
    assertEquals(vcTopicInfo.topicMemo, vcTopicMemo);

    // Test if address book file was created
    HcsIdentityNetwork createdNetwork = HcsIdentityNetwork.fromAddressBookFile(client, network,
            addressBook.getFileId());
    assertNotNull(createdNetwork);
    assertEquals(addressBook.toJson(), createdNetwork.getAddressBook().toJson());
  }

  @Test
  void testInitNetworkFromJsonAddressBook() {
    AddressBook addressBook = AddressBook.fromJson(ADDRESS_BOOK_JSON, addressBookFileId);
    HcsIdentityNetwork didNetwork = HcsIdentityNetwork.fromAddressBook(network, addressBook);

    assertNotNull(didNetwork);
    assertNotNull(didNetwork.getAddressBook().getFileId());
    assertEquals(didNetwork.getNetwork(), network);
  }

  @Test
  void testInitNetworkFromDid() throws TimeoutException, PrecheckStatusException {
    // Generate HcsDid
    HcsDid did = new HcsDid(network, HcsDid.generateDidRootKey().getPublicKey(), addressBookFileId);

    // Initialize network from this DID, reading address book file from Hedera File Service
    HcsIdentityNetwork didNetwork = HcsIdentityNetwork.fromHcsDid(client, did);

    assertNotNull(didNetwork);
    assertNotNull(didNetwork.getAddressBook().getFileId());
    assertEquals(didNetwork.getNetwork(), network);
    assertEquals(ADDRESS_BOOK_JSON, didNetwork.getAddressBook().toJson());
  }

  void checkTestGenerateDidForNetwork(HcsDid did, PublicKey publicKey, String didTopicId, boolean withTid) {
    assertNotNull(did);
    assertEquals(HcsDid.publicKeyToIdString(publicKey), did.getIdString());
    assertEquals(did.getNetwork(), network);
    assertEquals(did.getAddressBookFileId(), addressBookFileId);
    if (withTid) {
      assertEquals(did.getDidTopicId().toString(), didTopicId);
    } else {
      assertNull(did.getDidTopicId());
    }
    assertEquals(did.getMethod(), HcsDid.DID_METHOD);
  }

  @Test
  void testGenerateDidForNetwork() throws NoSuchAlgorithmException {
    AddressBook addressBook = AddressBook.fromJson(ADDRESS_BOOK_JSON, addressBookFileId);
    HcsIdentityNetwork didNetwork = HcsIdentityNetwork.fromAddressBook(network, addressBook);

    // Random DID with tid parameter
    HcsDid did = didNetwork.generateDid(true);
    assertTrue(did.getPrivateDidRootKey().isPresent());

    PublicKey publicKey = did.getPrivateDidRootKey().orElseThrow(() -> new NullPointerException()).getPublicKey();
    checkTestGenerateDidForNetwork(did, publicKey, addressBook.getDidTopicId(), true);

    // Random DID without tid parameter
    did = didNetwork.generateDid(false);
    assertTrue(did.getPrivateDidRootKey().isPresent());

    publicKey = did.getPrivateDidRootKey().orElseThrow(() -> new NullPointerException()).getPublicKey();
    checkTestGenerateDidForNetwork(did, publicKey, addressBook.getDidTopicId(), false);

    // Secure Random DID with tid parameter
    did = didNetwork.generateDid(true);
    assertTrue(did.getPrivateDidRootKey().isPresent());
    publicKey = did.getPrivateDidRootKey().orElseThrow(() -> new NullPointerException()).getPublicKey();
    checkTestGenerateDidForNetwork(did, publicKey, addressBook.getDidTopicId(), true);

    // Secure Random DID without tid parameter
    did = didNetwork.generateDid(false);
    assertTrue(did.getPrivateDidRootKey().isPresent());
    publicKey = did.getPrivateDidRootKey().orElseThrow(() -> new NullPointerException()).getPublicKey();
    checkTestGenerateDidForNetwork(did, publicKey, addressBook.getDidTopicId(), false);

    // From existing public key with tid parameter.
    publicKey = HcsDid.generateDidRootKey().getPublicKey();
    did = didNetwork.generateDid(publicKey, true);
    checkTestGenerateDidForNetwork(did, publicKey, addressBook.getDidTopicId(), true);

    // From existing public key without tid parameter.
    publicKey = HcsDid.generateDidRootKey().getPublicKey();
    did = didNetwork.generateDid(publicKey, false);
    checkTestGenerateDidForNetwork(did, publicKey, addressBook.getDidTopicId(), false);
  }
}
