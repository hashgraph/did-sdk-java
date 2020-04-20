package com.hedera.hashgraph.identity.hcs;

import com.hedera.hashgraph.identity.DidDocumentBase;
import com.hedera.hashgraph.identity.DidDocumentOperation;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PublicKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.bitcoinj.core.Base58;
import org.bouncycastle.math.ec.rfc8032.Ed25519;

/**
 * The DID document message in a plain (unencrypted) form.
 */
public class HcsDidPlainMessage {
  protected DidDocumentOperation didOperation;
  protected String did;
  protected String didDocumentBase64;
  protected String signature;
  protected Instant consensusTimestamp;

  /**
   * Creates a new instance of {@link HcsDidPlainMessage}.
   *
   * @param didOperation       The operation on DID document.
   * @param did                The DID string.
   * @param didDocumentBase64  The Base64-encoded DID document.
   * @param signature          The signature.
   * @param consensusTimestamp Consensus timestamp of the DID message.
   */
  protected HcsDidPlainMessage(final DidDocumentOperation didOperation, final String did,
      final String didDocumentBase64, final String signature, final Instant consensusTimestamp) {
    this.didOperation = didOperation;
    this.did = did;
    this.didDocumentBase64 = didDocumentBase64;
    this.signature = signature;
    this.consensusTimestamp = consensusTimestamp;
  }

  /**
   * Validates this DID message by checking its completeness, signature and DID document.
   *
   * @param  didTopicId The DID topic ID against which the message is validated.
   * @return            True if the message is valid, false otherwise.
   */
  public boolean isValid(final ConsensusTopicId didTopicId) {
    if (signature == null || did == null || didDocumentBase64 == null) {
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

      return verifySignature(publicKeyBytes);
      // ArrayIndexOutOfBoundsException is thrown in case public key is invalid in Ed25519PublicKey.fromBytes
    } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
      return false;
    }
  }

  /**
   * Validates this DID message by checking its completeness, signature and DID document without matching topic ID.
   *
   * @return True if the message is valid, false otherwise.
   */
  public boolean isValid() {
    return isValid(null);
  }

  /**
   * Verifies DID message signature.
   *
   * @param  publicKey Public key of DID root key.
   * @return           True if signature is valid and false otherwise.
   */
  private boolean verifySignature(final byte[] publicKey) {
    byte[] signatureToVerify = Base64.getDecoder().decode(signature.getBytes(StandardCharsets.UTF_8));
    byte[] docBase64Bytes = didDocumentBase64.getBytes(StandardCharsets.UTF_8);

    return Ed25519.verify(signatureToVerify, 0, publicKey, 0, docBase64Bytes, 0, docBase64Bytes.length);
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
