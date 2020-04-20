package com.hedera.hashgraph.identity.hcs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.hedera.hashgraph.identity.DidDocumentOperation;
import com.hedera.hashgraph.identity.DidDocumentPublishingMode;
import com.hedera.hashgraph.sdk.mirror.MirrorConsensusTopicResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.function.BiFunction;
import javax.annotation.Nullable;

/**
 * The DID document message submitted to appnet's DID Topic.
 */
class HcsDidMessage {
  @Expose(serialize = true, deserialize = true)
  protected DidDocumentOperation didOperation;

  @Expose(serialize = true, deserialize = true)
  protected DidDocumentPublishingMode mode;

  @Expose(serialize = true, deserialize = true)
  protected String did;

  @Expose(serialize = true, deserialize = true)
  protected String didDocumentBase64;

  @Expose(serialize = true, deserialize = true)
  protected String signature;

  @Expose(serialize = false, deserialize = false)
  protected Instant consensusTimestamp;

  /**
   * Converts a DID message JSON string into object instance.
   *
   * @param  json DID message as JSON string.
   * @return      The {@link HcsDidMessage}.
   */
  protected static HcsDidMessage fromJson(final String json) {
    Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    return gson.fromJson(json, HcsDidMessage.class);
  }

  /**
   * Converts a DID message from a DID topic response into object instance.
   *
   * @param  response DID topic message as a response from mirror node.
   * @return          The {@link HcsDidMessage}.
   */
  public static HcsDidMessage fromDidTopicMirrorResponse(final MirrorConsensusTopicResponse response) {
    String message = new String(response.message, StandardCharsets.UTF_8);
    HcsDidMessage result = HcsDidMessage.fromJson(message);
    result.consensusTimestamp = response.consensusTimestamp;

    return result;
  }

  /**
   * Extracts the DID from this message.
   * If the message was encrypted it will use the specified decrypter otherwise will return plain DID.
   *
   * @param  decrypter                The decrypter.
   * @return                          The DID string.
   * @throws IllegalArgumentException In case the message was encrypted and no decrypter is specified.
   */
  public String getPlainDid(@Nullable final BiFunction<byte[], Instant, byte[]> decrypter) {
    if (did == null) {
      return null;
    }

    validateDecrypterPresence(decrypter);

    String result = did;
    if (DidDocumentPublishingMode.ENCRYPTED.equals(mode)) {
      byte[] encryptedDid = Base64.getDecoder().decode(did.getBytes(StandardCharsets.UTF_8));
      byte[] decryptedDid = decrypter.apply(encryptedDid, consensusTimestamp);
      result = new String(decryptedDid, StandardCharsets.UTF_8);
    }

    return result;
  }

  /**
   * Extracts the Base64-encoded DID document from this message.
   * If the message was encrypted it will use the specified decrypter otherwise will return plain DID.
   *
   * @param  decrypter                The decrypter.
   * @return                          The DID Document as Base64-encoded JSON string.
   * @throws IllegalArgumentException In case the message was encrypted and no decrypter is specified.
   */
  public String getPlainDidDocumentBase64(@Nullable final BiFunction<byte[], Instant, byte[]> decrypter) {
    if (didDocumentBase64 == null) {
      return null;
    }

    validateDecrypterPresence(decrypter);

    String result = didDocumentBase64;
    if (DidDocumentPublishingMode.ENCRYPTED.equals(mode)) {
      byte[] docBytes = Base64.getDecoder().decode(didDocumentBase64.getBytes(StandardCharsets.UTF_8));
      docBytes = decrypter.apply(docBytes, consensusTimestamp);
      result = new String(docBytes, StandardCharsets.UTF_8);
    }

    return result;
  }

  /**
   * Extracts the DID document from this message.
   * If the message was encrypted it will use the specified decrypter otherwise will return plain DID.
   *
   * @param  decrypter                The decrypter.
   * @return                          The DID Document as JSON string.
   * @throws IllegalArgumentException In case the message was encrypted and no decrypter is specified.
   */
  public String getPlainDidDocument(@Nullable final BiFunction<byte[], Instant, byte[]> decrypter) {
    String base64EncodedDoc = getPlainDidDocumentBase64(decrypter);
    byte[] decodedDoc = Base64.getDecoder().decode(base64EncodedDoc.getBytes(StandardCharsets.UTF_8));

    return new String(decodedDoc, StandardCharsets.UTF_8);
  }

  /**
   * Creates a copy of this message in a plain mode.
   *
   * @param  decrypter The decrypter.
   * @return           This {@link HcsDidPlainMessage} in a plain mode.
   */
  public HcsDidPlainMessage toPlainDidMessage(@Nullable final BiFunction<byte[], Instant, byte[]> decrypter) {
    return new HcsDidPlainMessage(didOperation, getPlainDid(decrypter), getPlainDidDocumentBase64(decrypter), signature,
        consensusTimestamp);
  }

  /**
   * Validates this DID message by checking its completeness, signature and DID document.
   *
   * @param  decrypter The decrypter to use in case this message was submitted encrypted.
   * @return           True if the message is valid, false otherwise.
   */
  public boolean isValid(@Nullable final BiFunction<byte[], Instant, byte[]> decrypter) {
    return toPlainDidMessage(decrypter).isValid();
  }

  /**
   * Checks if this message requires decrypter and it was specified.
   *
   * @param  decrypter                The decrypter instance.
   * @throws IllegalArgumentException In case the message was encrypted and no decrypter is specified.
   */
  private void validateDecrypterPresence(final BiFunction<byte[], Instant, byte[]> decrypter) {
    if (DidDocumentPublishingMode.ENCRYPTED.equals(mode) && decrypter == null) {
      throw new IllegalArgumentException("Decrypter is required to extract information from this message.");
    }
  }

  /**
   * Decodes didDocumentBase64 field and returns its content.
   * In case this message is in encrypted mode, it will return encrypted content,
   * so getPlainDidDocument method should be used instead.
   *
   * @return The decoded DID document as JSON string.
   */
  public String getDidDocument() {
    if (didDocumentBase64 == null) {
      return null;
    }

    byte[] decodedDoc = Base64.getDecoder().decode(didDocumentBase64.getBytes(StandardCharsets.UTF_8));
    return new String(decodedDoc, StandardCharsets.UTF_8);
  }

  public DidDocumentOperation getDidOperation() {
    return didOperation;
  }

  public DidDocumentPublishingMode getMode() {
    return mode;
  }

  public String getDid() {
    return did;
  }

  public String getDidDocumentBase64() {
    return didDocumentBase64;
  }

  public String getSignature() {
    return signature;
  }

  public Instant getConsensusTimestamp() {
    return consensusTimestamp;
  }
}
