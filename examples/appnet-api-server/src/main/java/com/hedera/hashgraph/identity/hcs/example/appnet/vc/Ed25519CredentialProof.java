package com.hedera.hashgraph.identity.hcs.example.appnet.vc;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hedera.hashgraph.identity.hcs.did.HcsDidRootKey;
import com.hedera.hashgraph.identity.utils.JsonUtils;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.util.Base64URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;

/**
 * A simple, manually constructed example of a linked data proof of type Ed25519Signature2018.
 * Implementation is a simplified version for this example based on:
 * https://github.com/WebOfTrustInfo/ld-signatures-java/.
 * In a real-world application it is recommended to use a JSON-LD compatible library to handle normalization.
 * However at this point the only available one in Java support JSON-LD version 1.0, but 1.1 is required by W3C
 * Verifiable Credentials.
 */
public class Ed25519CredentialProof extends LinkedDataProof {
  public static final String PROOF_TYPE = "Ed25519Signature2018";
  public static final String VC_VERIFICATION_METHOD = "Ed25519Signature2018";
  public static final String VC_PROOF_PURPOSE = "assertionMethod";
  private static final String[] JSON_PROPERTIES_ORDER = { "type", "creator", "created", "domain", "nonce",
      "proofPurpose", "verificationMethod", "jws" };
  private static final String JSON_PROPERTY_JWS = "jws";

  /**
   * Constructs a new proof document - without signature.
   *
   * @param issuerDid DID of a credential issuer.
   */
  public Ed25519CredentialProof(final String issuerDid) {
    this(issuerDid, null, null);
  }

  /**
   * Constructs a new proof document - without signature.
   *
   * @param issuerDid DID of a credential issuer.
   * @param nonce     The variable nonce.
   */
  public Ed25519CredentialProof(final String issuerDid, final String nonce) {
    this(issuerDid, null, nonce);
  }

  /**
   * Constructs a new proof document - without signature.
   *
   * @param issuerDid DID of a credential issuer.
   * @param domain    The domain.
   * @param nonce     The variable nonce.
   */
  public Ed25519CredentialProof(final String issuerDid, final String domain, final String nonce) {
    setType(PROOF_TYPE);
    setProofPurpose(VC_PROOF_PURPOSE);
    setVerificationMethod(issuerDid + HcsDidRootKey.DID_ROOT_KEY_NAME);
    setCreator(issuerDid);
    setCreated(Instant.now());
    setDomain(domain);
    setNonce(nonce);
  }

  /**
   * Note: this is a manual implementation of ordered JSON items.
   * In a real-world application it is recommended to use a JSON-LD compatible library to handle normalization.
   * However at this point the only available one in Java support JSON-LD version 1.0, but 1.1 is required by W3C
   * Verifiable Credentials.
   * 
   * @param  withoutSignature Will skip signature value ('jwk' attribute) if True.
   * @return                  A normalized JSON string representation of this proof.
   */
  public JsonElement toNormalizedJsonElement(final boolean withoutSignature) {
    Gson gson = JsonUtils.getGson();

    // First turn to normal JSON
    JsonObject root = gson.toJsonTree(this).getAsJsonObject();
    // Then put JSON properties in ordered map
    LinkedHashMap<String, JsonElement> map = new LinkedHashMap<>();

    for (String property : JSON_PROPERTIES_ORDER) {
      if (JSON_PROPERTY_JWS.equals(property) && withoutSignature) {
        continue;
      } else if (root.has(property)) {
        map.put(property, root.get(property));
      }
    }
    // Turn map to JSON
    return gson.toJsonTree(map);
  }

  /**
   * Creates a linked data proof of type
   * Implementation is a simplified version for this example based on
   * https://github.com/WebOfTrustInfo/ld-signatures-java/.
   *
   * @param signingKey     Private key of the signing subject.
   * @param documentToSign The canonicalized JSON string of a verifiable credential document.
   */
  public void sign(final Ed25519PrivateKey signingKey, final String documentToSign) {
    byte[] inputForSigning = new byte[64];
    String normalizedProof = JsonUtils.getGson().toJson(toNormalizedJsonElement(true));

    byte[] normalizedDocHash = Hashing.sha256().hashBytes(documentToSign.getBytes(StandardCharsets.UTF_8))
        .asBytes();
    byte[] normalizedProofHash = Hashing.sha256().hashBytes(normalizedProof.getBytes(StandardCharsets.UTF_8))
        .asBytes();

    System.arraycopy(normalizedProofHash, 0, inputForSigning, 0, 32);
    System.arraycopy(normalizedDocHash, 0, inputForSigning, 32, 32);

    JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.EdDSA).customParam("b64", Boolean.FALSE)
        .criticalParams(Collections.singleton("b64")).build();
    byte[] jwsSigningInput = getJwsSigningInput(jwsHeader, inputForSigning);

    Base64URL signature = Base64URL.encode(signingKey.sign(jwsSigningInput));
    setJws(jwsHeader.toBase64URL().toString() + '.' + '.' + signature.toString());
  }

  /**
   * Creates a signing input in JWS form.
   *
   * @param  header       The JWS header.
   * @param  signingInput The signing input.
   * @return              The singing input in JWS form.
   */
  private static byte[] getJwsSigningInput(final JWSHeader header, final byte[] signingInput) {
    byte[] encodedHeader;

    if (header.getParsedBase64URL() != null) {
      encodedHeader = header.getParsedBase64URL().toString().getBytes(StandardCharsets.UTF_8);
    } else {
      encodedHeader = header.toBase64URL().toString().getBytes(StandardCharsets.UTF_8);
    }

    byte[] jwsSigningInput = new byte[encodedHeader.length + 1 + signingInput.length];
    System.arraycopy(encodedHeader, 0, jwsSigningInput, 0, encodedHeader.length);
    jwsSigningInput[encodedHeader.length] = (byte) '.';
    System.arraycopy(signingInput, 0, jwsSigningInput, encodedHeader.length + 1, signingInput.length);

    return jwsSigningInput;
  }
}
