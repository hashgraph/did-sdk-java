package com.hedera.hashgraph.identity.hcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hedera.hashgraph.identity.DidDocumentBase;
import com.hedera.hashgraph.identity.DidDocumentJsonProperties;
import com.hedera.hashgraph.identity.DidSyntax;
import com.hedera.hashgraph.identity.HederaNetwork;
import com.hedera.hashgraph.identity.hcs.did.HcsDid;
import com.hedera.hashgraph.identity.hcs.did.HcsDidRootKey;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.file.FileId;
import org.bitcoinj.core.Base58;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Tests base DID document serialization and deserialization.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DidDocumentBaseTest {

  @Test
  public void testSerialization() {
    Ed25519PrivateKey privateKey = HcsDid.generateDidRootKey();
    HcsDid did = new HcsDid(HederaNetwork.TESTNET, privateKey.publicKey, FileId.fromString("0.0.1"));
    DidDocumentBase doc = did.generateDidDocument();

    String didJson = doc.toJson();

    // Check if JSON object has all mandatory properties.
    JsonObject root = JsonParser.parseString(didJson).getAsJsonObject();

    assertNotNull(root);
    assertTrue(root.has(DidDocumentJsonProperties.CONTEXT));
    assertTrue(root.has(DidDocumentJsonProperties.ID));
    assertTrue(root.has(DidDocumentJsonProperties.PUBLIC_KEY));
    assertTrue(root.has(DidDocumentJsonProperties.AUTHENTICATION));
    assertEquals(root.get(DidDocumentJsonProperties.CONTEXT).getAsString(), DidSyntax.DID_DOCUMENT_CONTEXT);
    assertEquals(root.get(DidDocumentJsonProperties.ID).getAsString(), did.toDid());

    JsonObject didRootKey = root.getAsJsonArray(DidDocumentJsonProperties.PUBLIC_KEY).get(0).getAsJsonObject();
    assertEquals(didRootKey.get("type").getAsString(), HcsDidRootKey.DID_ROOT_KEY_TYPE);
    assertEquals(didRootKey.get(DidDocumentJsonProperties.ID).getAsString(),
        did.toDid() + HcsDidRootKey.DID_ROOT_KEY_NAME);
    assertEquals(didRootKey.get("controller").getAsString(), did.toDid());
    assertEquals(didRootKey.get("publicKeyBase58").getAsString(), Base58.encode(privateKey.publicKey.toBytes()));
  }

  @Test
  public void testDeserialization() {
    Ed25519PrivateKey privateKey = HcsDid.generateDidRootKey();
    HcsDid did = new HcsDid(HederaNetwork.TESTNET, privateKey.publicKey, FileId.fromString("0.0.1"));
    DidDocumentBase doc = did.generateDidDocument();

    String didJson = doc.toJson();

    DidDocumentBase parsedDoc = DidDocumentBase.fromJson(didJson);
    assertEquals(parsedDoc.getId(), doc.getId());

    HcsDidRootKey didRootKey = parsedDoc.getDidRootKey();
    assertNotNull(didRootKey);
    assertEquals(didRootKey.getPublicKeyBase58(), doc.getDidRootKey().getPublicKeyBase58());
    assertEquals(didRootKey.getController(), doc.getDidRootKey().getController());
    assertEquals(didRootKey.getId(), doc.getDidRootKey().getId());
    assertEquals(didRootKey.getType(), doc.getDidRootKey().getType());
  }

  @Test
  public void testInvalidJsonDeserialization() {
    final String didJson = "{"
        + "  \"@context\": \"https://www.w3.org/ns/did/v1\","
        + "  \"id\": \"did:hedera:mainnet:7Prd74ry1Uct87nZqL3ny7aR7Cg46JamVbJgk8azVgUm;hedera:mainnet:fid=0.0.1\","
        + "  \"authentication\": ["
        + " \"did:hedera:mainnet:7Prd74ry1Uct87nZqL3ny7aR7Cg46JamVbJgk8azVgUm;hedera:mainnet:fid=0.0.1#did-root-key\""
        + "  ],"
        + "  \"publicKey\":\"invalidPublicKey\","
        + "  \"service\": ["
        + "    {"
        + "    \"id\":\"did:hedera:mainnet:7Prd74ry1Uct87nZqL3ny7aR7Cg46JamVbJgk8azVgUm;hedera:mainnet:fid=0.0.1#vcs\","
        + "    \"type\": \"VerifiableCredentialService\","
        + "    \"serviceEndpoint\": \"https://example.com/vc/\""
        + "    }"
        + "  ]"
        + "}";

    assertThrows(IllegalArgumentException.class, () -> DidDocumentBase.fromJson(didJson));
  }

  @Test
  public void testIncompleteJsonDeserialization() {
    final String didJsonMissingPublicKeys = "{"
        + "  \"@context\": \"https://www.w3.org/ns/did/v1\","
        + "  \"id\": \"did:hedera:mainnet:7Prd74ry1Uct87nZqL3ny7aR7Cg46JamVbJgk8azVgUm;hedera:mainnet:fid=0.0.1\","
        + "  \"authentication\": ["
        + " \"did:hedera:mainnet:7Prd74ry1Uct87nZqL3ny7aR7Cg46JamVbJgk8azVgUm;hedera:mainnet:fid=0.0.1#did-root-key\""
        + "  ]"
        + "}";

    final String didJsonMissingRootKey = "{"
        + "  \"@context\": \"https://www.w3.org/ns/did/v1\","
        + "  \"id\": \"did:hedera:mainnet:7Prd74ry1Uct87nZqL3ny7aR7Cg46JamVbJgk8azVgUm;hedera:mainnet:fid=0.0.1\","
        + "  \"authentication\": ["
        + "  \"did:hedera:mainnet:7Prd74ry1Uct87nZqL3ny7aR7Cg46JamVbJgk8azVgUm;hedera:mainnet:fid=0.0.1#did-root-key\""
        + "  ],"
        + "  \"publicKey\": ["
        + "    {"
        + " \"id\": \"did:hedera:mainnet:7Prd74ry1Uct87nZqL3ny7aR7Cg46JamVbJgk8azVgUm;hedera:mainnet:fid=0.0.1#key-1\","
        + " \"type\": \"Ed25519VerificationKey2018\","
        + "      \"publicKeyBase58\": \"H3C2AVvLMv6gmMNam3uVAjZpfkcJCwDwnZn6z3wXmqPV\""
        + "    }"
        + "  ],"
        + "  \"service\": ["
        + "    {"
        + " \"id\": \"did:hedera:mainnet:7Prd74ry1Uct87nZqL3ny7aR7Cg46JamVbJgk8azVgUm;hedera:mainnet:fid=0.0.1#vcs\","
        + "      \"type\": \"VerifiableCredentialService\","
        + "      \"serviceEndpoint\": \"https://example.com/vc/\""
        + "    }"
        + "  ]"
        + "}";

    final String didJsonMissingPublicKeyId = "{"
        + "  \"@context\": \"https://www.w3.org/ns/did/v1\","
        + "  \"id\": \"did:hedera:mainnet:7Prd74ry1Uct87nZqL3ny7aR7Cg46JamVbJgk8azVgUm;hedera:mainnet:fid=0.0.1\","
        + "  \"authentication\": ["
        + "  \"did:hedera:mainnet:7Prd74ry1Uct87nZqL3ny7aR7Cg46JamVbJgk8azVgUm;hedera:mainnet:fid=0.0.1#did-root-key\""
        + "  ],"
        + "  \"publicKey\": ["
        + "    {"
        + " \"type\": \"Ed25519VerificationKey2018\","
        + "      \"publicKeyBase58\": \"H3C2AVvLMv6gmMNam3uVAjZpfkcJCwDwnZn6z3wXmqPV\""
        + "    }"
        + "  ],"
        + "  \"service\": ["
        + "    {"
        + " \"id\": \"did:hedera:mainnet:7Prd74ry1Uct87nZqL3ny7aR7Cg46JamVbJgk8azVgUm;hedera:mainnet:fid=0.0.1#vcs\","
        + "      \"type\": \"VerifiableCredentialService\","
        + "      \"serviceEndpoint\": \"https://example.com/vc/\""
        + "    }"
        + "  ]"
        + "}";

    DidDocumentBase doc = DidDocumentBase.fromJson(didJsonMissingPublicKeys);
    assertNotNull(doc);
    assertNull(doc.getDidRootKey());

    doc = DidDocumentBase.fromJson(didJsonMissingRootKey);
    assertNotNull(doc);
    assertNull(doc.getDidRootKey());

    doc = DidDocumentBase.fromJson(didJsonMissingPublicKeyId);
    assertNotNull(doc);
    assertNull(doc.getDidRootKey());

  }
}
