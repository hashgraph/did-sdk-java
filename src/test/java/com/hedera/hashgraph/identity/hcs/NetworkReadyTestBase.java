package com.hedera.hashgraph.identity.hcs;

import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.common.base.Strings;
import com.google.gson.JsonParseException;
import com.hedera.hashgraph.identity.DidMethodOperation;
import com.hedera.hashgraph.identity.hcs.did.HcsDid;
import com.hedera.hashgraph.identity.hcs.did.HcsDidMessage;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcMessage;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcOperation;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.FileId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import io.github.cdimascio.dotenv.Dotenv;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base class for test classes that need a hedera identity network set up before running.
 */
public abstract class NetworkReadyTestBase {
  protected static final Duration MIRROR_NODE_TIMEOUT = Duration.ofSeconds(15);
  protected static final long NO_MORE_MESSAGES_TIMEOUT = Duration.ofSeconds(10).toMillis();
  protected static final Hbar FEE = new Hbar(2);

  protected static final Consumer<Throwable> EXPECT_NO_ERROR = err -> {
    err.printStackTrace();
    assertNull(err);
  };

  protected Client client;

  protected AccountId operatorId;
  protected PrivateKey operatorKey;

  protected HcsIdentityNetwork didNetwork;
  protected String network;

  protected abstract void beforeAll();

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
    switch (network.toUpperCase()) {
      case "MAINNET":
        client = Client.forMainnet();
        break;
      case "TESTNET":
        client = Client.forTestnet();
        break;
      case "PREVIEWNET":
        client = Client.forPreviewnet();
        break;
      default:
        throw new Exception("Illegal argument for network.");
    }


    // Set the operator account ID and operator private key
    client.setOperator(operatorId, operatorKey);

    // If identity network is provided as environment variable read from there, otherwise setup new one:
    String abJson = dotenv.get("EXISTING_ADDRESS_BOOK_JSON");
    String abFileId = dotenv.get("EXISTING_ADDRESS_BOOK_FILE_ID");
    if (Strings.isNullOrEmpty(abJson) || Strings.isNullOrEmpty(abFileId)) {
      setupIdentityNetwork();
    } else {
      AddressBook addressBook = AddressBook.fromJson(abJson, FileId.fromString(abFileId));
      didNetwork = HcsIdentityNetwork.fromAddressBook(network, addressBook);
    }

    beforeAll();
  }

  @AfterAll
  void cleanup() {
    try {
      if (client != null) {
        client.close();
      }

      if (client != null) {
        client.close();
      }
    } catch (Exception e) {
      // ignore
    }
  }

  void setupIdentityNetwork() throws PrecheckStatusException, ReceiptStatusException, TimeoutException {
    final String appnetName = "Test Identity SDK appnet";
    final String didServerUrl = "http://localhost:3000/api/v1";
    final String didTopicMemo = "Test Identity SDK appnet DID topic";
    final String vcTopicMemo = "Test Identity SDK appnet VC topic";

    didNetwork = new HcsIdentityNetworkBuilder()
            .setNetwork(network)
            .setAppnetName(appnetName)
            .addAppnetDidServer(didServerUrl)
            .setPublicKey(operatorKey.getPublicKey())
            .setMaxTransactionFee(FEE)
            .setDidTopicMemo(didTopicMemo)
            .setVCTopicMemo(vcTopicMemo)
            .execute(client);
    System.out.println("New identity network created: " + appnetName);
    System.out.println("Sleeping 10s to allow propagation of new topics to mirror node");
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  protected MessageEnvelope<HcsDidMessage> sendDidTransaction(HcsDid did, String didDocumentJson,
                                                              DidMethodOperation operation,
                                                              Consumer<Throwable> onError) {
    AtomicReference<MessageEnvelope<HcsDidMessage>> messageRef = new AtomicReference<>(null);

    // Build and execute transaction
    didNetwork.createDidTransaction(operation)
            .setDidDocument(didDocumentJson)
            .signMessage(doc -> did.getPrivateDidRootKey().orElseThrow(() -> new NullPointerException()).sign(doc))
            .buildAndSignTransaction(tx -> tx.setMaxTransactionFee(FEE))
            .onMessageConfirmed(msg -> messageRef.set(msg))
            .onError(onError)
            .execute(client);

    // Wait until consensus is reached and mirror node received the DID document, but with max. time limit.
    Awaitility.await().atMost(MIRROR_NODE_TIMEOUT).until(() -> messageRef.get() != null);

    return messageRef.get();
  }

  protected MessageEnvelope<HcsDidMessage> resolveDid(String didString, Consumer<Throwable> onError) {
    AtomicReference<Map<String, MessageEnvelope<HcsDidMessage>>> mapRef = new AtomicReference<>(null);

    // Now resolve the DID.
    didNetwork.getDidResolver()
            .addDid(didString)
            .setTimeout(NO_MORE_MESSAGES_TIMEOUT)
            .onError(onError)
            .whenFinished(m -> mapRef.set(m))
            .execute(client);

    // Wait until mirror node resolves the DID.
    Awaitility.await().atMost(MIRROR_NODE_TIMEOUT).until(() -> mapRef.get() != null);

    return mapRef.get().get(didString);
  }

  protected MessageEnvelope<HcsVcMessage> sendVcTransaction(
          HcsVcOperation operation, String credentialHash, PrivateKey signingKey, Consumer<Throwable> onError) {
    AtomicReference<MessageEnvelope<HcsVcMessage>> messageRef = new AtomicReference<>(null);

    // Build and execute transaction
    didNetwork.createVcTransaction(operation, credentialHash, signingKey.getPublicKey())
            .signMessage(doc -> signingKey.sign(doc))
            .buildAndSignTransaction(tx -> tx.setMaxTransactionFee(FEE))
            .onMessageConfirmed(msg -> messageRef.set(msg))
            .onError(onError)
            .execute(client);

    // Wait until consensus is reached and mirror node received the DID document, but with max. time limit.
    Awaitility.await().atMost(MIRROR_NODE_TIMEOUT).until(() -> messageRef.get() != null);

    return messageRef.get();
  }

  protected MessageEnvelope<HcsVcMessage> resolveVcStatus(String credentialHash,
                                                          Function<String, Collection<PublicKey>> provider, Consumer<Throwable> onError) {
    AtomicReference<Map<String, MessageEnvelope<HcsVcMessage>>> mapRef = new AtomicReference<>(null);

    // Now resolve the DID.
    didNetwork.getVcStatusResolver(provider)
            .addCredentialHash(credentialHash)
            .setTimeout(NO_MORE_MESSAGES_TIMEOUT)
            .onError(onError)
            .whenFinished(m -> mapRef.set(m))
            .execute(client);

    // Wait until mirror node resolves the DID.
    Awaitility.await().atMost(MIRROR_NODE_TIMEOUT).until(() -> mapRef.get() != null);

    return mapRef.get().get(credentialHash);
  }

}
