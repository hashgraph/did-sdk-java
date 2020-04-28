package com.hedera.hashgraph.identity.hcs;

import static java.security.SecureRandom.getInstanceStrong;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.Charsets;
import com.hedera.hashgraph.identity.HederaNetwork;
import com.hedera.hashgraph.identity.hcs.did.HcsDid;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.HederaNetworkException;
import com.hedera.hashgraph.sdk.HederaStatusException;
import com.hedera.hashgraph.sdk.LocalValidationException;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicInfo;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicInfoQuery;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PublicKey;
import com.hedera.hashgraph.sdk.file.FileCreateTransaction;
import com.hedera.hashgraph.sdk.file.FileId;
import io.github.cdimascio.dotenv.Dotenv;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
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
  private Ed25519PrivateKey operatorKey;
  private FileId addressBookFileId;

  /**
   * Initialize hedera clients and accounts.
   */
  @BeforeAll
  void setup() throws LocalValidationException, HederaNetworkException, HederaStatusException {
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().load();

    // Grab the OPERATOR_ID and OPERATOR_KEY from environment variable
    operatorId = AccountId.fromString(Objects.requireNonNull(dotenv.get("OPERATOR_ID")));
    operatorKey = Ed25519PrivateKey.fromString(Objects.requireNonNull(dotenv.get("OPERATOR_KEY")));

    // Build Hedera testnet client
    client = Client.forTestnet();

    // Set the operator account ID and operator private key
    client.setOperator(operatorId, operatorKey);

    // Create address book file
    addressBookFileId = new FileCreateTransaction()
        .setContents(ADDRESS_BOOK_JSON.getBytes(Charsets.UTF_8))
        .addKey(operatorKey.publicKey)
        .setMaxTransactionFee(FEE)
        .build(client)
        .execute(client)
        .getReceipt(client)
        .getFileId();
  }

  /**
   * Tests creation of a new identity network.
   *
   * @throws HederaStatusException  In case querying Hedera File Service fails.
   * @throws HederaNetworkException In case of querying Hedera File Service fails due to transport calls.
   */
  @Test
  void testCreateIdentityNetwork() throws HederaStatusException, HederaNetworkException {
    final String appnetName = "Test Identity SDK appnet";
    final String didServerUrl = "http://localhost:3000/api/v1";
    final String didTopicMemo = "Test Identity SDK appnet DID topic";
    final String vcTopicMemo = "Test Identity SDK appnet VC topic";

    HcsIdentityNetwork didNetwork = new HcsIdentityNetworkBuilder()
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

    assertNotNull(didNetwork);
    assertNotNull(didNetwork.getAddressBook());

    AddressBook addressBook = didNetwork.getAddressBook();

    assertNotNull(addressBook.getDidTopicId());
    assertNotNull(addressBook.getVcTopicId());
    assertNotNull(addressBook.getAppnetDidServers());
    assertNotNull(addressBook.getFileId());
    assertEquals(addressBook.getAppnetName(), appnetName);
    assertEquals(didNetwork.getNetwork(), HederaNetwork.TESTNET);

    // Test if HCS topics exist.

    // DID topic
    ConsensusTopicInfo didTopicInfo = new ConsensusTopicInfoQuery()
        .setTopicId(didNetwork.getDidTopicId())
        .execute(client);

    assertNotNull(didTopicInfo);
    assertEquals(didTopicInfo.topicMemo, didTopicMemo);

    // VC topic
    ConsensusTopicInfo vcTopicInfo = new ConsensusTopicInfoQuery()
        .setTopicId(didNetwork.getVcTopicId())
        .execute(client);

    assertNotNull(vcTopicInfo);
    assertEquals(vcTopicInfo.topicMemo, vcTopicMemo);

    // Test if address book file was created
    HcsIdentityNetwork createdNetwork = HcsIdentityNetwork.fromAddressBookFile(client, HederaNetwork.TESTNET,
        addressBook.getFileId(), FEE);
    assertNotNull(createdNetwork);
    assertEquals(addressBook.toJson(), createdNetwork.getAddressBook().toJson());
  }

  @Test
  void testInitNetworkFromJsonAddressBook() {
    AddressBook addressBook = AddressBook.fromJson(ADDRESS_BOOK_JSON, addressBookFileId);
    HcsIdentityNetwork didNetwork = HcsIdentityNetwork.fromAddressBook(HederaNetwork.TESTNET, addressBook);

    assertNotNull(didNetwork);
    assertNotNull(didNetwork.getAddressBook().getFileId());
    assertEquals(didNetwork.getNetwork(), HederaNetwork.TESTNET);
  }

  @Test
  void testInitNetworkFromDid() throws HederaNetworkException, HederaStatusException {
    // Generate HcsDid
    HcsDid did = new HcsDid(HederaNetwork.TESTNET, HcsDid.generateDidRootKey().publicKey, addressBookFileId);

    // Initialize network from this DID, reading address book file from Hedera File Service
    HcsIdentityNetwork didNetwork = HcsIdentityNetwork.fromHcsDid(client, did, FEE);

    assertNotNull(didNetwork);
    assertNotNull(didNetwork.getAddressBook().getFileId());
    assertEquals(didNetwork.getNetwork(), HederaNetwork.TESTNET);
    assertEquals(ADDRESS_BOOK_JSON, didNetwork.getAddressBook().toJson());
  }

  void checkTestGenerateDidForNetwork(HcsDid did, Ed25519PublicKey publicKey, String didTopicId, boolean withTid) {
    assertNotNull(did);
    assertEquals(HcsDid.publicKeyToIdString(publicKey), did.getIdString());
    assertEquals(did.getNetwork(), HederaNetwork.TESTNET);
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
    HcsIdentityNetwork didNetwork = HcsIdentityNetwork.fromAddressBook(HederaNetwork.TESTNET, addressBook);

    // Random DID with tid parameter
    HcsDid did = didNetwork.generateDid(true);
    assertTrue(did.getPrivateDidRootKey().isPresent());

    Ed25519PublicKey publicKey = did.getPrivateDidRootKey().orElseThrow(() -> new NullPointerException()).publicKey;
    checkTestGenerateDidForNetwork(did, publicKey, addressBook.getDidTopicId(), true);

    // Random DID without tid parameter
    did = didNetwork.generateDid(false);
    assertTrue(did.getPrivateDidRootKey().isPresent());

    publicKey = did.getPrivateDidRootKey().orElseThrow(() -> new NullPointerException()).publicKey;
    checkTestGenerateDidForNetwork(did, publicKey, addressBook.getDidTopicId(), false);

    // Secure Random DID with tid parameter
    did = didNetwork.generateDid(getInstanceStrong(), true);
    assertTrue(did.getPrivateDidRootKey().isPresent());
    publicKey = did.getPrivateDidRootKey().orElseThrow(() -> new NullPointerException()).publicKey;
    checkTestGenerateDidForNetwork(did, publicKey, addressBook.getDidTopicId(), true);

    // Secure Random DID without tid parameter
    did = didNetwork.generateDid(getInstanceStrong(), false);
    assertTrue(did.getPrivateDidRootKey().isPresent());
    publicKey = did.getPrivateDidRootKey().orElseThrow(() -> new NullPointerException()).publicKey;
    checkTestGenerateDidForNetwork(did, publicKey, addressBook.getDidTopicId(), false);

    // From existing public key with tid parameter.
    publicKey = HcsDid.generateDidRootKey().publicKey;
    did = didNetwork.generateDid(publicKey, true);
    checkTestGenerateDidForNetwork(did, publicKey, addressBook.getDidTopicId(), true);

    // From existing public key without tid parameter.
    publicKey = HcsDid.generateDidRootKey().publicKey;
    did = didNetwork.generateDid(publicKey, false);
    checkTestGenerateDidForNetwork(did, publicKey, addressBook.getDidTopicId(), false);
  }
}
