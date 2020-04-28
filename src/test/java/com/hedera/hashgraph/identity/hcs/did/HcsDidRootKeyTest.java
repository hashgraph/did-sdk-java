package com.hedera.hashgraph.identity.hcs.did;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hedera.hashgraph.identity.HederaNetwork;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PublicKey;
import com.hedera.hashgraph.sdk.file.FileId;
import org.bitcoinj.core.Base58;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Tests {@link HcsDidRootKey} generation and parsing operations.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HcsDidRootKeyTest {

  @Test
  void testGenerate() {
    final String addressBook = "0.0.1";

    // Generate pair of HcsDid root keys
    Ed25519PrivateKey privateKey = HcsDid.generateDidRootKey();

    // Generate HcsDid
    HcsDid did =  new HcsDid(HederaNetwork.TESTNET, privateKey.publicKey, FileId.fromString(addressBook));

    assertThrows(IllegalArgumentException.class, () -> HcsDidRootKey.fromHcsIdentity(null, null));
    assertThrows(IllegalArgumentException.class, () -> HcsDidRootKey.fromHcsIdentity(did, null));
    assertThrows(IllegalArgumentException.class, () -> HcsDidRootKey.fromHcsIdentity(null, privateKey.publicKey));

    Ed25519PublicKey differentPublicKey = HcsDid.generateDidRootKey().publicKey;
    assertThrows(IllegalArgumentException.class, () -> HcsDidRootKey.fromHcsIdentity(did, differentPublicKey));

    HcsDidRootKey didRootKey = HcsDidRootKey.fromHcsIdentity(did, privateKey.publicKey);
    assertNotNull(didRootKey);

    assertEquals(didRootKey.getType(), "Ed25519VerificationKey2018");
    assertEquals(didRootKey.getId(), did.toDid() + HcsDidRootKey.DID_ROOT_KEY_NAME);
    assertEquals(didRootKey.getController(), did.toDid());
    assertEquals(didRootKey.getPublicKeyBase58(), Base58.encode(privateKey.publicKey.toBytes()));
  }
}
