package com.hedera.hashgraph.identity.hcs.example.appnet;

import com.github.jsonldjava.shaded.com.google.common.collect.Sets;
import com.hedera.hashgraph.identity.DidMethodOperation;
import com.hedera.hashgraph.identity.hcs.MessageEnvelope;
import com.hedera.hashgraph.identity.hcs.did.HcsDidMessage;
import com.hedera.hashgraph.identity.hcs.example.appnet.dto.VerifiableCredentialStatus;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcMessage;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcOperation;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PublicKey;

import java.io.*;
import java.time.Instant;
import java.util.*;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements appnet storage by persisting data to files.
 * Signatures and credential issuers are stored in two separate files, everytime a signature or issuer is added,
 * the complete set is persisted to file.
 * Additionally, DIDs and VCs are stored in their own files, along with the consensus timestamp of the last notification received
 * from mirror node. Persistence does not occur for every additional DiD or VC. Persistence occurs every n and m notifications
 * as determined by the .env file.
 * Upon restart, the persisted data is reloaded in memory and mirror subscriptions for the topic ids restarts from the last
 * persisted notification + 1 nano second.
 */
public class AppnetStorage {
  private final String credentialIssuersFile = "persistedCredentialIssuers.ser";
  private final String signaturesFile = "persistedSignatures.ser";
  private final String DiDsFile = "persistedDiDs.ser";
  private final String VCsFile = "persistedVCs.ser";;

  private static Logger log = LoggerFactory.getLogger(AppnetStorage.class);
  private Set<String> signatures;
  private Map<String, HcsDidMessage> didStorage;
  private Map<String, MessageEnvelope<HcsVcMessage>> vcStorage;
  private Map<String, Ed25519PublicKey> credentialIssuers;
  private int didCount = 0;
  private int vcCount = 0;
  private int didStoreInterval = 0;
  private int vcStoreInterval = 0;
  private Instant lastDiDConsensusTimeStamp = Instant.ofEpochMilli(0);
  private Instant lastVCConsensusTimeStamp = Instant.ofEpochMilli(0);

