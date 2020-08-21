package com.hedera.hashgraph.identity.hcs.example.appnet;

import com.google.common.base.Strings;
import com.hedera.hashgraph.identity.HederaNetwork;
import com.hedera.hashgraph.identity.hcs.AddressBook;
import com.hedera.hashgraph.identity.hcs.HcsIdentityNetwork;
import com.hedera.hashgraph.identity.hcs.HcsIdentityNetworkBuilder;
import com.hedera.hashgraph.identity.hcs.MessageListener;
import com.hedera.hashgraph.identity.hcs.did.HcsDidMessage;
import com.hedera.hashgraph.identity.hcs.example.appnet.handlers.DemoHandler;
import com.hedera.hashgraph.identity.hcs.example.appnet.handlers.DidHandler;
import com.hedera.hashgraph.identity.hcs.example.appnet.handlers.VcHandler;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcMessage;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.HederaNetworkException;
import com.hedera.hashgraph.sdk.HederaStatusException;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PublicKey;
import com.hedera.hashgraph.sdk.file.FileId;
import com.hedera.hashgraph.sdk.mirror.MirrorClient;
import io.github.cdimascio.dotenv.Dotenv;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.server.RatpackServer;

/**
 * Example appnet server that exposes DID and VC REST APIs.
 * This application uses only in-memory storage for demonstration purposes.
 * Upon server shutdown all data is lost.
 * The listener starts listening from now on (minus buffer time of a few seconds).
 * As it is a demonstration application, it only operates on data received at runtime.
 * All messages submitted to the same topic in the past are ignored.
 */
public class AppnetServer {
  private static Logger log = LoggerFactory.getLogger(AppnetServer.class);
  private static final int SERVER_PORT = 5050;

  private Client client;
  private MirrorClient mirrorClient;

  private HcsIdentityNetwork identityNetwork;

  private DidHandler didHandler;
  private VcHandler vcHandler;
  private DemoHandler demoHandler;

  private RatpackServer apiServer;
  private AppnetStorage storage;
  private MessageListener<HcsDidMessage> didListener;
  private MessageListener<HcsVcMessage> vcListener;

  /**
   * Server startup method.
   *
   * @param args Command line arguments
   */
  public static void main(final String[] args) {
    log.info("Starting appnet API server...");

    AppnetServer server = new AppnetServer();
    server.startUp();
  }

