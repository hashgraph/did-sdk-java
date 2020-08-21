package com.hedera.hashgraph.identity.hcs.example.appnet;

import com.github.jsonldjava.shaded.com.google.common.collect.Sets;
import com.hedera.hashgraph.identity.DidMethodOperation;
import com.hedera.hashgraph.identity.hcs.MessageEnvelope;
import com.hedera.hashgraph.identity.hcs.did.HcsDidMessage;
import com.hedera.hashgraph.identity.hcs.example.appnet.dto.VerifiableCredentialStatus;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcMessage;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcOperation;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PublicKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class simulates appnet's storage, that would normally be persistent (e.g. a database or files).
 * This example application does not preserver any data.
 * It also ignores messages sent to HCS topics before its startup operating only on what is received during runtime.
 */
public class AppnetStorage {
  private static Logger log = LoggerFactory.getLogger(AppnetStorage.class);
  private final Set<String> signatures;
  private final Map<String, HcsDidMessage> didStorage;
  private final Map<String, MessageEnvelope<HcsVcMessage>> vcStorage;
  private final Map<String, Ed25519PublicKey> credentialIssuers;

  /**
   * Initializes in-memory buffers for appent's storage.
   */
  public AppnetStorage() {
    this.didStorage = new HashMap<>();
    this.vcStorage = new HashMap<>();
    this.signatures = new HashSet<>();
    this.credentialIssuers = new HashMap<>();
  }

  /**
   * Resolves a DID document against the given DID.
   *
   * @param  did The DID string to be resolved.
   * @return     The last valid DID message registered on hedera with DID document inside.
   */
  public HcsDidMessage resolveDid(final String did) {
    return didStorage.get(did);
  }

  /**
   * Registers the fact of credential issuance by the issuer.
   * It is used by the topic listener to verify signatures only against those registered issuers.
   *
   * @param credentialHash  The hash of issued credential.
   * @param issuerPublicKey Public key (#did-root-key) of the issuer.
   */
  public void registerCredentialIssuance(final String credentialHash, final Ed25519PublicKey issuerPublicKey) {
    credentialIssuers.put(credentialHash, issuerPublicKey);
  }

  /**
   * Returns a set of public keys that can be used to verify credential status signature.
   *
   * @param  credentialHash The hash of issued credential.
   * @return                A set of public keys that can be used to verify credential status signature.
   */
  public Set<Ed25519PublicKey> getAcceptableCredentialHashPublicKeys(final String credentialHash) {
    Ed25519PublicKey publicKey = credentialIssuers.get(credentialHash);
    return publicKey == null ? Sets.newHashSet() : Sets.newHashSet(publicKey);
  }

  /**
   * Stores a newly received DID message in the local storage.
   * Before storing it, validates if the message is not a duplicate and if it is valid.
   *
   * @param envelope The DID message in an envelope.
   */
  public void storeDid(final MessageEnvelope<HcsDidMessage> envelope) {
    if (envelope == null) {
      log.warn("Empty envelope received, message ignored.");
      return;
    }

    if (signatures.contains(envelope.getSignature())) {
      log.warn("Duplicate message signature detected, message ignored: " + envelope.getSignature());
      return;
    }

    signatures.add(envelope.getSignature());
    HcsDidMessage msg = envelope.open();

    HcsDidMessage existing = didStorage.get(msg.getDid());
    if (existing != null
        && (envelope.getConsensusTimestamp().isBefore(existing.getUpdated())
            || (DidMethodOperation.DELETE.equals(existing.getOperation())
                && !DidMethodOperation.DELETE.equals(msg.getOperation())))) {

      log.warn("Outdated DID message received, ignored.");
      return;
    }

    // Preserve created and updated timestamps
    msg.setUpdated(envelope.getConsensusTimestamp());
    if (DidMethodOperation.CREATE.equals(msg.getOperation())) {
      msg.setCreated(envelope.getConsensusTimestamp());
    } else if (existing != null) {
      msg.setCreated(existing.getCreated());
    }

    log.info("New DID message " + msg.getOperation() + " received for: " + msg.getDid());
    didStorage.put(msg.getDid(), msg);
  }

  /**
   * Resolves a status of a verifiable credential.
   * Verifiers can use it to check if credential was not revoked or suspended.
   *
   * @param  credentialHash The hash of issued credential.
   * @return                The last valid status message with credential status inside or NULL if the status was not
   *                        found (e.g. credential was never issued).
   */
  public VerifiableCredentialStatus resolveVcStatus(final String credentialHash) {
    MessageEnvelope<HcsVcMessage> envelope = vcStorage.get(credentialHash);

    return envelope == null ? null
        : VerifiableCredentialStatus.fromHcsVcMessage(envelope.open(), envelope.getConsensusTimestamp());
  }

  /**
   * Stores a newly received credential status change message in local storage.
   * Before storing it, validates if the message is not a duplicate and if it is valid.
   *
   * @param envelope The credential status change message in an envelope.
   */
  public void storeVcStatus(final MessageEnvelope<HcsVcMessage> envelope) {
    if (envelope == null) {
      log.warn("Empty envelope received, message ignored.");
      return;
    }

    if (signatures.contains(envelope.getSignature())) {
      log.warn("Duplicate message signature detected, message ignored: " + envelope.getSignature());
      return;
    }

    signatures.add(envelope.getSignature());
    HcsVcMessage msg = envelope.open();

    MessageEnvelope<HcsVcMessage> existing = vcStorage.get(msg.getCredentialHash());
    // Skip messages that are older than the once collected or if we already have a REVOKED message
    if (existing != null
        && (envelope.getConsensusTimestamp().isBefore(existing.getConsensusTimestamp())
            || (HcsVcOperation.REVOKE.equals(existing.open().getOperation())
                && !HcsVcOperation.REVOKE.equals(msg.getOperation())))) {
      log.warn("Outdated VC message received, ignored.");
      return;
    }

    log.info("New VC message " + msg.getOperation() + " received for: " + msg.getCredentialHash());
    vcStorage.put(msg.getCredentialHash(), envelope);
  }

  /**
   * Extracts a public key (#did-root-key) from a DID document for the given DID.
   *
   * @param  did The DID string.
   * @return     The public key used to build the given DID or NULL if DID could not be resolved.
   */
  public Set<Ed25519PublicKey> getDidRootKey(final String did) {
    Set<Ed25519PublicKey> result = new HashSet<>();

    HcsDidMessage msg = resolveDid(did);
    if (msg != null) {
      result.add(msg.extractDidRootKey());
    }

    return result;
  }
}