  /**
   * Initializes in-memory buffers for appnet's storage.
   */
  @SuppressWarnings({"unchecked"})
  public AppnetStorage() {
    // load persisted data if it exists
    if (new File(DiDsFile).exists()) {
      log.info("Loading DiDs from persistence");
      try {
        FileInputStream fis = new FileInputStream(DiDsFile);
        ObjectInputStream ois = new ObjectInputStream(fis);
        this.didStorage = (Map<String, HcsDidMessage>) ois.readObject();
        this.lastDiDConsensusTimeStamp = (Instant) ois.readObject();
        ois.close();
        fis.close();
      } catch (IOException ioe) {
        ioe.printStackTrace();
        System.exit(1);
      } catch (ClassNotFoundException c) {
        System.out.println("Class not found");
        c.printStackTrace();
        System.exit(1);
      }
    } else {
      this.didStorage = new HashMap<>();
    }

    if (new File(VCsFile).exists()) {
      log.info("Loading VCs from persistence");
      try {
        FileInputStream fis = new FileInputStream(VCsFile);
        ObjectInputStream ois = new ObjectInputStream(fis);
        this.vcStorage = (Map<String, MessageEnvelope<HcsVcMessage>>) ois.readObject();
        this.lastVCConsensusTimeStamp = (Instant) ois.readObject();
        ois.close();
        fis.close();
      } catch (IOException ioe) {
        ioe.printStackTrace();
        System.exit(1);
      } catch (ClassNotFoundException c) {
        System.out.println("Class not found");
        c.printStackTrace();
        System.exit(1);
      }
    } else {
      this.vcStorage = new HashMap<>();
    }

    if (new File(signaturesFile).exists()) {
      log.info("Loading Signatures from persistence");
      try {
        FileInputStream fis = new FileInputStream(signaturesFile);
        ObjectInputStream ois = new ObjectInputStream(fis);
        this.signatures = (HashSet) ois.readObject();
        ois.close();
        fis.close();
      } catch (IOException ioe) {
        ioe.printStackTrace();
        System.exit(1);
      } catch (ClassNotFoundException c) {
        System.out.println("Class not found");
        c.printStackTrace();
        System.exit(1);
      }
    } else {
      this.signatures = new HashSet<>();
    }

    this.credentialIssuers = new HashMap<>();
    if (new File(credentialIssuersFile).exists()) {
      log.info("Loading credential issuers from persistence");
      try {
        FileInputStream fis = new FileInputStream(credentialIssuersFile);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Map<String, String> persistedCredentialIssuers = (Map<String, String>) ois.readObject();
        ois.close();
        fis.close();

        // Ed25519public key is not serializable, perform the conversion here
        for (Map.Entry<String, String> entry : persistedCredentialIssuers.entrySet()) {
          this.credentialIssuers.put(entry.getKey(), Ed25519PublicKey.fromString(entry.getValue()));
        }
      } catch (IOException ioe) {
        ioe.printStackTrace();
        System.exit(1);
      } catch (ClassNotFoundException c) {
        System.out.println("Class not found");
        c.printStackTrace();
        System.exit(1);
      }
    }
    // Get Environment variables for persistence
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().load();

    // Grab the DID_PERSIST_COUNT and VC_PERSIST_COUNT from environment variables
    this.didStoreInterval = Integer.parseInt(dotenv.get("DID_PERSIST_INTERVAL", "10"));
    this.vcStoreInterval = Integer.parseInt(dotenv.get("VC_PERSIST_INTERVAL", "10"));

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
    persistCredentialIssuers();
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
  /**
   * Persists DiDs from memory into a file
   *
   * @param consensusTimeStamp  The consensusTimeStamp of the last message received from mirror node
   */
  private void persistDiDs(Instant consensusTimeStamp) {
    didCount += 1;
    if (didCount == didStoreInterval) {
      didCount = 0;
      try {
        FileOutputStream fos =
                new FileOutputStream(DiDsFile);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(didStorage);
        oos.writeObject(consensusTimeStamp);
        oos.close();
        fos.close();
        log.info("Serialized DiD HashMap data is saved in persistedDID.ser");
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
  }

  /**
   * Persists VCss from memory into a file
   *
   * @param consensusTimeStamp  The consensusTimeStamp of the last message received from mirror node
   */
  private void persistVCs(Instant consensusTimeStamp) {
    vcCount += 1;
    if (vcCount == vcStoreInterval) {
      vcCount = 0;
      try {
        FileOutputStream fos =
                new FileOutputStream(VCsFile);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(vcStorage);
        oos.writeObject(consensusTimeStamp);
        oos.close();
        fos.close();
        log.info("Serialized VC HashMap data is saved in persistedVC.ser");
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
  }

  /**
   * Persists signatures to a file
   */
  private void persistSignatures() {
    try {
      FileOutputStream fos =
              new FileOutputStream(signaturesFile);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(signatures);
      oos.close();
      fos.close();
      log.info("Serialized signatures data is saved in signatures.ser");
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  /**
   * Persists crediential issuers to a file
   */
  private void persistCredentialIssuers() {
    try {
      FileOutputStream fos =
              new FileOutputStream(credentialIssuersFile);
      ObjectOutputStream oos = new ObjectOutputStream(fos);

      Map<String, String> persistedCredentialIssuers = new HashMap<>();
      // Ed25519public key is not serializable, perform the conversion here
      for (Map.Entry<String, Ed25519PublicKey> entry : this.credentialIssuers.entrySet()) {
        persistedCredentialIssuers.put(entry.getKey(), entry.getValue().toString());
      }

      oos.writeObject(persistedCredentialIssuers);
      oos.close();
      fos.close();
      log.info("Serialized signatures data is saved in credentialIssuers.ser");
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  /**
   * Getter for lastDidConsensusTimestamp
   *
   * @return Instant the last consensus timestamp for DiD
   */
  public Instant getLastDiDConsensusTimeStamp() {
    return this.lastDiDConsensusTimeStamp;
  }
  /**
   * Getter for lastVCConsensusTimestamp
   *
   * @return Instant the last consensus timestamp for VC
   */
  public Instant getLastVCConsensusTimeStamp() {
    return this.lastVCConsensusTimeStamp;
  }
}