  /**
   * Starts up the API server and initializes dependent services.
   */
  private void startUp() {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        shutDown();
      }
    });

    try {
      initHederaIdentityNetwork();
      Thread.sleep(10000);
      initStorageAndTopicListeners();
      initHandlers();
      startApiServer();

      log.info("Appnet API server is ready on port: " + SERVER_PORT);
    } catch (Exception e) {
      log.error("Error during startup: ", e);
    }
  }

  /**
   * Initializes appnet's local storage and HCS topic listeners.
   * This application uses only in-memory storage for demonstration purposes.
   * Upon server shutdown all data is lost.
   */
  private void initStorageAndTopicListeners() {
    log.info("Initializing storage and topic listeners...");
    storage = new AppnetStorage();

    startDidTopicListener();
    startVcTopicListener();
  }

  /**
   * Starts listening to the HCS topic for DID messages.
   * The listener starts listening from now on (minus buffer time of a few seconds).
   * As it is a demonstration application, it only operates on data received at runtime.
   * All messages submitted to the same topic in the past are ignored.
   */
  private void startDidTopicListener() {
    if (didListener != null) {
      didListener.unsubscribe();
    }

    Instant startTime = Instant.now().minusSeconds(10);
    didListener = identityNetwork.getDidTopicListener()
        .setStartTime(startTime)
        .onInvalidMessageReceived((resp, reason) -> {
          log.warn("Invalid message received from DID topic: " + reason);
          log.warn(new String(resp.message, StandardCharsets.UTF_8));
        })
        .onError(e -> {
          Code code = null;
          if (e instanceof StatusRuntimeException) {
            code = ((StatusRuntimeException) e).getStatus().getCode();
          }

          if (Code.UNAVAILABLE.equals(code)) {
            // Restart if listener crashed.
            startDidTopicListener();
          } else {
            log.error("Error while processing message from DID topic: ", e);
          }
        })
        .subscribe(mirrorClient, envelope -> storage.storeDid(envelope));
  }

  /**
   * Starts listening to the HCS topic for verifiable credential status messages.
   * The listener starts listening from now on (minus buffer time of a few seconds).
   * As it is a demonstration application, it only operates on data received at runtime.
   * All messages submitted to the same topic in the past are ignored.
   */
  private void startVcTopicListener() {
    if (vcListener != null) {
      vcListener.unsubscribe();
    }

    Instant startTime = Instant.now().minusSeconds(10);
    vcListener = identityNetwork.getVcTopicListener(vc -> storage.getAcceptableCredentialHashPublicKeys(vc))
        .setStartTime(startTime)
        .onInvalidMessageReceived((resp, reason) -> {
          log.warn("Invalid message received from VC topic: " + reason);
          log.warn(new String(resp.message, StandardCharsets.UTF_8));
        })
        .onError(e -> {
          Code code = null;
          if (e instanceof StatusRuntimeException) {
            code = ((StatusRuntimeException) e).getStatus().getCode();
          }

          if (Code.UNAVAILABLE.equals(code)) {
            // Restart if listener crashed.
            startVcTopicListener();
          } else {
            log.error("Error while processing message from VC topic: ", e);
          }
        })
        .subscribe(mirrorClient, envelope -> storage.storeVcStatus(envelope));
  }

  /**
   * Initializes HTTP request handlers.
   */
  private void initHandlers() {
    log.info("Initializing request handlers...");
    didHandler = new DidHandler(identityNetwork, storage);
    vcHandler = new VcHandler(identityNetwork, storage);
    demoHandler = new DemoHandler(identityNetwork, storage);
  }

  /**
   * Starts up the API server.
   *
   * @throws Exception In case startup failed.
   */
  private void startApiServer() throws Exception {
    apiServer = RatpackServer.start(server -> server
        .serverConfig(b -> b.port(SERVER_PORT).registerShutdownHook(true).findBaseDir())
        .handlers(chain -> chain
            .all(new CommonHeaders())
            .get(ctx -> ctx.render("This is an example appnet API server.\n"
                + "Please refer to documentation for more details about available APIs."))

            // REST API endpoints for DID
            .path("did", ctx -> ctx.byMethod(m -> m
                .post(() -> didHandler.create(ctx))
                .put(() -> didHandler.update(ctx))
                .delete(() -> didHandler.delete(ctx))
                .get(() -> didHandler.resolve(ctx))))
            .post("did-submit", ctx -> didHandler.submit(ctx, client, mirrorClient))

            // REST API endpoints for VC
            .path("vc/:credentialHash", ctx -> ctx.byMethod(m -> m
                .post(() -> vcHandler.issue(ctx))
                .put(() -> vcHandler.suspend(ctx))
                .patch(() -> vcHandler.resume(ctx))
                .delete(() -> vcHandler.revoke(ctx))
                .get(() -> vcHandler.resolveVcStatus(ctx))))
            .post("vc-submit", ctx -> vcHandler.submit(ctx, client, mirrorClient))

            // REST API endpoints for demo functions that in a normal environment would be run on the client side.
            .post("demo/generate-did", ctx -> demoHandler.generateDid(ctx))
            .post("demo/sign-did-message", ctx -> demoHandler.signDidMessage(ctx))
            .post("demo/generate-driving-license", ctx -> demoHandler.generateDrivingLicense(ctx))
            .post("demo/sign-vc-message", ctx -> demoHandler.signVcMessage(ctx))
            .get("demo/get-credential-hash", ctx -> demoHandler.determineCredentialHash(ctx))

            // Schema files
            .files(f -> f.dir("schemas").files("driving-license-schema.json"))

            ));
  }

  /**
   * Shuts the server down stopping all listeners and cleaning up resources.
   */
  private void shutDown() {
    log.info("Shutting down appnet API server...");
    try {
      if (didListener != null) {
        didListener.unsubscribe();
      }

      if (vcListener != null) {
        vcListener.unsubscribe();
      }

      if (client != null) {
        client.close();
      }

      if (mirrorClient != null) {
        mirrorClient.close();
      }
    } catch (Exception e) {
      log.error("Error during shutdown: ", e);
    }
    log.info("Appnet API server stopped.");
  }

  /**
   * Initializes Hedera Identity network.
   * That is, it will read the environment variables and if they are present,
   * it will use them to set up the network.
   * Otherwise a new identity network will be set up:
   * - address book in Hedera File Service
   * - DID topic in HCS
   * - VC topic in HCS
   *
   * @throws HederaNetworkException In case communication with Hedera network fails.
   * @throws HederaStatusException  In case setting up identity network artifacts fail.
   */
  private void initHederaIdentityNetwork() throws HederaNetworkException, HederaStatusException {
    log.info("Initializing identity network...");
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().load();

    // Grab the OPERATOR_ID and OPERATOR_KEY from environment variable
    final AccountId operatorId = AccountId.fromString(Objects.requireNonNull(dotenv.get("OPERATOR_ID")));
    final Ed25519PrivateKey operatorKey = Ed25519PrivateKey
        .fromString(Objects.requireNonNull(dotenv.get("OPERATOR_KEY")));

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
      setupIdentityNetwork(operatorKey.publicKey);
    } else {
      AddressBook addressBook = AddressBook.fromJson(abJson, FileId.fromString(abFileId));
      identityNetwork = HcsIdentityNetwork.fromAddressBook(HederaNetwork.TESTNET, addressBook);
    }

    log.info("Identity network initialized: " + identityNetwork.getAddressBook().getAppnetName());
    log.info("Address book FileId: " + identityNetwork.getAddressBook().getFileId());
    log.info("DID TopicId: " + identityNetwork.getAddressBook().getDidTopicId());
    log.info("VC TopicId: " + identityNetwork.getAddressBook().getVcTopicId());
  }

  /**
   * Creates a new identity network artifacts.
   * - address book in Hedera File Service
   * - DID topic in HCS
   * - VC topic in HCS
   *
   * @param  publicKey              Public key of the account operator.
   * @throws HederaNetworkException In case communication with Hedera network fails.
   * @throws HederaStatusException  In case setting up identity network artifacts fail.
   */
  private void setupIdentityNetwork(final Ed25519PublicKey publicKey)
      throws HederaNetworkException, HederaStatusException {
    log.info("Setting up new identity network...");
    final Hbar fee = new Hbar(2);
    final String appnetName = "Example appnet using Hedera Identity SDK";
    final String didServerUrl = "http://localhost:5050/";
    final String didTopicMemo = "Example appnet DID topic";
    final String vcTopicMemo = "Example appnet VC topic";

    identityNetwork = new HcsIdentityNetworkBuilder()
        .setNetwork(HederaNetwork.TESTNET)
        .setAppnetName(appnetName)
        .addAppnetDidServer(didServerUrl)
        .buildAndSignAddressBookCreateTransaction(tx -> tx
            .addKey(publicKey)
            .setMaxTransactionFee(fee)
            .build(client))
        .buildAndSignDidTopicCreateTransaction(tx -> tx
            .setAdminKey(publicKey)
            .setMaxTransactionFee(fee)
            // .setSubmitKey(operatorKey.publicKey)
            .setTopicMemo(didTopicMemo)
            .build(client))
        .buildAndSignVcTopicCreateTransaction(tx -> tx
            .setAdminKey(publicKey)
            .setMaxTransactionFee(fee)
            // .setSubmitKey(operatorKey.publicKey)
            .setTopicMemo(vcTopicMemo)
            .build(client))
        .execute(client);

    log.info("New identity network created: " + appnetName);
    log.info("Sleeping 10s to allow propagation of new topics to mirror node");
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
