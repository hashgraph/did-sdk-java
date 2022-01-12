package com.hedera.hashgraph.identity.hcs.did;

import com.hedera.hashgraph.sdk.PublicKey;
import org.bitcoinj.core.Base58;

/**
 * Represents a root key of HCS Identity DID.
 * That is a public key of type Ed25519VerificationKey2018 compatible with a single publicKey entry of a DID Document.
 */
public final class HcsDidRootKey extends HcsDidRootKeyBase {
  public static final String DID_ROOT_KEY_NAME = "#did-root-key";
  public static final String DID_ROOT_KEY_TYPE = "Ed25519VerificationKey2018";

  private HcsDidRootKey(final String id, final String type, final String controller, final String publicKeyBase58) {
    super(id, type, controller, publicKeyBase58);
  }

  /**
   * Creates a {@link HcsDidRootKey} object from the given {@link HcsDid} DID and it's root public key.
   *
   * @param did        The {@link HcsDid} DID object.
   * @param didRootKey The public key from which the DID was derived.
   * @return The {@link HcsDidRootKey} object.
   */
  public static HcsDidRootKey fromHcsIdentity(final HcsDid did, final PublicKey didRootKey) {
    if (did == null) {
      throw new IllegalArgumentException("DID cannot be null");
    }

    if (didRootKey == null) {
      throw new IllegalArgumentException("DID root key cannot be null");
    }

    // Validate if hcsIdentity is derived from the given root key
    if (!HcsDid.publicKeyToIdString(didRootKey).equals(did.getIdString())) {
      throw new IllegalArgumentException("The specified DID does not correspond to the given DID root key");
    }

    String controller = did.toDid();
    String id = controller + DID_ROOT_KEY_NAME;
    String publicKeyBase58 = Base58.encode(didRootKey.toBytes());

    return new HcsDidRootKey(
            id, DID_ROOT_KEY_TYPE, controller, publicKeyBase58
    );
  }
}
