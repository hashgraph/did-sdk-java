package com.hedera.hashgraph.identity.hcs.did;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hedera.hashgraph.identity.DidDocumentBase;
import com.hedera.hashgraph.identity.DidSyntax;
import com.hedera.hashgraph.identity.DidSyntax.Method;
import com.hedera.hashgraph.identity.HederaDid;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PublicKey;
import com.hedera.hashgraph.sdk.file.FileId;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

import io.github.cdimascio.dotenv.Dotenv;
import org.bitcoinj.core.Base58;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Tests HcsDid generation and parsing operations.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HcsDidTest {
  private Dotenv dotenv = Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().load();
  // Grab the network to use from environment variables
  private String network = Objects.requireNonNull(dotenv.get("NETWORK"));

  @Test
  void testGenerateAndParseDidWithoutTid() throws NoSuchAlgorithmException {
    final String addressBook = "0.0.24352";

    // Generate pair of HcsDid root keys
    Ed25519PrivateKey privKey = HcsDid.generateDidRootKey();
    Ed25519PublicKey pubKey = privKey.publicKey;

    // Generate HcsDid
    HcsDid did = new HcsDid(network, pubKey, FileId.fromString(addressBook));

    // Convert HcsDid to HcsDid string
    String didString = did.toString();

    assertNotNull(didString);

    // Parse HcsDid string back to HcsDid object.
    HcsDid parsedDid = HcsDid.fromString(didString);

    assertNotNull(parsedDid);
    assertNotNull(parsedDid.getAddressBookFileId());

    assertNull(parsedDid.getDidTopicId());

    assertEquals(parsedDid.toString(), didString);
    assertEquals(parsedDid.getMethod(), Method.HEDERA_HCS);
    assertEquals(parsedDid.getNetwork(), network);
    assertEquals(parsedDid.getAddressBookFileId().toString(), addressBook);
    assertEquals(parsedDid.getIdString(), did.getIdString());
  }

  @Test
  void testGenerateAndParseDidWithTid() throws NoSuchAlgorithmException {
    final String addressBook = "0.0.24352";
    final String didTopicId = "1.5.23462345";

    // Generate pair of HcsDid root keys
    Ed25519PrivateKey privateKey = HcsDid.generateDidRootKey(SecureRandom.getInstanceStrong());

    // Generate HcsDid
    FileId fileId = FileId.fromString(addressBook);
    ConsensusTopicId topicId = ConsensusTopicId.fromString(didTopicId);
    HcsDid did = new HcsDid(network, privateKey.publicKey, fileId, topicId);

    // Convert HcsDid to HcsDid string
    String didString = did.toString();

    assertNotNull(didString);

    // Parse HcsDid string back to HcsDid object.
    HcsDid parsedDid = HcsDid.fromString(didString);

    assertNotNull(parsedDid);
    assertNotNull(parsedDid.getAddressBookFileId());
    assertNotNull(parsedDid.getDidTopicId());

    assertEquals(parsedDid.toDid(), didString);
    assertEquals(parsedDid.getMethod(), Method.HEDERA_HCS);
    assertEquals(parsedDid.getNetwork(), network);
    assertEquals(parsedDid.getAddressBookFileId().toString(), addressBook);
    assertEquals(parsedDid.getDidTopicId().toString(), didTopicId);
    assertEquals(parsedDid.getIdString(), did.getIdString());

    // Generate DID document
    DidDocumentBase parsedDocument = parsedDid.generateDidDocument();

    assertNotNull(parsedDocument);
    assertEquals(parsedDocument.getId(), parsedDid.toString());
    assertEquals(parsedDocument.getContext(), DidSyntax.DID_DOCUMENT_CONTEXT);
    assertNull(parsedDocument.getDidRootKey());

    // Generate DID document from original DID.
    DidDocumentBase document = did.generateDidDocument();

    assertNotNull(document);
    assertEquals(document.getId(), parsedDid.toString());
    assertEquals(document.getContext(), DidSyntax.DID_DOCUMENT_CONTEXT);
    assertNotNull(document.getDidRootKey());
    assertEquals(document.getDidRootKey().getPublicKeyBase58(), Base58.encode(privateKey.publicKey.toBytes()));

  }

  @Test
  void testParsePredefinedDids() throws NoSuchAlgorithmException {
    final String addressBook = "0.0.24352";
    final String didTopicId = "1.5.23462345";

    final String validDidWithSwitchedParamsOrder = "did:hedera:testnet:8LjUL78kFVnWV9rFnNCTE5bZdRmjm2obqJwS892jVLak"
        + ";hedera:testnet:tid=" + didTopicId
        + ";hedera:testnet:fid=" + addressBook;

    final String[] invalidDids = {
        null,
        "invalidDid1",
        "did:invalid",
        "did:invalidMethod:8LjUL78kFVnWV9rFnNCTE5bZdRmjm2obqJwS892jVLak;hedera:testnet:fid=0.0.24352",
        "did:hedera:invalidNetwork:8LjUL78kFVnWV9rFnNCTE5bZdRmjm2obqJwS892jVLak;hedera:testnet:fid=0.0.24352",
        "did:hedera:testnet:invalidAddress;hedera:testnet:fid=0.0.24352;hedera:testnet:tid=1.5.23462345",
        "did:hedera:testnet;hedera:testnet:fid=0.0.24352;hedera:testnet:tid=1.5.23462345",
        "did:hedera:testnet:8LjUL78kFVnWV9rFnNCTE5bZdRmjm2obqJwS892jVLak;missing:fid=0.0.24352;"
            + "hedera:testnet:tid=1.5.2",
        "did:hedera:testnet:8LjUL78kFVnWV9rFnNCTE5bZdRmjm2obqJwS892jVLak;missing:fid=0.0.1;"
            + "hedera:testnet:tid=1.5.2;unknown:parameter=1",
        "did:hedera:testnet:8LjUL78kFVnWV9rFnNCTE5bZdRmjm2obqJwS892jVLak;hedera:testnet:fid=0.0.1=1",
        "did:hedera:testnet:8LjUL78kFVnWV9rFnNCTE5bZdRmjm2obqJwS892jVLak;hedera:testnet:fid",
        "did:hedera:testnet:8LjUL78kFVnWV9rFnNCTE5bZdRmjm2obqJwS892jVLak:unknownPart;hedera:testnet:fid=0.0.1",
        "did:notHedera:testnet:8LjUL78kFVnWV9rFnNCTE5bZdRmjm2obqJwS892jVLak;hedera:testnet:fid=0.0.1",
    };

    // Expect to fail parsing all invalid DIDs
    for (String did : invalidDids) {
      assertThrows(IllegalArgumentException.class, () -> HcsDid.fromString(did));
      assertThrows(IllegalArgumentException.class, () -> HederaDid.fromString(did));
    }

    // Parse valid DID with parameters order switched
    HcsDid validDid = HcsDid.fromString(validDidWithSwitchedParamsOrder);

    assertNotNull(validDid);
    assertNotNull(validDid.getAddressBookFileId());
    assertNotNull(validDid.getDidTopicId());

    assertEquals(validDid.getAddressBookFileId().toString(), addressBook);
    assertEquals(validDid.getDidTopicId().toString(), didTopicId);

    HederaDid validDidViaInterface = HederaDid.fromString(validDidWithSwitchedParamsOrder);
    assertNotNull(validDidViaInterface);

    assertEquals(validDid.getMethod(), HcsDid.DID_METHOD);
  }
}
