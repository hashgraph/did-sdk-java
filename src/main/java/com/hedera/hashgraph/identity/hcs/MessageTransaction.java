package com.hedera.hashgraph.identity.hcs;

import com.google.common.base.Strings;
import com.hedera.hashgraph.identity.utils.Validator;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicMessageSubmitTransaction;
import com.hedera.hashgraph.sdk.Transaction;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransactionResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java8.util.function.BiFunction;
import org.threeten.bp.Instant;

public abstract class MessageTransaction<T extends Message> {

  protected final TopicId topicId;
  protected MessageEnvelope<T> message;
  private UnaryOperator<byte[]> encrypter;
  private BiFunction<byte[], Instant, byte[]> decrypter;
  private Function<TopicMessageSubmitTransaction, Transaction> buildTransactionFunction;
  private Consumer<MessageEnvelope<T>> receiver;
  private Consumer<Throwable> errorHandler;
  private boolean executed;
  private UnaryOperator<byte[]> signer;
  private MessageListener<T> listener;

  /**
   * Creates a new instance of a message transaction.
   *
   * @param topicId Consensus topic ID to which message will be submitted.
   */
  public MessageTransaction(final TopicId topicId) {
    this.topicId = topicId;
    this.executed = false;
  }

  /**
   * Creates a new instance of a message transaction with already prepared message.
   *
   * @param topicId Consensus topic ID to which message will be submitted.
   * @param message The message signed and ready to be sent.
   */
  public MessageTransaction(final TopicId topicId, final MessageEnvelope<T> message) {
    this.topicId = topicId;
    this.message = message;
    this.executed = false;
  }

  /**
   * Method that constructs a message envelope with a message of type T.
   *
   * @return The message envelope with a message inside ready to sign.
   */
  protected abstract MessageEnvelope<T> buildMessage();

  /**
   * Provides an instance of a message encrypter.
   *
   * @param encryptionFunction Encryption function used to encrypt single message property.
   * @return The message encrypter instance.
   */
  protected abstract UnaryOperator<T> provideMessageEncrypter(final UnaryOperator<byte[]> encryptionFunction);

  /**
   * Provides a {@link MessageListener} instance specific to the submitted message type.
   *
   * @param topicIdToListen ID of the HCS topic.
   * @return The topic listener for this message on a mirror node.
   */
  protected abstract MessageListener<T> provideTopicListener(final TopicId topicIdToListen);

  /**
   * Handles the error.
   * If external error handler is defined, passes the error there, otherwise raises RuntimeException.
   *
   * @param err The error.
   * @throws RuntimeException Runtime exception with the given error in case external error handler is not defined.
   */
  protected void handleError(final Throwable err) {
    // Ignore Status cancelled error. It happens on unsubscribe.
    //TODO: Check this
    //    if (err instanceof StatusRuntimeException
    //        && Code.CANCELLED.equals(((StatusRuntimeException) err).getStatus().getCode())) {
    //      return;
    //    }

    if (errorHandler != null) {
      errorHandler.accept(err);
    } else {
      throw new RuntimeException(err);
    }
  }

  /**
   * Defines encryption function that encrypts the message attributes before submission.
   *
   * @param encrypter The encrypter to use.
   * @return This transaction instance.
   */
  public MessageTransaction<T> onEncrypt(final UnaryOperator<byte[]> encrypter) {
    this.encrypter = encrypter;
    return this;
  }

  /**
   * Handles event from a mirror node when a message was consensus was reached and message received.
   *
   * @param receiver The receiver handling incoming message.
   * @return This transaction instance.
   */
  public MessageTransaction<T> onMessageConfirmed(final Consumer<MessageEnvelope<T>> receiver) {
    this.receiver = receiver;
    return this;
  }

  /**
   * Defines a handler for errors when they happen during execution.
   *
   * @param handler The error handler.
   * @return This transaction instance.
   */
  public MessageTransaction<T> onError(final Consumer<Throwable> handler) {
    this.errorHandler = handler;
    return this;
  }

  /**
   * Defines decryption function that decrypts message attributes after consensus is reached.
   * Decryption function must accept a byte array of encrypted message and an Instant that is its consensus timestamp,
   *
   * @param decrypter The decrypter to use.
   * @return This transaction instance.
   */
  public MessageTransaction<T> onDecrypt(final BiFunction<byte[], Instant, byte[]> decrypter) {
    this.decrypter = decrypter;
    return this;
  }

  /**
   * Defines a function that signs the message.
   *
   * @param signer The signing function to set.
   * @return This transaction instance.
   */
  public MessageTransaction<T> signMessage(final UnaryOperator<byte[]> signer) {
    this.signer = signer;
    return this;
  }

  /**
   * Sets {@link TopicMessageSubmitTransaction} parameters, builds and signs it without executing it.
   * Topic ID and transaction message content are already set in the incoming transaction.
   *
   * @param builderFunction The transaction builder function.
   * @return This transaction instance.
   */
  public MessageTransaction<T> buildAndSignTransaction(
          final Function<TopicMessageSubmitTransaction, Transaction> builderFunction) {
    this.buildTransactionFunction = builderFunction;
    return this;
  }

  /**
   * Builds the message and submits it to appnet's topic.
   *
   * @param client The hedera network client.
   * @return Transaction ID.
   */
  public TransactionId execute(final Client client) {
    new Validator().checkValidationErrors("MessageTransaction execution failed: ", v -> validate(v));

    MessageEnvelope<T> envelope = message == null ? buildMessage() : message;
    if (encrypter != null) {
      envelope.encrypt(provideMessageEncrypter(encrypter));
    }

    byte[] messageContent = envelope.getSignature() == null ? envelope.sign(signer)
            : envelope.toJson().getBytes(StandardCharsets.UTF_8);

    if (receiver != null) {
      listener = provideTopicListener(topicId);
      listener.setStartTime(Instant.now().minusSeconds(1))
              .setIgnoreErrors(false)
              .addFilter(r -> Arrays.equals(messageContent, r.contents))
              .onError(err -> handleError(err))
              .onInvalidMessageReceived((response, reason) -> {
                // Consider only the message submitted.
                if (!Arrays.equals(messageContent, response.contents)) {
                  return;
                }

                // Report error and stop listening
                handleError(new InvalidMessageException(response, reason));
                listener.unsubscribe();
              })
              .onDecrypt(decrypter)
              .subscribe(client, msg -> {
                listener.unsubscribe();
                receiver.accept(msg);
              });
    }

    TopicMessageSubmitTransaction tx = new TopicMessageSubmitTransaction()
            .setTopicId(topicId)
            .setMessage(messageContent);

    TransactionId transactionId = null;
    try {
      TransactionResponse response = (TransactionResponse) buildTransactionFunction
              .apply(tx)
              .execute(client);
      transactionId = response.transactionId;
      executed = true;
    } catch (PrecheckStatusException | TimeoutException e) {
      handleError(e);
      if (listener != null) {
        listener.unsubscribe();
      }
    }

    return transactionId;
  }

  /**
   * Runs validation logic.
   *
   * @param validator The errors validator.
   */
  protected void validate(final Validator validator) {
    validator.require(!executed, "This transaction has already been executed.");
    // signing function is only needed if signed message was not provided.
    validator.require(signer != null || (message != null && !Strings.isNullOrEmpty(message.getSignature())),
            "Signing function is missing.");
    validator.require(buildTransactionFunction != null, "Transaction builder is missing.");
    validator.require((encrypter != null && decrypter != null) || (encrypter == null && decrypter == null),
            "Either both encrypter and decrypter must be specified or none.");
  }

}
