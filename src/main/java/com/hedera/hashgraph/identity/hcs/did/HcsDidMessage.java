package com.hedera.hashgraph.identity.hcs.did;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.Expose;
import com.hedera.hashgraph.identity.DidDocumentBase;
import com.hedera.hashgraph.identity.DidDocumentJsonProperties;
import com.hedera.hashgraph.identity.DidMethodOperation;
import com.hedera.hashgraph.identity.hcs.Message;
import com.hedera.hashgraph.identity.hcs.MessageEnvelope;
import com.hedera.hashgraph.identity.utils.Iso8601InstantTypeAdapter;
import com.hedera.hashgraph.identity.utils.JsonUtils;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PublicKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import org.bitcoinj.core.Base58;

/**
 * The DID document message submitted to appnet's DID Topic.
 */
public class HcsDidMessage extends Message {
  @Expose(serialize = true, deserialize = true)
  protected DidMethodOperation operation;

  @Expose(serialize = true, deserialize = true)
  protected String did;

  @Expose(serialize = true, deserialize = true)
  protected String didDocumentBase64;

  /**
   * The date when the DID was created and published.
   * It is equal to consensus timestamp of the first creation message.
   * This property is set by the listener and injected into the DID document upon calling getDidDocument() method.
   */
  @Expose(serialize = false, deserialize = false)
  protected Instant created;

  /**
   * The date when the DID was updated and published.
   * It is equal to consensus timestamp of the last valid update or delete message.
   * This property is set by the listener and injected into the DID document upon calling getDidDocument() method.
   */
  @Expose(serialize = false, deserialize = false)
  protected Instant updated;

  /**
   * Creates a new instance of {@link HcsDidMessage}.
   *
   * @param operation         The operation on DID document.
   * @param did               The DID string.
   * @param didDocumentBase64 The Base64-encoded DID document.
   */
  protected HcsDidMessage(final DidMethodOperation operation, final String did, final String didDocumentBase64) {
    this.operation = operation;
    this.did = did;
    this.didDocumentBase64 = didDocumentBase64;
  }

  /**
   * Creates a new DID message for submission to HCS topic.
   *
   * @param  didDocumentJson DID document as JSON string.
   * @param  operation       The operation on DID document.
   * @return                 The HCS message wrapped in an envelope for the given DID document and method operation.
   */
  public static MessageEnvelope<HcsDidMessage> fromDidDocumentJson(final String didDocumentJson,
      final DidMethodOperation operation) {
    DidDocumentBase didDocumentBase = DidDocumentBase.fromJson(didDocumentJson);

    byte[] encodedDoc = Base64.getEncoder().encode(didDocumentJson.getBytes(StandardCharsets.UTF_8));
    String didDocumentBase64 = new String(encodedDoc, StandardCharsets.UTF_8);

    HcsDidMessage message = new HcsDidMessage(operation, didDocumentBase.getId(), didDocumentBase64);

    return new MessageEnvelope<>(message);
  }

  /**
   * Provides an encryption operator that converts an {@link HcsDidMessage} into encrypted one.
   *
   * @param  encryptionFunction The encryption function to use for encryption of single attributes.
   * @return                    The encryption operator instance.
   */
  public static UnaryOperator<HcsDidMessage> getEncrypter(final UnaryOperator<byte[]> encryptionFunction) {
    if (encryptionFunction == null) {
      throw new IllegalArgumentException("Encryption function is missing or null.");
    }

    return message -> {

      // Encrypt the DID
      byte[] encryptedDid = encryptionFunction.apply(message.getDid().getBytes(StandardCharsets.UTF_8));
      String did = new String(Base64.getEncoder().encode(encryptedDid), StandardCharsets.UTF_8);

      // Encrypt the DID document
      byte[] encryptedDoc = encryptionFunction.apply(message.getDidDocumentBase64().getBytes(StandardCharsets.UTF_8));
      String encryptedDocBase64 = new String(Base64.getEncoder().encode(encryptedDoc), StandardCharsets.UTF_8);

      return new HcsDidMessage(message.getOperation(), did, encryptedDocBase64);
    };
  }

  /**
   * Provides a decryption function that converts {@link HcsDidMessage} in encrypted for into a plain form.
   *
   * @param  decryptionFunction The decryption function to use for decryption of single attributes.
   * @return                    The Decryption function for the {@link HcsDidMessage}
   */
  public static BiFunction<HcsDidMessage, Instant, HcsDidMessage> getDecrypter(
      final BiFunction<byte[], Instant, byte[]> decryptionFunction) {
    if (decryptionFunction == null) {
      throw new IllegalArgumentException("Decryption function is missing or null.");
    }

    return (encryptedMsg, consensusTimestamp) -> {

      // Decrypt DID string
      String decryptedDid = encryptedMsg.getDid();
      if (decryptedDid != null) {
        byte[] didBytes = Base64.getDecoder().decode(decryptedDid.getBytes(StandardCharsets.UTF_8));
        didBytes = decryptionFunction.apply(didBytes, consensusTimestamp);
        decryptedDid = new String(didBytes, StandardCharsets.UTF_8);
      }

      // Decrypt DID document
      String decryptedDocBase64 = encryptedMsg.getDidDocumentBase64();
      if (decryptedDocBase64 != null) {
        byte[] docBytes = Base64.getDecoder().decode(decryptedDocBase64.getBytes(StandardCharsets.UTF_8));
        docBytes = decryptionFunction.apply(docBytes, consensusTimestamp);
        decryptedDocBase64 = new String(docBytes, StandardCharsets.UTF_8);
      }

      return new HcsDidMessage(encryptedMsg.getOperation(), decryptedDid, decryptedDocBase64);
    };
  }

