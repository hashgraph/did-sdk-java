package test.com.hedera.hashgraph.identity.hcs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.hedera.hashgraph.identity.DidDocumentOperation;
import com.hedera.hashgraph.identity.HederaNetwork;
import com.hedera.hashgraph.identity.hcs.HcsDid;
import com.hedera.hashgraph.identity.hcs.HcsDidNetwork;
import com.hedera.hashgraph.identity.hcs.HcsDidNetworkAddressBook;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Tests HCS identity network creation, instantiation and DID operations.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HcsDidNetworkCrudTest {
  public static final Duration TRANSACTION_TIMEOUT = Duration.ofSeconds(10);

  private Client client;
  private AccountId operatorId;
  private Ed25519PrivateKey operatorKey;
  private MirrorClient mirrorClient;

  /**
   * Address book of existing identity network used for unit testing.
   */
  private static final String ADDRESS_BOOK_JSON = "{\"appnetName\":\"Test Identity SDK appnet\",\"didTopicId\":\"0.0.214919\",\"vcTopicId\":\"0.0.214920\",\"appnetDidServers\":[\"http://localhost:3000/api/v1\"]}";
  private static final FileId ADDRESS_BOOK_FILE_ID = FileId.fromString("0.0.214921");

  // private static final String TEST_DID =
  // "did:hedera:testnet:GvaaoYtUg56pVyEZM3JCvC9t7VAn3wpWCHr1tqNtefZ4;hedera:testnet:fid=0.0.214921";
  // private static final String TEST_DID_PRIVATE_KEY_BASE58 = "EynDgimpFtqHUd8Emaburk9VYt8oyV7A2h6AQo7LDBBn";

  /**
   * Initialize hedera clients and accounts.
   */
  @BeforeAll
  void setup() {
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
  }

  @Test
  void testCreateUpdateDeleteDid() throws HederaNetworkException, HederaStatusException {
    HcsDidNetworkAddressBook addressBook = HcsDidNetworkAddressBook.fromJson(ADDRESS_BOOK_JSON, ADDRESS_BOOK_FILE_ID);
    HcsDidNetwork didNetwork = HcsDidNetwork.fromAddressBook(HederaNetwork.TESTNET, addressBook);

    Entry<Ed25519PrivateKey, HcsDid> entry = didNetwork.generateDid(false);
    Ed25519PrivateKey privateKey = entry.getKey();
    HcsDid hcsDid = entry.getValue();
    String didDocument = hcsDid.generateDidDocument().toJson();

    // We'll store did and it's document coming from mirror node in this map
    Map<String, String> map = new HashMap<>();

    // Build and execute transaction
    didNetwork.createDidTransaction(DidDocumentOperation.CREATE)
        .setDidDocument(didDocument)
        .signDidDocument(doc -> privateKey.sign(doc))
        .buildAndSignTransaction(tx -> tx.setMaxTransactionFee(new Hbar(2)).build(client))
        .onDidDocumentReceived((did, doc) -> map.put(did, doc))
        .execute(client, mirrorClient);

    // Wait until consensus is reached and mirror node received the DID document, but with max. time limit.
    Awaitility.await().atMost(TRANSACTION_TIMEOUT).until(() -> !map.isEmpty());

    // Check results
    assertFalse(map.isEmpty());
    String receivedDocument = map.get(hcsDid.toDid());

    assertNotNull(receivedDocument);
    System.out.println(receivedDocument);
  }
  // TODO implement missing RUD operations
  // TODO implement encryption decryption tests.
}
