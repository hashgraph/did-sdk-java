package com.hedera.hashgraph.identity.hcs.example.appnet;

import com.google.common.base.Strings;
import com.hedera.hashgraph.identity.hcs.AddressBook;
import com.hedera.hashgraph.identity.hcs.HcsIdentityNetwork;
import com.hedera.hashgraph.identity.hcs.HcsIdentityNetworkBuilder;
import com.hedera.hashgraph.identity.hcs.MessageListener;
import com.hedera.hashgraph.identity.hcs.did.HcsDidMessage;
import com.hedera.hashgraph.identity.hcs.example.appnet.handlers.DemoHandler;
import com.hedera.hashgraph.identity.hcs.example.appnet.handlers.DidHandler;
import com.hedera.hashgraph.identity.hcs.example.appnet.handlers.VcHandler;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcMessage;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.FileId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import io.github.cdimascio.dotenv.Dotenv;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
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
  private static final int SERVER_PORT = 5050;
  private static Logger log = LoggerFactory.getLogger(AppnetServer.class);
  private Client client;

  private HcsIdentityNetwork identityNetwork;

  private DidHandler didHandler;
  private VcHandler vcHandler;
  private DemoHandler demoHandler;

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
  private void initStorageAndTopicListeners() throws IOException, ClassNotFoundException {
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

    didListener = identityNetwork.getDidTopicListener()
            .setStartTime(storage.getLastDiDConsensusTimeStamp().plusNanos(1))
            .onInvalidMessageReceived((resp, reason) -> {
              log.warn("Invalid message received from DID topic: " + reason);
              log.warn(new String(resp.contents, StandardCharsets.UTF_8));
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
            .subscribe(client, envelope -> storage.storeDid(envelope));
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

    vcListener = identityNetwork.getVcTopicListener(vc -> storage.getAcceptableCredentialHashPublicKeys(vc))
            .setStartTime(storage.getLastVCConsensusTimeStamp().plusNanos(1))
            .onInvalidMessageReceived((resp, reason) -> {
              log.warn("Invalid message received from VC topic: " + reason);
              log.warn(new String(resp.contents, StandardCharsets.UTF_8));
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
            .subscribe(client, envelope -> storage.storeVcStatus(envelope));
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
    RatpackServer.start(server -> server
            .serverConfig(b -> b.port(SERVER_PORT).registerShutdownHook(true).findBaseDir())
            .handlers(chain -> chain
                    .all(new CommonHeaders())
                    .get(ctx -> ctx.render("This is an example appnet API server.\n"
                            + "Please refer to documentation for more details about available APIs."))

                    // REST API endpoints for DID
                    .path("did", ctx ->
                            ctx.byMethod(m -> m
                                    .get(() -> didHandler.resolve(ctx))
                                    .post(() -> didHandler.create(ctx))
                                    .put(() -> didHandler.update(ctx))
                                    .delete(() -> didHandler.delete(ctx)))
                    )
                    .post("did-resolve", ctx -> didHandler.resolve(ctx))
                    .post("did-submit", ctx -> didHandler.submit(ctx, client))

                    // REST API endpoints for VC
                    .path("vc/:credentialHash", ctx -> ctx.byMethod(m -> m
                            .post(() -> vcHandler.issue(ctx))
                            .put(() -> vcHandler.suspend(ctx))
                            .patch(() -> vcHandler.resume(ctx))
                            .delete(() -> vcHandler.revoke(ctx))
                            .get(() -> vcHandler.resolveVcStatus(ctx))))
                    .post("vc-submit", ctx -> vcHandler.submit(ctx, client))

                    // REST API endpoints for demo functions that in a normal environment
                    // would be run on the client side.
                    .post("demo/generate-did", ctx -> demoHandler.generateDid(ctx))
                    .post("demo/sign-did-message", ctx -> demoHandler.signDidMessage(ctx))
                    .post("demo/generate-driving-license", ctx -> demoHandler.generateDrivingLicense(ctx))
                    .post("demo/sign-vc-message", ctx -> demoHandler.signVcMessage(ctx))
                    .post("demo/get-credential-hash", ctx -> demoHandler.determineCredentialHash(ctx))

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

      if (client != null) {
        client.close();
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
   */
  private void initHederaIdentityNetwork()
          throws InterruptedException, ReceiptStatusException, PrecheckStatusException, TimeoutException {
    log.info("Initializing identity network...");
    Dotenv dotenv = Dotenv.configure().load();

    // Grab the network to use from environment variables
    final String network = Objects.requireNonNull(dotenv.get("NETWORK"));

    // Build Hedera testnet client

    // Grab the desired mirror from environment variables
    final String mirrorProvider = Objects.requireNonNull(dotenv.get("MIRROR_PROVIDER"));

    // Grab the mirror node address MIRROR_NODE_ADDRESS from environment variable
    String mirrorNodeAddress = "hcs." + network + ".mirrornode.hedera.com:5600";
    switch (network) {
      case "mainnet":
        client = Client.forMainnet();
        if ("kabuto".equals(mirrorProvider)) {
          mirrorNodeAddress = "api.kabuto.sh:50211";
        }
        break;
      case "testnet":
        client = Client.forTestnet();
        if ("kabuto".equals(mirrorProvider)) {
          mirrorNodeAddress = "api.testnet.kabuto.sh:50211";
        }
        break;
      default:
        log.error("invalid previewnet network for Kabuto, please edit .env file");
        throw new RuntimeException("invalid previewnet network for Kabuto, please edit .env file");
    }

    // Grab the OPERATOR_ID and OPERATOR_KEY from environment variable
    final AccountId operatorId = AccountId.fromString(Objects.requireNonNull(dotenv.get("OPERATOR_ID")));
    final PrivateKey operatorKey = PrivateKey
            .fromString(Objects.requireNonNull(dotenv.get("OPERATOR_KEY")));

    // Set the operator account ID and operator private key
    client.setOperator(operatorId, operatorKey);

    // Build the mirror node client
    client.setMirrorNetwork(List.of(mirrorNodeAddress));

    // If identity network is provided as environment variable read from there, otherwise setup new one:
    String abJson = dotenv.get("EXISTING_ADDRESS_BOOK_JSON");
    String abFileId = dotenv.get("EXISTING_ADDRESS_BOOK_FILE_ID");
    if (Strings.isNullOrEmpty(abJson)) {
      if (Strings.isNullOrEmpty(abFileId)) {
        // no file, no JSON, create from new
        setupIdentityNetwork(network, operatorKey.getPublicKey());
      } else {
        // We have a file ID, load from the network
        identityNetwork = HcsIdentityNetwork.fromAddressBookFile(client, network, FileId.fromString(abFileId));
      }
    } else {
      // we have json, use it
      AddressBook addressBook = AddressBook.fromJson(abJson, FileId.fromString(abFileId));
      identityNetwork = HcsIdentityNetwork.fromAddressBook(network, addressBook);
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
   * @param publicKey Public key of the account operator.
   * @param network   The network to use
   * @throws PrecheckStatusException in the event the transaction is not validated by the node
   * @throws ReceiptStatusException  in the event a receipt contains an error
   * @throws TimeoutException        in the event the client cannot connect to the network in a timely manner
   */
  private void setupIdentityNetwork(final String network, final PublicKey publicKey)
          throws PrecheckStatusException, ReceiptStatusException, TimeoutException {
    log.info("Setting up new identity network...");
    final Hbar fee = new Hbar(2);
    final String appnetName = "Example appnet using Hedera Identity SDK";
    final String didServerUrl = "http://localhost:5050/";
    final String didTopicMemo = "Example appnet DID topic";
    final String vcTopicMemo = "Example appnet VC topic";

    identityNetwork = new HcsIdentityNetworkBuilder()
            .setNetwork(network)
            .setAppnetName(appnetName)
            .addAppnetDidServer(didServerUrl)
            .setPublicKey(publicKey)
            .setMaxTransactionFee(fee)
            .setDidTopicMemo(didTopicMemo)
            .setVCTopicMemo(vcTopicMemo)
            .execute(client);

    log.info("New identity network created: " + appnetName);
    log.info("Sleeping 10s to allow propagation of new topics to mirror node");
    try {
      Thread.sleep(10_000);
    } catch (InterruptedException e) {
      log.error(e.getMessage());
    }
  }
}
