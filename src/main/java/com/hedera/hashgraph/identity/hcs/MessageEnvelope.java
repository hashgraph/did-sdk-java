package com.hedera.hashgraph.identity.hcs;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.hedera.hashgraph.identity.utils.JsonUtils;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PublicKey;
import com.hedera.hashgraph.sdk.mirror.MirrorConsensusTopicResponse;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.bouncycastle.math.ec.rfc8032.Ed25519;

/**
 * The envelope for Hedera identity messages sent to HCS DID or VC topics.
 */
public class MessageEnvelope<T extends Message> {
  private static final String MESSAGE_KEY = "message";
  private static final String SIGNATURE_KEY = "signature";

  @Expose(serialize = true, deserialize = true)
  protected MessageMode mode;

  @Expose(serialize = true, deserialize = true)
  @SerializedName(MESSAGE_KEY)
  protected T message;

  @Expose(serialize = true, deserialize = true)
  @SerializedName(SIGNATURE_KEY)
  protected String signature;

  @Expose(serialize = false, deserialize = false)
  protected String messageJson;

  @Expose(serialize = false, deserialize = false)
  protected T decryptedMessage;

  @Expose(serialize = false, deserialize = false)
  protected MirrorConsensusTopicResponse mirrorResponse;

  /**
   * Creates a new message envelope for the given message.
   *
   * @param message The message.
   */
  public MessageEnvelope(final T message) {
    if (message == null) {
      throw new IllegalArgumentException("Message cannot be null.");
    }

    this.message = message;
    this.mode = MessageMode.PLAIN;
  }

  /**
   * Creates an empty message envelope.
   */
  MessageEnvelope() {
    // This constructor is empty intentionally
  }

  /**
   * Signs this message envelope with the given signing function.
   *
   * @param  signer The signing function.
   * @return        This envelope signed and serialized to JSON, ready for submission to HCS topic.
   */
  public byte[] sign(final UnaryOperator<byte[]> signer) {
    if (signer == null) {
      throw new IllegalArgumentException("Signing function is not provided.");
    }

    if (!Strings.isNullOrEmpty(signature)) {
      throw new IllegalStateException("Message is already signed.");
    }

    byte[] msgBytes = message.toJson().getBytes(StandardCharsets.UTF_8);
    byte[] signatureBytes = signer.apply(msgBytes);
    signature = new String(Base64.getEncoder().encode(signatureBytes), StandardCharsets.UTF_8);

    return toJson().getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Converts this message envelope into a JSON string.
   *
   * @return The JSON string representing this message envelope.
   */
  public String toJson() {
    return JsonUtils.getGson().toJson(this);
  }

  /**
   * Encrypts the message in this envelope and returns its encrypted instance.
   *
   * @param  encrypter The function used to encrypt the message.
   * @return           This envelope instance.
   */
  public MessageEnvelope<T> encrypt(final UnaryOperator<T> encrypter) {
    if (encrypter == null) {
      throw new IllegalArgumentException("The encryption function is not provided.");
    }

    this.decryptedMessage = message;
    this.message = encrypter.apply(message);
    this.mode = MessageMode.ENCRYPTED;

    return this;
  }

  /**
   * Converts a message from a DID or VC topic response into object instance.
   *
   * @param  <U>          Type of the message inside envelope.
   * @param  response     Topic message as a response from mirror node.
   * @param  messageClass Class type of the message inside envelope.
   * @return              The {@link MessageEnvelope}.
   */
  public static <U extends Message> MessageEnvelope<U> fromMirrorResponse(
      final MirrorConsensusTopicResponse response, final Class<U> messageClass) {

    String msgJson = new String(response.message, StandardCharsets.UTF_8);

    MessageEnvelope<U> result = MessageEnvelope.fromJson(msgJson, messageClass);
    result.mirrorResponse = response;

    return result;
  }

  /**
   * Converts a VC topic message from a JSON string into object instance.
   *
   * @param  <U>          Type of the message inside envelope.
   * @param  json         VC topic message as JSON string.
   * @param  messageClass Class of the message inside envelope.
   * @return              The {@link MessageEnvelope}.
   */
  public static <U extends Message> MessageEnvelope<U> fromJson(final String json, final Class<U> messageClass) {
    Gson gson = JsonUtils.getGson();
    Type envelopeType = TypeToken.getParameterized(MessageEnvelope.class, messageClass).getType();

    MessageEnvelope<U> result = gson.fromJson(json, envelopeType);

    // extract original message JSON part separately to be able to verify signature.
    JsonObject root = JsonParser.parseString(json).getAsJsonObject();
    result.messageJson = root.has(MESSAGE_KEY) ? root.get(MESSAGE_KEY).toString() : null;

    return result;
  }

  /**
   * Verifies the signature of the envelope against the public key of it's signer.
   *
   * @param  publicKeyProvider Provider of a public key of this envelope signer.
   * @return                   True if the message is valid, false otherwise.
   */
  public boolean isSignatureValid(final Function<MessageEnvelope<T>, Ed25519PublicKey> publicKeyProvider) {
    if (signature == null || messageJson == null) {
      return false;
    }

    Ed25519PublicKey publicKey = publicKeyProvider.apply(this);
    if (publicKey == null) {
      return false;
    }

    byte[] signatureToVerify = Base64.getDecoder().decode(signature.getBytes(StandardCharsets.UTF_8));
    byte[] messageBytes = messageJson.getBytes(StandardCharsets.UTF_8);

    return Ed25519.verify(signatureToVerify, 0, publicKey.toBytes(), 0, messageBytes, 0, messageBytes.length);
  }

  /**
   * Opens a plain message in this envelope.
   * If the message is encrypted, this will throw {@link IllegalArgumentException}.
   *
   * @return The message object in a plain mode.
   */
  public T open() {
    return open(null);
  }

  /**
   * Opens a message in this envelope.
   * If the message is encrypted, the given decrypter will be used first to decrypt it.
   * If the message is not encrypted, it will be immediately returned.
   *
   * @param  decrypter The function used to decrypt the message.
   * @return           The message object in a plain mode.
   */
  public T open(final BiFunction<T, Instant, T> decrypter) {
    if (decryptedMessage != null) {
      return decryptedMessage;
    }

    if (!MessageMode.ENCRYPTED.equals(mode)) {
      decryptedMessage = message;
    } else if (decrypter == null) {
      throw new IllegalArgumentException("The message is encrypted, provide decryption function.");
    } else if (decryptedMessage == null) {
      // Only decrypt once
      decryptedMessage = decrypter.apply(message, getConsensusTimestamp());
    }

    return decryptedMessage;
  }

  public String getSignature() {
    return signature;
  }

  public Instant getConsensusTimestamp() {
    return mirrorResponse == null ? null : mirrorResponse.consensusTimestamp;
  }

  public MessageMode getMode() {
    return mode;
  }

  public MirrorConsensusTopicResponse getMirrorResponse() {
    return mirrorResponse;
  }
}
