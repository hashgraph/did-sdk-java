package com.hedera.hashgraph.identity.hcs.did;

import com.google.gson.annotations.Expose;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PublicKey;
import org.bitcoinj.core.Base58;

/**
 * Represents a root key of HCS Identity DID.
 * That is a public key of type Ed25519VerificationKey2018 compatible with a single publicKey entry of a DID Document.
 */
public class HcsDidRootKey {
  public static final String DID_ROOT_KEY_NAME = "#did-root-key";
  public static final String DID_ROOT_KEY_TYPE = "Ed25519VerificationKey2018";

  @Expose(serialize = true, deserialize = true)
  private String id;

  @Expose(serialize = true, deserialize = true)
  private String type;

  @Expose(serialize = true, deserialize = true)
  private String controller;

  @Expose(serialize = true, deserialize = true)
  private String publicKeyBase58;

  /**
   * Creates a {@link HcsDidRootKey} object from the given {@link HcsDid} DID and it's root public key.
   *
   * @param  did        The {@link HcsDid} DID object.
   * @param  didRootKey The public key from which the DID was derived.
   * @return            The {@link HcsDidRootKey} object.
   */
  public static HcsDidRootKey fromHcsIdentity(final HcsDid did, final Ed25519PublicKey didRootKey) {
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

    HcsDidRootKey result = new HcsDidRootKey();
    result.controller = did.toDid();
    result.id = result.controller + DID_ROOT_KEY_NAME;
    result.publicKeyBase58 = Base58.encode(didRootKey.toBytes());
    result.type = DID_ROOT_KEY_TYPE;

    return result;
  }

  public String getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public String getController() {
    return controller;
  }

  public String getPublicKeyBase58() {
    return publicKeyBase58;
  }
}
