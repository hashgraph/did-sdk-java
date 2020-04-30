package com.hedera.hashgraph.identity.hcs.example.appnet.handlers;

import com.google.common.base.Strings;
import com.hedera.hashgraph.identity.DidDocumentBase;
import com.hedera.hashgraph.identity.HederaNetwork;
import com.hedera.hashgraph.identity.hcs.HcsIdentityNetwork;
import com.hedera.hashgraph.identity.hcs.Message;
import com.hedera.hashgraph.identity.hcs.MessageEnvelope;
import com.hedera.hashgraph.identity.hcs.did.HcsDid;
import com.hedera.hashgraph.identity.hcs.did.HcsDidMessage;
import com.hedera.hashgraph.identity.hcs.example.appnet.AppnetStorage;
import com.hedera.hashgraph.identity.hcs.example.appnet.dto.DrivingLicenseRequest;
import com.hedera.hashgraph.identity.hcs.example.appnet.dto.ErrorResponse;
import com.hedera.hashgraph.identity.hcs.example.appnet.vc.CredentialSchema;
import com.hedera.hashgraph.identity.hcs.example.appnet.vc.DrivingLicense;
import com.hedera.hashgraph.identity.hcs.example.appnet.vc.DrivingLicenseDocument;
import com.hedera.hashgraph.identity.hcs.example.appnet.vc.Ed25519CredentialProof;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcDocumentBase;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcMessage;
import com.hedera.hashgraph.identity.utils.JsonUtils;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.handling.Context;
import ratpack.http.Status;
import ratpack.jackson.Jackson;

/**
 * Handler of demo requests for presentation purposes.
 * In a real-world application, these functionalities would be executed
 * in a secure environment (e.g. a secure enclave, HSM, or a on the client side),
 * so that private keys would not be handled and exchanged over REST API.
 */
public class DemoHandler extends AppnetHandler {
  private static Logger log = LoggerFactory.getLogger(DemoHandler.class);
  private static final String HEADER_PRIVATE_KEY = "privateKey";

  /**
   * Instantiates the handler.
   *
   * @param identityNetwork The Hedera Identity network.
   * @param storage         The appnet's local storage.
   */
  public DemoHandler(final HcsIdentityNetwork identityNetwork, final AppnetStorage storage) {
    super(identityNetwork, storage);
  }

  /**
   * Generates a new DID document and writes it to the response body.
   * Private key is returned in a response header.
   *
   * @param ctx The HTTP context.
   */
  public void generateDid(final Context ctx) {
    Ed25519PrivateKey privateKey = HcsDid.generateDidRootKey();
    HcsDid did = new HcsDid(HederaNetwork.TESTNET, privateKey.publicKey, identityNetwork.getAddressBook().getFileId());
    DidDocumentBase doc = did.generateDidDocument();

    ctx.header(HEADER_PRIVATE_KEY, privateKey.toString());
    ctx.render(doc.toJson());
  }

  /**
   * Signs the DID message with a private key provided in the request header.
   *
   * @param ctx The HTTP context.
   */
  public void signDidMessage(final Context ctx) {
    signMessage(ctx, HcsDidMessage.class);
  }

  /**
   * Signs the message of a given type with a private key provided in the request header.
   * Writes the signed message into the response body.
   *
   * @param <T>          The type of the message.
   * @param ctx          The HTTP context.
   * @param messageClass The class type of the message.
   */
  private <T extends Message> void signMessage(final Context ctx, final Class<T> messageClass) {
    try {
      Ed25519PrivateKey privateKey = getPrivateKeyFromHeader(ctx);

      ctx.getRequest().getBody().then(data -> signAndReturnMessage(ctx, data.getText(), privateKey, messageClass));
    } catch (Exception e) {
      ctx.getResponse().status(Status.BAD_REQUEST);
      ctx.render(Jackson.json(new ErrorResponse(e)));
    }
  }

  /**
   * Signs the message of a given type with a private key provided in the request header.
   * Writes the signed message into the response body.
   *
   * @param <T>          The type of the message.
   * @param ctx          The HTTP context.
   * @param json         The message as JSON string.
   * @param privateKey   Private key of the signer.
   * @param messageClass The class type of the message.
   */
  private <T extends Message> void signAndReturnMessage(final Context ctx, final String json,
      final Ed25519PrivateKey privateKey,
      final Class<T> messageClass) {
    try {
      MessageEnvelope<T> envelope = MessageEnvelope.fromJson(json, messageClass);
      ctx.render(new String(envelope.sign(m -> privateKey.sign(m)), StandardCharsets.UTF_8));
    } catch (Exception e) {
      ctx.getResponse().status(Status.BAD_REQUEST);
      ctx.render(Jackson.json(new ErrorResponse("Invalid message content received.", e)));
    }

  }

