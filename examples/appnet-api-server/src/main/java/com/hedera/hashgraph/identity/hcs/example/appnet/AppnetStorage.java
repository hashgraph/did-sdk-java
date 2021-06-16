package com.hedera.hashgraph.identity.hcs.example.appnet;

import com.github.jsonldjava.shaded.com.google.common.collect.Sets;
import com.hedera.hashgraph.identity.DidMethodOperation;
import com.hedera.hashgraph.identity.hcs.MessageEnvelope;
import com.hedera.hashgraph.identity.hcs.did.HcsDidMessage;
import com.hedera.hashgraph.identity.hcs.example.appnet.dto.VerifiableCredentialStatus;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcMessage;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcOperation;
import com.hedera.hashgraph.sdk.PublicKey;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Instant;

/**
 * This class implements appnet storage by persisting data to files.
 * Signatures and credential issuers are stored in two separate files, everytime a signature or issuer is added,
 * the complete set is persisted to file.
 * Additionally, DIDs and VCs are stored in their own files, along with the consensus timestamp of the last notification
 * received from mirror node. Persistence does not occur for every additional DiD or VC. Persistence occurs every
 * n and m notifications as determined by the .env file.
 * Upon restart, the persisted data is reloaded in memory and mirror subscriptions for the topic ids restarts
 * from the last persisted notification + 1 nano second.
 */
public class AppnetStorage extends AppnetStorageProperties {
  private static Logger log = LoggerFactory.getLogger(AppnetStorage.class);
  private Set<String> signatures;
  private Map<String, HcsDidMessage> didStorage;
  private Map<String, MessageEnvelope<HcsVcMessage>> vcStorage;
  private Map<String, PublicKey> credentialIssuers;

  private void loadDids() throws IOException, ClassNotFoundException {
    if (new File(DIDSFILEPATH).exists()) {
      log.info("Loading DiDs from persistence");
      try (InputStream fis = Files.newInputStream(Paths.get(DIDSFILEPATH))) {
        ObjectInputStream ois = new ObjectInputStream(fis);
        this.didStorage = (Map<String, HcsDidMessage>) ois.readObject();
        this.lastDiDConsensusTimeStamp = (Instant) ois.readObject();
        ois.close();
      }
    } else {
      this.didStorage = new HashMap<>();
    }
  }

  private void loadVcs() throws IOException, ClassNotFoundException {
    if (new File(VCSFILEPATH).exists()) {
      log.info("Loading VCs from persistence");
      try (InputStream fis = Files.newInputStream(Paths.get(VCSFILEPATH))) {
        ObjectInputStream ois = new ObjectInputStream(fis);
        this.vcStorage = (Map<String, MessageEnvelope<HcsVcMessage>>) ois.readObject();
        this.lastVCConsensusTimeStamp = (Instant) ois.readObject();
        ois.close();
      }
    } else {
      this.vcStorage = new HashMap<>();
    }
  }

  private void loadSignatures() throws IOException, ClassNotFoundException {
    if (new File(SIGNATURES_FILE_PATH).exists()) {
      log.info("Loading Signatures from persistence");
      try (InputStream fis = Files.newInputStream(Paths.get(SIGNATURES_FILE_PATH))) {
        ObjectInputStream ois = new ObjectInputStream(fis);
        this.signatures = (HashSet) ois.readObject();
        ois.close();
      }
    } else {
      this.signatures = new HashSet<>();
    }
  }

  private void loadCredentialIssuers() throws IOException, ClassNotFoundException {
    this.credentialIssuers = new HashMap<>();
    if (new File(CREDENTIAL_ISSUERS_FILE_PATH).exists()) {
      log.info("Loading credential issuers from persistence");
      try (InputStream fis = Files.newInputStream(Paths.get(CREDENTIAL_ISSUERS_FILE_PATH))) {
        ObjectInputStream ois = new ObjectInputStream(fis);
        Map<String, String> persistedCredentialIssuers = (Map<String, String>) ois.readObject();
        // Ed25519public key is not serializable, perform the conversion here
        for (Map.Entry<String, String> entry : persistedCredentialIssuers.entrySet()) {
          this.credentialIssuers.put(entry.getKey(), PublicKey.fromString(entry.getValue()));
        }
        ois.close();
      }
    }
  }

  /**
   * Initializes in-memory buffers for appnet's storage.
   */
  @SuppressWarnings({"unchecked"})
  public AppnetStorage() throws IOException, ClassNotFoundException {
    super();
    // load persisted data if it exists
    loadDids();
    loadVcs();
    loadSignatures();
    loadCredentialIssuers();
  }

