package com.hedera.hashgraph.identity.hcs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hedera.hashgraph.identity.DidDocumentBase;
import com.hedera.hashgraph.identity.DidDocumentOperation;
import com.hedera.hashgraph.identity.DidDocumentPublishingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.UnaryOperator;

/**
 * The DID document message builder.
 */
final class HcsDidMessageBuilder {
  private final HcsDidMessage message;

  /**
   * Creates a new builder instance using a given DID document JSON string.
   *
   * @param  json DID document as JSON string.
   * @return      The {@link HcsDidMessageBuilder}.
   */
  private HcsDidMessageBuilder(final String didDocumentJson) {
    message = new HcsDidMessage();
    DidDocumentBase didDocumentBase = DidDocumentBase.fromJson(didDocumentJson);

    message.did = didDocumentBase.getId();

    byte[] encodedDoc = Base64.getEncoder().encode(didDocumentJson.getBytes(StandardCharsets.UTF_8));
    message.didDocumentBase64 = new String(encodedDoc, StandardCharsets.UTF_8);
  }

  /**
   * Sets DID document operation.
   *
   * @return This builder instance.
   */
  public HcsDidMessageBuilder setOperation(final DidDocumentOperation operation) {
    message.didOperation = operation;
    return this;
  }

  /**
   * Converts this DID document operation message into a plain JSON byte array ready to be posted to DID topic.
   *
   * @param  signer Function that signs DID document with private DID root key.
   * @return        The JSON representation of this message as byte array.
   */
  public byte[] buildAndSign(final UnaryOperator<byte[]> signer) {
    return this.buildAndSign(null, signer);
  }

  /**
   * Converts this DID document operation message to JSON and encrypts DID and DID document with the given encrypter.
   * Ready to be posted to DID topic.
   *
   * @param  encrypter The encrypter to use.
   * @param  signer    Function that signs DID document with private DID root key.
   * @return           The JSON representation of this message as byte array.
   */
  public byte[] buildAndSign(final UnaryOperator<byte[]> encrypter, final UnaryOperator<byte[]> signer) {
    if (message.didOperation == null) {
      throw new IllegalArgumentException("DID Document operation is not defined.");
    }

    if (signer == null) {
      throw new IllegalArgumentException("Signing function is not provided.");
    }

    byte[] docBytes = message.didDocumentBase64.getBytes(StandardCharsets.UTF_8);
    byte[] signature = signer.apply(docBytes);
    message.signature = new String(Base64.getEncoder().encode(signature), StandardCharsets.UTF_8);

    // If encrypter is not specified the message will be built plain.
    if (encrypter == null) {
      message.mode = DidDocumentPublishingMode.PLAIN;
    } else {
      // Encrypt DID and DID document
      message.mode = DidDocumentPublishingMode.ENCRYPTED;
      message.didDocumentBase64 = new String(Base64.getEncoder().encode(encrypter.apply(docBytes)),
          StandardCharsets.UTF_8);

      byte[] encryptedDid = encrypter.apply(message.did.getBytes(StandardCharsets.UTF_8));
      message.did = new String(Base64.getEncoder().encode(encryptedDid), StandardCharsets.UTF_8);
    }

    Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().create();
    return gson.toJson(message).getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Creates a new message instance from the given DID document.
   *
   * @param  json DID document as JSON string.
   * @return      The {@link HcsDidMessageBuilder}.
   */
  public static HcsDidMessageBuilder fromDidDocument(final String didDocumentJson) {
    return new HcsDidMessageBuilder(didDocumentJson);
  }
}
