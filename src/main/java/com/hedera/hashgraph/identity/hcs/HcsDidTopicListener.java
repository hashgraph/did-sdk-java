package com.hedera.hashgraph.identity.hcs;

import com.hedera.hashgraph.identity.DidDocumentPublishingMode;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.mirror.MirrorClient;
import com.hedera.hashgraph.sdk.mirror.MirrorConsensusTopicQuery;
import com.hedera.hashgraph.sdk.mirror.MirrorConsensusTopicResponse;
import com.hedera.hashgraph.sdk.mirror.MirrorSubscriptionHandle;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import java.time.Instant;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * A listener of confirmed {@link HcsDidMessage} messages from a DID topic.
 * Messages are received from a given mirror node, parsed and validated.
 */
public class HcsDidTopicListener {
  private final ConsensusTopicId didTopicId;
  private final MirrorConsensusTopicQuery query;
  private Consumer<Throwable> errorHandler;
  private boolean ignoreInvalidMessages;
  private boolean ignoreErrors;
  private BiFunction<byte[], Instant, byte[]> decrypter;
  private MirrorSubscriptionHandle subscriptionHandle;

  /**
   * Creates a new instance of a DID topic listener for the given consensus topic.
   * By default, invalid messages are ignored and errors are not.
   *
   * @param didTopicId The DID consensus topic ID.
   */
  public HcsDidTopicListener(final ConsensusTopicId didTopicId) {
    this.didTopicId = didTopicId;
    this.query = new MirrorConsensusTopicQuery().setTopicId(didTopicId);
    this.ignoreInvalidMessages = true;
    this.ignoreErrors = false;
  }

  /**
   * Subscribes to mirror node topic messages stream.
   *
   * @param mirrorClient Mirror client instance.
   * @param receiver     Receiver of parsed DID messages.
   */
  public void subscribe(final MirrorClient mirrorClient, final Consumer<HcsDidPlainMessage> receiver) {
    subscriptionHandle = query.subscribe(
        mirrorClient,
        resp -> handleResponse(resp, receiver),
        err -> handleError(err));
  }

  /**
   * Stops receiving messages from the DID topic.
   */
  public void unsubscribe() {
    if (subscriptionHandle != null) {
      subscriptionHandle.unsubscribe();
    }
  }

  /**
   * Handles incoming DID messages from DID Topic on a mirror node.
   *
   * @param resp     Response message coming from the mirror node for the DID topic.
   * @param receiver Consumer of the result message.
   */
  protected void handleResponse(final MirrorConsensusTopicResponse resp, final Consumer<HcsDidPlainMessage> receiver) {
    try {
      HcsDidMessage message = HcsDidMessage.fromDidTopicMirrorResponse(resp);

      // Skip encrypted messages if decrypter was not provided
      if (DidDocumentPublishingMode.ENCRYPTED.equals(message.getMode()) && decrypter == null) {
        return;
      }

      HcsDidPlainMessage plainMsg = message.toPlainDidMessage(decrypter);

      // Skip validate message if it was requested to skip invalid ones.
      // Otherwise message will not be validated.
      if (ignoreInvalidMessages && !plainMsg.isValid(didTopicId)) {
        return;
      }

      receiver.accept(plainMsg);
    } catch (Exception err) {
      handleError(err);
    }
  }

  /**
   * Handles the given error internally.
   * If external error handler is defined, passes the error there, otherwise raises RuntimeException or ignores it
   * depending on a ignoreErrors flag.
   *
   * @param  err     The error.
   * @throws Runtime exception with the given error in case external error handler is not defined
   *                 and errors were not requested to be ignored.
   */
  private void handleError(final Throwable err) {
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
   * Defines a handler for errors when they happen during execution.
   *
   * @param  handler The error handler.
   * @return         This transaction instance.
   */
  public HcsDidTopicListener onError(final Consumer<Throwable> handler) {
    this.errorHandler = handler;
    return this;
  }

  /**
   * Defines decryption function that decrypts submitted the DID and DID document after consensus is reached.
   * Decryption function must accept a byte array of encrypted message and an Instant that is its consensus timestamp,
   * If decrypter is not specified, encrypted messages will be ignored.
   *
   * @param  decrypter The decrypter to use.
   * @return           This transaction instance.
   */
  public HcsDidTopicListener onDecrypt(final BiFunction<byte[], Instant, byte[]> decrypter) {
    this.decrypter = decrypter;
    return this;
  }

  public HcsDidTopicListener setStartTime(final Instant startTime) {
    query.setStartTime(startTime);
    return this;
  }

  public HcsDidTopicListener setEndTime(final Instant endTime) {
    query.setEndTime(endTime);
    return this;
  }

  public HcsDidTopicListener setLimit(final long messagesLimit) {
    query.setLimit(messagesLimit);
    return this;
  }

  public HcsDidTopicListener setIgnoreInvalidMessages(final boolean ignoreInvalidMessages) {
    this.ignoreInvalidMessages = ignoreInvalidMessages;
    return this;
  }

  public HcsDidTopicListener setIgnoreErrors(final boolean ignoreErrors) {
    this.ignoreErrors = ignoreErrors;
    return this;
  }

}