  /**
   * Resolves a DID document against the given DID.
   *
   * @param did The DID string to be resolved.
   * @return The last valid DID message registered on hedera with DID document inside.
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
  public void registerCredentialIssuance(final String credentialHash, final PublicKey issuerPublicKey) {
    credentialIssuers.put(credentialHash, issuerPublicKey);
    persistCredentialIssuers();
  }

  /**
   * Returns a set of public keys that can be used to verify credential status signature.
   *
   * @param credentialHash The hash of issued credential.
   * @return A set of public keys that can be used to verify credential status signature.
   */
  public Set<PublicKey> getAcceptableCredentialHashPublicKeys(final String credentialHash) {
    PublicKey publicKey = credentialIssuers.get(credentialHash);
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
    persistSignatures();
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

    persistDiDs(envelope.getConsensusTimestamp());
  }

  /**
    * Resolves a status of a verifiable credential.
    * Verifiers can use it to check if credential was not revoked or suspended.
    *
    * @param credentialHash The hash of issued credential.
    * @return The last valid status message with credential status inside or NULL if the status
    *     was not found (e.g. credential was never issued).
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
    persistSignatures();
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
    persistVCs(msg.getTimestamp());
  }

  /**
   * Extracts a public key (#did-root-key) from a DID document for the given DID.
   *
   * @param did The DID string.
   * @return The public key used to build the given DID or NULL if DID could not be resolved.
   */
  public Set<PublicKey> getDidRootKey(final String did) {
    Set<PublicKey> result = new HashSet<>();

    HcsDidMessage msg = resolveDid(did);
    if (msg != null) {
      result.add(msg.extractDidRootKey());
    }

    return result;
  }

  /**
   * Persists DiDs from memory into a file.
   *
   * @param consensusTimeStamp The consensusTimeStamp of the last message received from mirror node
   */
  private void persistDiDs(final Instant consensusTimeStamp) {
    didCount += 1;
    if (didCount == didStoreInterval) {
      didCount = 0;
      try (OutputStream fos = Files.newOutputStream(Paths.get(DIDSFILEPATH), StandardOpenOption.CREATE)) {
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(didStorage);
        oos.writeObject(consensusTimeStamp);
        oos.close();
        log.info("Serialized DiD HashMap data is saved in persistedDID.ser");
      } catch (IOException ioe) {
        log.error(ioe.getMessage());
      }
    }
  }

  /**
   * Persists VCss from memory into a file.
   *
   * @param consensusTimeStamp The consensusTimeStamp of the last message received from mirror node
   */
  private void persistVCs(final Instant consensusTimeStamp) {
    vcCount += 1;
    if (vcCount == vcStoreInterval) {
      vcCount = 0;
      try (OutputStream fos = Files.newOutputStream(Paths.get(VCSFILEPATH), StandardOpenOption.CREATE)) {
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(vcStorage);
        oos.writeObject(consensusTimeStamp);
        oos.close();
        log.info("Serialized VC HashMap data is saved in persistedVC.ser");
      } catch (IOException ioe) {
        log.error(ioe.getMessage());
      }
    }
  }

  /**
   * Persists signatures to a file.
   */
  private void persistSignatures() {
    try (OutputStream fos = Files.newOutputStream(Paths.get(SIGNATURES_FILE_PATH), StandardOpenOption.CREATE)) {
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(signatures);
      oos.close();
      log.info("Serialized signatures data is saved in signatures.ser");
    } catch (IOException ioe) {
      log.error(ioe.getMessage());
    }
  }

  /**
   * Persists crediential issuers to a file.
   */
  private void persistCredentialIssuers() {
    try (OutputStream fos = Files.newOutputStream(Paths.get(CREDENTIAL_ISSUERS_FILE_PATH), StandardOpenOption.CREATE)) {
      ObjectOutputStream oos = new ObjectOutputStream(fos);

      Map<String, String> persistedCredentialIssuers = new HashMap<>();
      // Ed25519public key is not serializable, perform the conversion here
      for (Map.Entry<String, PublicKey> entry : this.credentialIssuers.entrySet()) {
        persistedCredentialIssuers.put(entry.getKey(), entry.getValue().toString());
      }

      oos.writeObject(persistedCredentialIssuers);
      oos.close();
      log.info("Serialized signatures data is saved in credentialIssuers.ser");
    } catch (IOException ioe) {
      log.error(ioe.getMessage());
    }
  }

  /**
   * Getter for lastDidConsensusTimestamp.
   *
   * @return Instant the last consensus timestamp for DiD
   */
  public Instant getLastDiDConsensusTimeStamp() {
    return this.lastDiDConsensusTimeStamp;
  }

  /**
   * Getter for lastVCConsensusTimestamp.
   *
   * @return Instant the last consensus timestamp for VC
   */
  public Instant getLastVCConsensusTimeStamp() {
    return this.lastVCConsensusTimeStamp;
  }
}