  /**
   * Generates a new example verifiable credential document - a driving license.
   * The input data comes from request body.
   * Private key used to create a VC proof comes from the request header.
   * The VC is built manually here for demonstration purposes only.
   * A real-world application should use an RDF or JSON-LD library support to build flexible,
   * extensible and manageable verifiable credential documents.
   *
   * @param ctx The HTTP context.
   */
  public void generateDrivingLicense(final Context ctx) {
    ctx.getRequest().getBody().then(data -> {
      DrivingLicenseRequest req = null;
      try {
        req = JsonUtils.getGson().fromJson(data.getText(), DrivingLicenseRequest.class);
      } catch (Exception e) {
        ctx.getResponse().status(Status.BAD_REQUEST);
        ctx.render(Jackson.json(new ErrorResponse("Invalid request input.")));
        return;
      }

      // TODO This check could be enabled in case we want to make sure the Issuer published their DID first.
      // HcsDidMessage issuer = storage.resolveDid(req.getIssuer());
      // if (issuer == null) {
      // ctx.getResponse().status(Status.BAD_REQUEST);
      // ctx.render(Jackson.json(new ErrorResponse("The issuer DID could not be resolved: " + req.getIssuer())));
      // return;
      // }

      log.info("Generating new driving license document for: " + req.getOwner());

      try {
        DrivingLicenseDocument vc = new DrivingLicenseDocument();
        vc.setIssuer(req.getIssuer());
        vc.setIssuanceDate(Instant.now());
        vc.addCredentialSubject(
            new DrivingLicense(req.getOwner(), req.getFirstName(), req.getLastName(),
                req.getDrivingLicenseCategories()));

        CredentialSchema schema = new CredentialSchema("http://localhost:5050/driving-license-schema.json",
            DrivingLicenseDocument.CREDENTIAL_SCHEMA_TYPE);

        vc.setCredentialSchema(schema);

        Ed25519PrivateKey privateKey = getPrivateKeyFromHeader(ctx);
        Ed25519CredentialProof proof = new Ed25519CredentialProof(req.getIssuer());
        proof.sign(privateKey, vc.toNormalizedJson(true));
        vc.setProof(proof);

        storage.registerCredentialIssuance(vc.toCredentialHash(), privateKey.publicKey);
        ctx.render(vc.toNormalizedJson(false));
      } catch (Exception e) {
        ctx.getResponse().status(Status.INTERNAL_SERVER_ERROR);
        ctx.render(Jackson.json(new ErrorResponse("Driving license generation failed.", e)));
        return;
      }
    });
  }

  /**
   * Extracts the Ed25519 private key from a request header parameter.
   *
   * @param  ctx                      The HTTP context.
   * @return                          The extracted private key.
   * @throws IllegalArgumentException In case private key is missing or an invalid string was provided.
   */
  private Ed25519PrivateKey getPrivateKeyFromHeader(final Context ctx) {
    String privateKeyString = ctx.getRequest().getHeaders().get(HEADER_PRIVATE_KEY);
    if (Strings.isNullOrEmpty(privateKeyString)) {
      throw new IllegalArgumentException("Private key is missing in the request header.");
    }

    try {
      return Ed25519PrivateKey.fromString(privateKeyString);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Provided private key is invalid.", ex);
    }
  }

  /**
   * Signs the VC message with a private key given in the request header.
   * The signed message is written into the response body.
   *
   * @param ctx The HTTP context.
   */
  public void signVcMessage(final Context ctx) {
    signMessage(ctx, HcsVcMessage.class);
  }

  /**
   * Calculates a credential hash for the given credential document.
   * Resulting hash is written directly into the response body as raw text.
   *
   * @param ctx The HTTP context.
   */
  public void determineCredentialHash(final Context ctx) {
    ctx.getRequest().getBody().then(data -> {
      try {
        String hash = HcsVcDocumentBase.fromJson(data.getText(), DrivingLicense.class).toCredentialHash();
        ctx.render(hash);
      } catch (Exception e) {
        ctx.getResponse().status(Status.BAD_REQUEST);
        ctx.render(Jackson.json(new ErrorResponse("Invalid verifiable credential document received.")));
      }
    });
  }
}
