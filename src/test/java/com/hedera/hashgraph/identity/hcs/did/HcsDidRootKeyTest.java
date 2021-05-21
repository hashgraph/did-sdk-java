package com.hedera.hashgraph.identity.hcs.did;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.FileId;
import io.github.cdimascio.dotenv.Dotenv;
import org.bitcoinj.core.Base58;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Objects;

/**
 * Tests {@link HcsDidRootKey} generation and parsing operations.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HcsDidRootKeyTest {
  private Dotenv dotenv = Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().load();
  // Grab the network to use from environment variables
  private String network = Objects.requireNonNull(dotenv.get("NETWORK"));

  @Test
  void testGenerate() {
    final String addressBook = "0.0.1";

    // Generate pair of HcsDid root keys
    PrivateKey privateKey = HcsDid.generateDidRootKey();

    // Generate HcsDid
    HcsDid did = new HcsDid(network, privateKey.getPublicKey(), FileId.fromString(addressBook));

    assertThrows(IllegalArgumentException.class, () -> HcsDidRootKey.fromHcsIdentity(null, null));
    assertThrows(IllegalArgumentException.class, () -> HcsDidRootKey.fromHcsIdentity(did, null));
    assertThrows(IllegalArgumentException.class, () -> HcsDidRootKey.fromHcsIdentity(null, privateKey.getPublicKey()));

    PublicKey differentPublicKey = HcsDid.generateDidRootKey().getPublicKey();
    assertThrows(IllegalArgumentException.class, () -> HcsDidRootKey.fromHcsIdentity(did, differentPublicKey));

    HcsDidRootKey didRootKey = HcsDidRootKey.fromHcsIdentity(did, privateKey.getPublicKey());
    assertNotNull(didRootKey);

    assertEquals(didRootKey.getType(), "Ed25519VerificationKey2018");
    assertEquals(didRootKey.getId(), did.toDid() + HcsDidRootKey.DID_ROOT_KEY_NAME);
    assertEquals(didRootKey.getController(), did.toDid());
    assertEquals(didRootKey.getPublicKeyBase58(), Base58.encode(privateKey.getPublicKey().toBytes()));
  }
}
