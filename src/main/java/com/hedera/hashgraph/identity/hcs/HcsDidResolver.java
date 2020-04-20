package com.hedera.hashgraph.identity.hcs;

import com.hedera.hashgraph.identity.DidDocumentOperation;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.mirror.MirrorClient;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Resolves the DID from Hedera network.
 */
public class HcsDidResolver {

  /**
   * Default time to wait before finishing resolution and after the last message was received.
   */
  public static final long DEFAULT_TIMEOUT = 30_000;

  private BiFunction<byte[], Instant, byte[]> decrypter;
  private final ConsensusTopicId topicId;

  private Consumer<Map<String, HcsDidPlainMessage>> resultsHandler;
  private Consumer<Throwable> errorHandler;

  private final Map<String, HcsDidPlainMessage> results;
  private final ScheduledExecutorService executorService;
  private final AtomicLong lastMessageArrivalTime;

  private HcsDidTopicListener listener;
  private long noMoreMessagesTimeout;

  /**
   * Instantiates a new DID resolver for the given DID topic.
   *
   * @param topicId The HCS DID topic ID where message will be submitted.
   */
  public HcsDidResolver(final ConsensusTopicId topicId) {
    this.topicId = topicId;
    this.results = new HashMap<>();
    this.executorService = Executors.newScheduledThreadPool(2);
    this.noMoreMessagesTimeout = DEFAULT_TIMEOUT;
    this.lastMessageArrivalTime = new AtomicLong(System.currentTimeMillis());
  }

  /**
   * Adds a DID to resolve.
   *
   * @param  did The DID string.
   * @return     This resolver instance.
   */
  public HcsDidResolver addDid(final String did) {
    if (did != null) {
      results.put(did, null);
    }
    return this;
  }

  /**
   * Adds multiple DIDs to resolve.
   *
   * @param  dids The set of DID strings.
   * @return      This resolver instance.
   */
  public HcsDidResolver addDids(final Set<String> dids) {
    if (dids != null) {
      dids.forEach(d -> addDid(d));
    }

    return this;
  }

  /**
   * Resolves the given DIDs.
   *
   * @param mirrorClient The mirror node client.
   */
  public void execute(final MirrorClient mirrorClient) {
    new Validator().checkValidationErrors("Resolver not executed: ", v -> validate(v));

    listener = new HcsDidTopicListener(topicId);
    listener.setStartTime(Instant.MIN)
        .setEndTime(Instant.now())
        .setIgnoreInvalidMessages(true)
        .setIgnoreErrors(false)
        .onError(errorHandler)
        .onDecrypt(decrypter)
        .subscribe(mirrorClient, msg -> handleMessage(msg));

    lastMessageArrivalTime.set(System.currentTimeMillis());
    waitOrFinish();
  }

  private void waitOrFinish() {
    // Check if the task should be rescheduled as new message arrived.
    long timeDiff = System.currentTimeMillis() - lastMessageArrivalTime.get();
    if (timeDiff < noMoreMessagesTimeout) {
      Runnable finishTask = () -> waitOrFinish();

      executorService.schedule(finishTask, noMoreMessagesTimeout - timeDiff, TimeUnit.MILLISECONDS);
      return;
    }

    // Finish the task
    resultsHandler.accept(results);

    // Stop listening for new messages.
    if (listener != null) {
      listener.unsubscribe();
    }

    // Stop the timeout executor.
    if (executorService != null) {
      executorService.shutdown();
    }
  }

  /**
   * Handles incoming DID messages from DID Topic on a mirror node.
   *
   * @param message The parsed message in a plain form.
   */
  protected void handleMessage(final HcsDidPlainMessage message) {
    lastMessageArrivalTime.set(System.currentTimeMillis());

    // Skip messages that are not relevant for requested DID's
    if (!results.containsKey(message.getDid())) {
      return;
    }

    // Also skip messages that are older than the once collected or if we already have a DELETE message
    HcsDidPlainMessage existingMessage = results.get(message.getDid());
    if (existingMessage != null
        && (existingMessage.getConsensusTimestamp().isAfter(message.getConsensusTimestamp())
            || (DidDocumentOperation.DELETE.equals(existingMessage.getDidOperation())
                && !DidDocumentOperation.DELETE.equals(message.getDidOperation())))) {
      return;
    }

    // Add valid message to the results
    results.put(message.getDid(), message);
  }

  /**
   * Defines a handler for resolution results.
   * This will be called when the resolution process is finished.
   *
   * @param  handler The results handler.
   * @return         This resolver instance.
   */
  public HcsDidResolver whenFinished(final Consumer<Map<String, HcsDidPlainMessage>> handler) {
    this.resultsHandler = handler;
    return this;
  }

  /**
   * Defines a handler for errors when they happen during resolution.
   *
   * @param  handler The error handler.
   * @return         This resolver instance.
   */
  public HcsDidResolver onError(final Consumer<Throwable> handler) {
    this.errorHandler = handler;
    return this;
  }

  /**
   * Defines a maximum time in milliseconds to wait for new messages from the topic.
   * Default is 30 seconds.
   *
   * @param  timeout The timeout in milliseconds to wait for new messages from the topic.
   * @return         This resolver instance.
   */
  public HcsDidResolver setTimeout(final long timeout) {
    this.noMoreMessagesTimeout = timeout;
    return this;
  }

  /**
   * Defines decryption function that decrypts submitted the DID and DID document after consensus is reached.
   * Decryption function must accept a byte array of encrypted message and an Instant that is its consensus timestamp,
   * If decrypter is not specified, encrypted messages will be ignored.
   *
   * @param  decrypter The decrypter to use.
   * @return           This resolver instance.
   */
  public HcsDidResolver onDecrypt(final BiFunction<byte[], Instant, byte[]> decrypter) {
    this.decrypter = decrypter;
    return this;
  }

  /**
   * Runs validation logic.
   *
   * @param validator The errors validator.
   */
  protected void validate(final Validator validator) {
    validator.require(!results.isEmpty(), "No DIDs to resolve.");
    validator.require(topicId != null, "DID topic ID not defined.");
    validator.require(resultsHandler != null, "Results handler 'whenFinished' not defined.");
  }
}
