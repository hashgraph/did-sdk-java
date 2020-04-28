package com.hedera.hashgraph.identity.hcs.vc;

import com.google.common.base.Strings;
import com.google.gson.annotations.Expose;
import com.hedera.hashgraph.identity.hcs.Message;
import com.hedera.hashgraph.identity.hcs.MessageEnvelope;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

/**
 * The Verifiable Credential message.
 */
public class HcsVcMessage extends Message {
  @Expose(serialize = true, deserialize = true)
  private final HcsVcOperation operation;

  @Expose(serialize = true, deserialize = true)
  private final String credentialHash;

  /**
   * Provides an encryption operator that converts an {@link HcsVcMessage} into encrypted one.
   *
   * @param  encryptionFunction The encryption function to use for encryption of single attributes.
   * @return                    The encryption operator instance.
   */
  public static UnaryOperator<HcsVcMessage> getEncrypter(final UnaryOperator<byte[]> encryptionFunction) {
    if (encryptionFunction == null) {
      throw new IllegalArgumentException("Encryption function is missing or null.");
    }

    return message -> {

      // Encrypt the credential hash
      byte[] encryptedHash = encryptionFunction.apply(message.getCredentialHash().getBytes(StandardCharsets.UTF_8));
      String hash = new String(Base64.getEncoder().encode(encryptedHash), StandardCharsets.UTF_8);

      return new HcsVcMessage(message.getOperation(), hash);
    };
  }

  /**
   * Provides a decryption function that converts {@link HcsVcMessage} in encrypted for into a plain form.
   *
   * @param  decryptionFunction The decryption function to use for decryption of single attributes.
   * @return                    The decryption function for the {@link HcsVcMessage}
   */
  public static BiFunction<HcsVcMessage, Instant, HcsVcMessage> getDecrypter(
      final BiFunction<byte[], Instant, byte[]> decryptionFunction) {
    if (decryptionFunction == null) {
      throw new IllegalArgumentException("Decryption function is missing or null.");
    }

    return (encryptedMsg, consensusTimestamp) -> {

      // Decrypt DID string
      String decryptedHash = encryptedMsg.getCredentialHash();
      if (decryptedHash != null) {
        byte[] hashBytes = Base64.getDecoder().decode(decryptedHash.getBytes(StandardCharsets.UTF_8));
        hashBytes = decryptionFunction.apply(hashBytes, consensusTimestamp);
        decryptedHash = new String(hashBytes, StandardCharsets.UTF_8);
      }

      return new HcsVcMessage(encryptedMsg.getOperation(), decryptedHash);
    };
  }

  /**
   * Creates a new message instance.
   *
   * @param operation      Operation type.
   * @param credentialHash Credential hash.
   */
  protected HcsVcMessage(final HcsVcOperation operation, final String credentialHash) {
    super();
    this.operation = operation;
    this.credentialHash = credentialHash;
  }

  /**
   * Creates a new VC message for submission to HCS topic.
   *
   * @param  credentialHash VC hash.
   * @param  operation      The operation on a VC document.
   * @return                The HCS message wrapped in an envelope for the given VC and operation.
   */
  public static MessageEnvelope<HcsVcMessage> fromCredentialHash(final String credentialHash,
      final HcsVcOperation operation) {
    HcsVcMessage message = new HcsVcMessage(operation, credentialHash);
    return new MessageEnvelope<>(message);
  }

  /**
   * Checks if the message is valid from content point of view.
   * Does not verify hash nor any signatures.
   *
   * @return True if the message is valid and False otherwise.
   */
  public boolean isValid() {
    return !Strings.isNullOrEmpty(credentialHash) && operation != null;
  }

  public HcsVcOperation getOperation() {
    return operation;
  }

  public String getCredentialHash() {
    return credentialHash;
  }
}
