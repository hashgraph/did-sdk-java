package com.hedera.hashgraph.identity.hcs.example.appnet;

import io.github.cdimascio.dotenv.Dotenv;
import org.threeten.bp.Instant;

/**
 * This class implements appnet storage properties for use by AppnetStorage.
 */
public abstract class AppnetStorageProperties {
  protected static final String PERSISTENCE_DIR = "data/";
  protected static final String CREDENTIAL_ISSUERS_FILE_PATH = PERSISTENCE_DIR + "persistedCredentialIssuers.ser";
  protected static final String SIGNATURES_FILE_PATH = PERSISTENCE_DIR + "persistedSignatures.ser";
  protected static final String DIDSFILEPATH = PERSISTENCE_DIR + "persistedDiDs.ser";
  protected static final String VCSFILEPATH = PERSISTENCE_DIR + "persistedVCs.ser";
  protected int didStoreInterval;
  protected int vcStoreInterval;
  protected int didCount;
  protected int vcCount;
  protected Instant lastDiDConsensusTimeStamp = Instant.ofEpochMilli(0);
  protected Instant lastVCConsensusTimeStamp = Instant.ofEpochMilli(0);

  /**
   * constructor for the abstract class.
   */
  protected AppnetStorageProperties() {
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().load();
    // Grab the DID_PERSIST_COUNT and VC_PERSIST_COUNT from environment variables
    this.didStoreInterval = Integer.parseInt(dotenv.get("DID_PERSIST_INTERVAL", "10"));
    this.vcStoreInterval = Integer.parseInt(dotenv.get("VC_PERSIST_INTERVAL", "10"));
  }
}
