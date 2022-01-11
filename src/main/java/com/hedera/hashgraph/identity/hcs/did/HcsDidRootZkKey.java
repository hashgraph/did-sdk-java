package com.hedera.hashgraph.identity.hcs.did;

import org.bitcoinj.core.Base58;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;

public final class HcsDidRootZkKey extends HcsDidRootKeyBase {
    public static final String DID_ROOT_KEY_NAME = "#did-root-key-zero-knowledge";
    public static final String DID_ROOT_KEY_TYPE = "Tweedle";

    private HcsDidRootZkKey(final String id, final String type, final String controller, final String publicKeyBase58) {
        super(id, type, controller, publicKeyBase58);
    }

    /**
     * Creates a {@link HcsDidRootKey} object from the given {@link HcsDid} DID and it's root public key.
     *
     * @param did        The {@link HcsDid} DID object.
     * @param didRootKey The public key byte array from which the DID was derived.
     * @return The {@link HcsDidRootZkKey} object.
     */
    public static HcsDidRootZkKey fromHcsIdentity(final HcsDid did, final String didRootKey) {
        if (did == null) {
            throw new IllegalArgumentException("DID cannot be null");
        }

        if (didRootKey == null) {
            throw new IllegalArgumentException("DID root key cannot be null");
        }

        // Validate if hcsIdentity is derived from the given root key
        if (!HcsDid.publicKeyToIdString(didRootKey).equals(did.getIdStringZk())) {
            throw new IllegalArgumentException("The specified DID does not correspond to the given DID root key");
        }

        String controller = did.toDid();
        String id = controller + DID_ROOT_KEY_NAME;
        String publicKeyBase58 = Base58.encode(ByteUtils.fromHexString(didRootKey));

        return new HcsDidRootZkKey(
                id, DID_ROOT_KEY_TYPE, controller, publicKeyBase58
        );
    }
}
