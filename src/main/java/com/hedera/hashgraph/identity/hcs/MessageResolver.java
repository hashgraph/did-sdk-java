package com.hedera.hashgraph.identity.hcs;

import com.hedera.hashgraph.identity.utils.Validator;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.mirror.MirrorClient;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public abstract class MessageResolver<T extends Message> {

  /**
   * Default time to wait before finishing resolution and after the last message was received.
   */
  public static final long DEFAULT_TIMEOUT = 30_000;

  protected final ConsensusTopicId topicId;
  protected final Map<String, MessageEnvelope<T>> results;

  private Consumer<Map<String, MessageEnvelope<T>>> resultsHandler;
  private Consumer<Throwable> errorHandler;
  private BiFunction<byte[], Instant, byte[]> decrypter;
  private Set<String> existingSignatures;
  private final ScheduledExecutorService executorService;
  private final AtomicLong lastMessageArrivalTime;
  private MessageListener<T> listener;
  private long noMoreMessagesTimeout;

  /**
   * Checks if the message matches preliminary search criteria.
   *
   * @param  message The message read from the topic.
   * @return         True if the message matches search criteria, false otherwise.
   */
  protected abstract boolean matchesSearchCriteria(T message);

  /**
   * Applies custom filters on the message and if successfully verified, adds it to the results map.
   *
   * @param envelope Message inside an envelope in PLAIN mode.
   */
  protected abstract void processMessage(final MessageEnvelope<T> envelope);

  /**
   * Supplies message listener for messages of specified type.
   *
   * @return The {@link MessageListener} instance.
   */
  protected abstract MessageListener<T> supplyMessageListener();

  /**
   * Instantiates a message resolver.
   *
   * @param topicId Consensus topic ID.
   */
  public MessageResolver(final ConsensusTopicId topicId) {
    this.topicId = topicId;
    this.results = new HashMap<>();
    this.executorService = Executors.newScheduledThreadPool(2);
    this.noMoreMessagesTimeout = DEFAULT_TIMEOUT;
    this.lastMessageArrivalTime = new AtomicLong(System.currentTimeMillis());
  }

  /**
   * Resolves queries defined in implementing classes against a mirror node.
   *
   * @param mirrorClient The mirror node client.
   */
  public void execute(final MirrorClient mirrorClient) {
    new Validator().checkValidationErrors("Resolver not executed: ", v -> validate(v));
    existingSignatures = new HashSet<>();

    listener = supplyMessageListener();

    listener.setStartTime(Instant.MIN)
        .setEndTime(Instant.now())
        .setIgnoreErrors(false)
        .onError(errorHandler)
        .onDecrypt(decrypter)
        .subscribe(mirrorClient, msg -> handleMessage(msg));

    lastMessageArrivalTime.set(System.currentTimeMillis());
    waitOrFinish();
  }

  /**
   * Handles incoming DID messages from DID Topic on a mirror node.
   *
   * @param envelope The parsed message envelope in a PLAIN mode.
   */
  private void handleMessage(final MessageEnvelope<T> envelope) {
    lastMessageArrivalTime.set(System.currentTimeMillis());

    // Skip messages that are not relevant for requested DID's
    if (!matchesSearchCriteria(envelope.open())) {
      return;
    }

    // Skip duplicated messages
    if (existingSignatures.contains(envelope.getSignature())) {
      return;
    }
    existingSignatures.add(envelope.getSignature());

    processMessage(envelope);
  }

  /**
   * Waits for a new message from the topic for the configured amount of time.
   */
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
   * Defines a handler for resolution results.
   * This will be called when the resolution process is finished.
   *
   * @param  handler The results handler.
   * @return         This resolver instance.
   */
  public MessageResolver<T> whenFinished(final Consumer<Map<String, MessageEnvelope<T>>> handler) {
    this.resultsHandler = handler;
    return this;
  }

  /**
   * Defines a handler for errors when they happen during resolution.
   *
   * @param  handler The error handler.
   * @return         This resolver instance.
   */
  public MessageResolver<T> onError(final Consumer<Throwable> handler) {
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
  public MessageResolver<T> setTimeout(final long timeout) {
    this.noMoreMessagesTimeout = timeout;
    return this;
  }

  /**
   * Defines decryption function that decrypts submitted the message after consensus was reached.
   * Decryption function must accept a byte array of encrypted message and an Instant that is its consensus timestamp,
   * If decrypter is not specified, encrypted messages will be ignored.
   *
   * @param  decrypter The decrypter to use.
   * @return           This resolver instance.
   */
  public MessageResolver<T> onDecrypt(final BiFunction<byte[], Instant, byte[]> decrypter) {
    this.decrypter = decrypter;
    return this;
  }

  /**
   * Runs validation logic of the resolver's configuration.
   *
   * @param validator The errors validator.
   */
  protected void validate(final Validator validator) {
    validator.require(!results.isEmpty(), "Nothing to resolve.");
    validator.require(topicId != null, "Consensus topic ID not defined.");
    validator.require(resultsHandler != null, "Results handler 'whenFinished' not defined.");
  }

}