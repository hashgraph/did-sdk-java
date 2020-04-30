package com.hedera.hashgraph.identity.hcs;

import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.mirror.MirrorClient;
import com.hedera.hashgraph.sdk.mirror.MirrorConsensusTopicQuery;
import com.hedera.hashgraph.sdk.mirror.MirrorConsensusTopicResponse;
import com.hedera.hashgraph.sdk.mirror.MirrorSubscriptionHandle;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A listener of confirmed messages from a HCS identity topic.
 * Messages are received from a given mirror node, parsed and validated.
 */
public abstract class MessageListener<T extends Message> {
  protected final ConsensusTopicId topicId;
  protected final MirrorConsensusTopicQuery query;
  protected Consumer<Throwable> errorHandler;
  protected boolean ignoreErrors;
  protected BiFunction<byte[], Instant, byte[]> decrypter;
  protected MirrorSubscriptionHandle subscriptionHandle;
  protected List<Predicate<MirrorConsensusTopicResponse>> filters;
  protected BiConsumer<MirrorConsensusTopicResponse, String> invalidMessageHandler;

  /**
   * Extracts and parses the message inside the response object into the given type.
   *
   * @param  response Response message coming from the mirror node for for this listener's topic.
   * @return          The message inside an envelope.
   */
  protected abstract MessageEnvelope<T> extractMessage(final MirrorConsensusTopicResponse response);

  /**
   * Validates the message and its envelope signature.
   *
   * @param  message  The message inside an envelope.
   * @param  response Response message coming from the mirror node for for this listener's topic.
   * @return          True if the message is valid, False otherwise.
   */
  protected abstract boolean isMessageValid(final MessageEnvelope<T> message,
      final MirrorConsensusTopicResponse response);

  /**
   * Creates a new instance of a topic listener for the given consensus topic.
   * By default, invalid messages are ignored and errors are not.
   *
   * @param topicId The consensus topic ID.
   */
  public MessageListener(final ConsensusTopicId topicId) {
    this.topicId = topicId;
    this.query = new MirrorConsensusTopicQuery().setTopicId(topicId);
    this.ignoreErrors = false;

  }

  /**
   * Adds a custom filter for topic responses from a mirror node.
   * Messages that do not pass the test are skipped before any other checks are run.
   *
   * @param  filter The filter function.
   * @return        This listener instance.
   */
  public MessageListener<T> addFilter(final Predicate<MirrorConsensusTopicResponse> filter) {
    if (filters == null) {
      filters = new ArrayList<>();
    }

    filters.add(filter);
    return this;
  }

  /**
   * Subscribes to mirror node topic messages stream.
   *
   * @param  mirrorClient Mirror client instance.
   * @param  receiver     Receiver of parsed messages.
   * @return              This listener instance.
   */
  public MessageListener<T> subscribe(final MirrorClient mirrorClient, final Consumer<MessageEnvelope<T>> receiver) {
    subscriptionHandle = query.subscribe(
        mirrorClient,
        resp -> handleResponse(resp, receiver),
        err -> handleError(err));

    return this;
  }

  /**
   * Stops receiving messages from the topic.
   */
  public void unsubscribe() {
    if (subscriptionHandle != null) {
      subscriptionHandle.unsubscribe();
    }
  }

  /**
   * Handles incoming messages from the topic on a mirror node.
   *
   * @param response Response message coming from the mirror node for the topic.
   * @param receiver Consumer of the result message.
   */
  protected void handleResponse(final MirrorConsensusTopicResponse response,
      final Consumer<MessageEnvelope<T>> receiver) {

    // Run external filters first
    if (filters != null) {
      for (Predicate<MirrorConsensusTopicResponse> filter : filters) {
        if (!filter.test(response)) {
          reportInvalidMessage(response, "Message was rejected by external filter");
          return;
        }
      }
    }

    // Extract and parse message from the response.
    MessageEnvelope<T> envelope = extractMessage(response);

    // Skip encrypted messages if decrypter was not provided
    if (envelope == null) {
      reportInvalidMessage(response, "Extracting envelope from the mirror response failed");
      return;
    }

    if (MessageMode.ENCRYPTED.equals(envelope.getMode()) && decrypter == null) {
      reportInvalidMessage(response, "Message is encrypted and no decryption function was provided");
      return;
    }

    // Check if message inside the envelope is valid and only accept it if it is.
    if (isMessageValid(envelope, response)) {
      receiver.accept(envelope);
    }
  }

  /**
   * Handles the given error internally.
   * If external error handler is defined, passes the error there, otherwise raises RuntimeException or ignores it
   * depending on a ignoreErrors flag.
   *
   * @param  err              The error.
   * @throws RuntimeException Runtime exception with the given error in case external error handler is not defined
   *                          and errors were not requested to be ignored.
   */
  protected void handleError(final Throwable err) {
    // Ignore Status cancelled error. It happens on unsubscribe.
    if (err instanceof StatusRuntimeException
        && Code.CANCELLED.equals(((StatusRuntimeException) err).getStatus().getCode())) {
      return;
    }

    if (errorHandler != null) {
      errorHandler.accept(err);
    } else if (!ignoreErrors) {
      throw new RuntimeException(err);
    }
  }

  /**
   * Reports invalid message to the handler.
   *
   * @param response The mirror response.
   * @param reason   The reason why message validation failed.
   */
  protected void reportInvalidMessage(final MirrorConsensusTopicResponse response, final String reason) {
    if (invalidMessageHandler != null) {
      invalidMessageHandler.accept(response, reason);
    }
  }

  /**
   * Defines a handler for errors when they happen during execution.
   *
   * @param  handler The error handler.
   * @return         This transaction instance.
   */
  public MessageListener<T> onError(final Consumer<Throwable> handler) {
    this.errorHandler = handler;
    return this;
  }

  /**
   * Defines a handler for invalid messages received from the topic.
   * The first parameter of the handler is the mirror response.
   * The second parameter is the reason why the message failed validation (if available).
   *
   * @param  handler The invalid message handler.
   * @return         This transaction instance.
   */
  public MessageListener<T> onInvalidMessageReceived(final BiConsumer<MirrorConsensusTopicResponse, String> handler) {
    this.invalidMessageHandler = handler;
    return this;
  }

  /**
   * Defines decryption function that decrypts submitted message attributes after consensus is reached.
   * Decryption function must accept a byte array of encrypted message and an Instant that is its consensus timestamp,
   * If decrypter is not specified, encrypted messages will be ignored.
   *
   * @param  decrypter The decryption function to use.
   * @return           This transaction instance.
   */
  public MessageListener<T> onDecrypt(final BiFunction<byte[], Instant, byte[]> decrypter) {
    this.decrypter = decrypter;

    return this;
  }

  public MessageListener<T> setStartTime(final Instant startTime) {
    query.setStartTime(startTime);
    return this;
  }

  public MessageListener<T> setEndTime(final Instant endTime) {
    query.setEndTime(endTime);
    return this;
  }

  public MessageListener<T> setLimit(final long messagesLimit) {
    query.setLimit(messagesLimit);
    return this;
  }

  public MessageListener<T> setIgnoreErrors(final boolean ignoreErrors) {
    this.ignoreErrors = ignoreErrors;
    return this;
  }

}