  /**
   * Validates this DID message by checking its completeness, signature and DID document.
   *
   * @return True if the message is valid, false otherwise.
   */
  public boolean isValid() {
    return isValid(null);
  }

  /**
   * Validates this DID message by checking its completeness, signature and DID document.
   *
   * @param  didTopicId The DID topic ID against which the message is validated.
   * @return            True if the message is valid, false otherwise.
   */
  public boolean isValid(@Nullable final ConsensusTopicId didTopicId) {
    if (did == null || didDocumentBase64 == null) {
      return false;
    }

    try {
      DidDocumentBase doc = DidDocumentBase.fromJson(getDidDocument());

      // Validate if DID and DID document are present and match
      if (!did.equals(doc.getId())) {
        return false;
      }

      // Validate if DID root key is present in the document
      if (doc.getDidRootKey() == null || doc.getDidRootKey().getPublicKeyBase58() == null) {
        return false;
      }

      // Verify that DID was derived from this DID root key
      HcsDid hcsDid = HcsDid.fromString(did);

      // Extract public key from the DID document
      byte[] publicKeyBytes = Base58.decode(doc.getDidRootKey().getPublicKeyBase58());
      Ed25519PublicKey publicKey = Ed25519PublicKey.fromBytes(publicKeyBytes);

      if (!HcsDid.publicKeyToIdString(publicKey).equals(hcsDid.getIdString())) {
        return false;
      }

      // Verify that the message was sent to the right topic, if the DID contains the topic
      if (didTopicId != null && hcsDid.getDidTopicId() != null && !didTopicId.equals(hcsDid.getDidTopicId())) {
        return false;
      }
    } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
      return false;
    }

    return true;
  }

  /**
   * Extracts #did-root-key from the DID document.
   *
   * @return Public key of the DID subject.
   */
  public Ed25519PublicKey extractDidRootKey() {
    Ed25519PublicKey result = null;

    try {
      DidDocumentBase doc = DidDocumentBase.fromJson(getDidDocument());

      // Make sure that DID root key is present in the document
      if (doc.getDidRootKey() != null && doc.getDidRootKey().getPublicKeyBase58() != null) {
        byte[] publicKeyBytes = Base58.decode(doc.getDidRootKey().getPublicKeyBase58());
        result = Ed25519PublicKey.fromBytes(publicKeyBytes);
      }

      // ArrayIndexOutOfBoundsException is thrown in case public key is invalid in Ed25519PublicKey.fromBytes
    } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
      return null;
    }

    return result;
  }

  /**
   * Decodes didDocumentBase64 field and returns its content.
   * In case this message is in encrypted mode, it will return encrypted content,
   * so getPlainDidDocument method should be used instead.
   * If message consensus timestamps for creation and update are provided they will be injected into the result
   * document upon decoding.
   *
   * @return The decoded DID document as JSON string.
   */
  public String getDidDocument() {
    if (didDocumentBase64 == null) {
      return null;
    }

    byte[] decodedDoc = Base64.getDecoder().decode(didDocumentBase64.getBytes(StandardCharsets.UTF_8));
    String document = new String(decodedDoc, StandardCharsets.UTF_8);

    // inject timestamps
    if (created != null || updated != null) {
      JsonObject root = JsonParser.parseString(document).getAsJsonObject();
      TypeAdapter<Instant> adapter = Iso8601InstantTypeAdapter.getInstance();

      if (created != null) {
        root.add(DidDocumentJsonProperties.CREATED, adapter.toJsonTree(created));
      }

      if (updated != null) {
        root.add(DidDocumentJsonProperties.UPDATED, adapter.toJsonTree(updated));
      }

      document = JsonUtils.getGson().toJson(root);
    }

    return document;
  }

  public DidMethodOperation getOperation() {
    return operation;
  }

  public String getDid() {
    return did;
  }

  public String getDidDocumentBase64() {
    return didDocumentBase64;
  }

  public Instant getCreated() {
    return created;
  }

  public void setCreated(final Instant created) {
    this.created = created;
  }

  public Instant getUpdated() {
    return updated;
  }

  public void setUpdated(final Instant updated) {
    this.updated = updated;
  }
}
