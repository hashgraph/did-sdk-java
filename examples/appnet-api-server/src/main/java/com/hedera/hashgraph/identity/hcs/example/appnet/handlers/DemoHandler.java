package com.hedera.hashgraph.identity.hcs.example.appnet.handlers;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.hedera.hashgraph.identity.DidDocumentBase;
import com.hedera.hashgraph.identity.hcs.HcsIdentityNetwork;
import com.hedera.hashgraph.identity.hcs.Message;
import com.hedera.hashgraph.identity.hcs.MessageEnvelope;
import com.hedera.hashgraph.identity.hcs.did.HcsDid;
import com.hedera.hashgraph.identity.hcs.did.HcsDidMessage;
import com.hedera.hashgraph.identity.hcs.example.appnet.AppnetStorage;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.interactor.AgeCircuitProverInteractor;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.interactor.AgeCircuitVerifierInteractor;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.mapper.AgeCircuitProverDataMapper;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.mapper.AgeCircuitVerifierDataMapper;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.model.VerifyAgePublicInput;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.provider.ZkSnarkAgeProverProvider;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.provider.ZkSnarkAgeVerifierProvider;
import com.hedera.hashgraph.identity.hcs.example.appnet.dto.DrivingLicenseRequest;
import com.hedera.hashgraph.identity.hcs.example.appnet.dto.ErrorResponse;
import com.hedera.hashgraph.identity.hcs.example.appnet.marshaller.DriverAboveAgeVpMarshaller;
import com.hedera.hashgraph.identity.hcs.example.appnet.marshaller.DrivingLicenseZeroKnowledgeVcMarshaller;
import com.hedera.hashgraph.identity.hcs.example.appnet.vc.*;
import com.hedera.hashgraph.identity.hcs.example.appnet.vp.DriverAboveAgePresentation;
import com.hedera.hashgraph.identity.hcs.example.appnet.vp.DriverAboveAgeVerifiableCredential;
import com.hedera.hashgraph.identity.hcs.example.appnet.vp.DrivingLicenseVpGenerator;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcDocumentBase;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcMessage;
import com.hedera.hashgraph.identity.utils.JsonUtils;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.zeroknowledge.merkletree.factory.MerkleTreeFactoryImpl;
import com.hedera.hashgraph.zeroknowledge.vp.proof.PresentationProof;
import com.hedera.hashgraph.zeroknowledge.vp.proof.ZkSignature;
import io.github.cdimascio.dotenv.Dotenv;
import io.horizen.common.schnorrnative.SchnorrKeyPair;
import io.horizen.common.schnorrnative.SchnorrPublicKey;
import io.horizen.common.schnorrnative.SchnorrSecretKey;
import org.bitcoinj.core.Base58;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import ratpack.handling.Context;
import ratpack.http.Status;
import ratpack.jackson.Jackson;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler of demo requests for presentation purposes.
 * In a real-world application, these functionalities would be executed
 * in a secure environment (e.g. a secure enclave, HSM, or a on the client side),
 * so that private keys would not be handled and exchanged over REST API.
 */
public class DemoHandler extends AppnetHandler {
  public static final String HEADER_PRIVATE_KEY = "privateKey";
  public static final String HEADER_SCHNORR_SECRET_KEY = "schnorrSecretKey";
  private static final Logger log = LoggerFactory.getLogger(DemoHandler.class);
  public static final String SCHNORR_PUBLIC_KEY = "schnorrPublicKey";

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
    PrivateKey privateKey = HcsDid.generateDidRootKey();
    HcsDid did = new HcsDid(identityNetwork.getNetwork(), privateKey.getPublicKey(),
            identityNetwork.getAddressBook().getFileId());
    DidDocumentBase doc = did.generateDidDocument();

    ctx.header(HEADER_PRIVATE_KEY, privateKey.toString());
    ctx.render(doc.toJson());
  }

  /**
   * Generates a new DID document containing an additional public key in the public key section used to compute
   * the zero knowledge proof and writes it to the response body.
   * Private key is returned in a response header.
   *
   * @param ctx The HTTP context.
   */
  public void generateDidWithZeroKnowledge(final Context ctx) {
    PrivateKey privateKey = HcsDid.generateDidRootKey();
    SchnorrPublicKey holderPublicKey = getSchnorrPublicKeyFromHeader(ctx);
    HcsDid did = new HcsDid(identityNetwork.getNetwork(), privateKey.getPublicKey(),
            identityNetwork.getAddressBook().getFileId());
    did.setZkDidKeys(ByteUtils.toHexString(holderPublicKey.serializePublicKey()));
    DidDocumentBase doc = did.generateDidDocumentZk();

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
      PrivateKey privateKey = getPrivateKeyFromHeader(ctx);

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
                                                        final PrivateKey privateKey,
                                                        final Class<T> messageClass) {
    try {
      MessageEnvelope<T> envelope = MessageEnvelope.fromJson(json, messageClass);
      ctx.render(new String(envelope.sign(privateKey::sign), StandardCharsets.UTF_8));
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
      DrivingLicenseRequest req;
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
                        req.getDrivingLicenseCategories(), req.getBirthDate()));

        CredentialSchema schema = new CredentialSchema(
                "http://localhost:5050/driving-license-schema.json",
                DrivingLicenseDocument.CREDENTIAL_SCHEMA_TYPE
        );

        vc.setCredentialSchema(schema);

        PrivateKey privateKey = getPrivateKeyFromHeader(ctx);
        Ed25519CredentialProof proof = new Ed25519CredentialProof(req.getIssuer());
        proof.sign(privateKey, vc.toNormalizedJson(true));
        vc.setProof(proof);

        storage.registerCredentialIssuance(vc.toCredentialHash(), privateKey.getPublicKey());
        ctx.render(vc.toNormalizedJson(false));
      } catch (Exception e) {
        ctx.getResponse().status(Status.INTERNAL_SERVER_ERROR);
        ctx.render(Jackson.json(new ErrorResponse("Driving license generation failed.", e)));
      }
    });
  }

  /**
   * Generates a verifiable credential document containing the driving license. This method uses the zero knowledge feature,
   * so it's including the merkle tree root and a zero knowledge signature in the proof section.
   * The input data comes from request body.
   * It needs both private key to sign the document and the Schnorr secret key to compute the zk signature.
   * Both the keys come from the request header.
   * @param ctx The HTTP context.
   */
  public void generateZeroKnowledgeDrivingLicense(final Context ctx) {
    ctx.getRequest().getBody().then(data -> {
      DrivingLicenseRequest req;
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

      log.info("Generating new driving license document with zero knowledge for: " + req.getOwner());

      try {
        DrivingLicenseZeroKnowledgeDocument vc = new DrivingLicenseZeroKnowledgeDocument();
        DrivingLicenseZeroKnowledgeVcMarshaller presenter = new DrivingLicenseZeroKnowledgeVcMarshaller();

        vc.setIssuer(req.getIssuer());
        vc.setIssuanceDate(Instant.now());
        DrivingLicense drivingLicense = new DrivingLicense(req.getOwner(), req.getFirstName(), req.getLastName(),
                req.getDrivingLicenseCategories(), req.getBirthDate());
        vc.addCredentialSubject(drivingLicense);

        CredentialSchema schema = new CredentialSchema(
                "http://localhost:5050/driving-license-schema.json",
                DrivingLicenseDocument.CREDENTIAL_SCHEMA_TYPE
        );

        vc.setCredentialSchema(schema);

        PrivateKey privateKey = getPrivateKeyFromHeader(ctx);
        SchnorrSecretKey schnorrSecretKey = getSchnorrSecretKeyFromHeader(ctx);

        Ed25519CredentialProof proof = new Ed25519CredentialProof(req.getIssuer());
        proof.sign(privateKey, presenter.fromDocumentToString(vc));
        vc.setProof(proof);

        ZkSignature<DrivingLicense> zkSignature = new ZkSignature<>(
                new MerkleTreeFactoryImpl()
        );
        zkSignature.sign(schnorrSecretKey.serializeSecretKey(), vc);
        vc.setZeroKnowledgeSignature(zkSignature);

        storage.registerCredentialIssuance(vc.toCredentialHash(), privateKey.getPublicKey());
        ctx.render(presenter.fromDocumentToString(vc));
      } catch (Exception e) {
        ctx.getResponse().status(Status.INTERNAL_SERVER_ERROR);
        ctx.render(Jackson.json(new ErrorResponse("Driving license generation failed.", e)));
      }
    });
  }

  /**
   * Method to generate an above-age presentation from a VC document containing the user's birthdate.
   * The input data comes from request body.
   * The user's and authority's public keys come from the request header.
   * The generated presentation is sent in the response body.
   *
   * @param ctx The HTTP context.
   */
  public void generateDrivingAboveAgePresentation(final Context ctx) {
    ctx.getRequest().getBody().then(data -> {
      JsonObject req;
      DrivingLicenseZeroKnowledgeDocument doc;

      try {
        req = JsonUtils.getGson().fromJson(data.getText(), JsonObject.class);
        DrivingLicenseZeroKnowledgeVcMarshaller presenter = new DrivingLicenseZeroKnowledgeVcMarshaller();
        doc = presenter.fromStringToDocument(JsonUtils.getGson().toJson(req.get("verifiableCredential")));
      } catch (Exception e) {
        ctx.getResponse().status(Status.BAD_REQUEST);
        ctx.render(Jackson.json(new ErrorResponse("Invalid request input.")));
        return;
      }

      try {
        DriverAboveAgeVpMarshaller presenter = new DriverAboveAgeVpMarshaller();
        DrivingLicenseVpGenerator vpGenerator = new DrivingLicenseVpGenerator(
                new ZkSnarkAgeProverProvider(
                        new AgeCircuitProverInteractor(),
                        new AgeCircuitProverDataMapper(new MerkleTreeFactoryImpl())
                )
        );
        String holderPublicKeyBase58 = getBase58PublicKeyFromHeaderByLabel(ctx, "holderPublicKey");
        String holderPublicKey = ByteUtils.toHexString(Base58.decode(holderPublicKeyBase58));

        String authorityPublicKeyBase58 = getBase58PublicKeyFromHeaderByLabel(ctx, "authorityPublicKey");
        String authorityPublicKey = ByteUtils.toHexString(Base58.decode(authorityPublicKeyBase58));

        String secretKey = ByteUtils.toHexString(getSchnorrSecretKeyFromHeader(ctx).serializeSecretKey());

        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("challenge", req.get("challenge").getAsString());
        metadataMap.put("ageThreshold", req.get("ageThreshold").getAsString());
        metadataMap.put("secretKey", secretKey);
        metadataMap.put("dayLabel", req.get("dayLabel").getAsString());
        metadataMap.put("monthLabel", req.get("monthLabel").getAsString());
        metadataMap.put("yearLabel", req.get("yearLabel").getAsString());
        metadataMap.put("holderPublicKey", holderPublicKey);
        metadataMap.put("authorityPublicKey", authorityPublicKey);

        DriverAboveAgePresentation presentation = vpGenerator.generatePresentation(
                Collections.singletonList(doc),
                metadataMap
        );

        ctx.render(presenter.fromDocumentToString(presentation));
      } catch (Exception e) {
        ctx.getResponse().status(Status.INTERNAL_SERVER_ERROR);
        ctx.render(Jackson.json(new ErrorResponse("Cannot generate verifiable presentation document.")));
      }
    });
  }

  private String getBase58PublicKeyFromHeaderByLabel(Context ctx, String label) {
    String holderPublicKey = ctx.getRequest().getHeaders().get(label);
    if (Strings.isNullOrEmpty(holderPublicKey)) {
      throw new IllegalArgumentException("Private key is missing in the request header.");
    }

    return holderPublicKey;
  }

  /**
   * Extracts the Ed25519 private key from a request header parameter.
   *
   * @param ctx The HTTP context.
   * @return The extracted private key.
   * @throws IllegalArgumentException In case private key is missing or an invalid string was provided.
   */
  private PrivateKey getPrivateKeyFromHeader(final Context ctx) {
    String privateKeyString = ctx.getRequest().getHeaders().get(HEADER_PRIVATE_KEY);
    if (Strings.isNullOrEmpty(privateKeyString)) {
      throw new IllegalArgumentException("Private key is missing in the request header.");
    }

    try {
      return PrivateKey.fromString(privateKeyString);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Provided private key is invalid.", ex);
    }
  }

  private SchnorrSecretKey getSchnorrSecretKeyFromHeader(final Context ctx) {
    String privateKeyString = ctx.getRequest().getHeaders().get(HEADER_SCHNORR_SECRET_KEY);
    if (Strings.isNullOrEmpty(privateKeyString)) {
      throw new IllegalArgumentException("Schnorr secret key is missing in the request header.");
    }

    try {
      return SchnorrSecretKey.deserialize(ByteUtils.fromHexString(privateKeyString));
    } catch (Exception ex) {
      throw new IllegalArgumentException("Provided Schnorr secret key is invalid.", ex);
    }
  }

  private SchnorrPublicKey getSchnorrPublicKeyFromHeader(final Context ctx) {
    String publicKeyString = ctx.getRequest().getHeaders().get(SCHNORR_PUBLIC_KEY);
    if (Strings.isNullOrEmpty(publicKeyString)) {
      throw new IllegalArgumentException("Schnorr public key is missing in the request header.");
    }

    try {
      return SchnorrPublicKey.deserialize(ByteUtils.fromHexString(publicKeyString));
    } catch (Exception ex) {
      throw new IllegalArgumentException("Provided Schnorr public key is invalid.", ex);
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
        String hash = HcsVcDocumentBase.fromJson(data.getText(), DrivingLicenseDocument.class, DrivingLicense.class).toCredentialHash();
        ctx.render(hash);
      } catch (Exception e) {
        ctx.getResponse().status(Status.BAD_REQUEST);
        ctx.render(Jackson.json(new ErrorResponse("Invalid verifiable credential document received.")));
      }
    });
  }

  public void determineZkCredentialHash(Context ctx) {
    ctx.getRequest().getBody().then(data -> {
      try {
        DrivingLicenseZeroKnowledgeVcMarshaller presenter = new DrivingLicenseZeroKnowledgeVcMarshaller();
        DrivingLicenseZeroKnowledgeDocument document = presenter.fromStringToDocument(data.getText());
        String hash = document.toCredentialHash();
        ctx.render(hash);
      } catch (Exception e) {
        ctx.getResponse().status(Status.BAD_REQUEST);
        ctx.render(Jackson.json(new ErrorResponse("Invalid verifiable credential document received.")));
      }
    });
  }

  public void determinePresentationCredentialHash(Context ctx) {
    ctx.getRequest().getBody().then(data -> {
      try {
        DriverAboveAgeVpMarshaller presenter = new DriverAboveAgeVpMarshaller();
        DriverAboveAgePresentation dld = presenter.fromStringToDocument(data.getText());
        String hash = dld.getCredentialHashForVerifiableCredentialByIndex(0);

        ctx.render(hash);
      } catch (Exception e) {
        ctx.getResponse().status(Status.BAD_REQUEST);
        ctx.render(Jackson.json(new ErrorResponse("Invalid verifiable credential document received.")));
      }
    });
  }

  public void verifyPresentation(Context ctx) {
    ctx.getRequest().getBody().then(data -> {
      try {
        JsonObject req;
        DriverAboveAgePresentation dld;

        try {
          req = JsonUtils.getGson().fromJson(data.getText(), JsonObject.class);
          DriverAboveAgeVpMarshaller presenter = new DriverAboveAgeVpMarshaller();
          dld = presenter.fromStringToDocument(JsonUtils.getGson().toJson(req.get("presentation")));
        } catch (Exception e) {
          ctx.getResponse().status(Status.BAD_REQUEST);
          ctx.render(Jackson.json(new ErrorResponse("Invalid request input.")));
          return;
        }

        Dotenv dotenv = Dotenv.configure().load();

        String challenge = req.get("challenge").getAsString();
        String verificationKeyPath = dotenv.get("VERIFICATION_KEY_PATH");
        String ageThresholdString = req.get("ageThreshold").getAsString();
        int ageThreshold = Integer.parseInt(ageThresholdString);

        DriverAboveAgeVerifiableCredential drivingLicense = dld.getVerifiableCredential().get(0);
        ZkSnarkAgeVerifierProvider zkProofProvider = new ZkSnarkAgeVerifierProvider(
                new AgeCircuitVerifierInteractor(),
                new AgeCircuitVerifierDataMapper()
        );
        PresentationProof presentationProof = drivingLicense.getProof();
        String proof = presentationProof.getProof();

        LocalDateTime date = LocalDateTime.ofInstant(drivingLicense.getIssuanceDate(), ZoneId.systemDefault());

        String holderPublicKeyBase58 = getBase58PublicKeyFromHeaderByLabel(ctx, "holderPublicKey");
        String holderPublicKey = ByteUtils.toHexString(Base58.decode(holderPublicKeyBase58));

        String authorityPublicKeyBase58 = getBase58PublicKeyFromHeaderByLabel(ctx, "authorityPublicKey");
        String authorityPublicKey = ByteUtils.toHexString(Base58.decode(authorityPublicKeyBase58));

        VerifyAgePublicInput verifyAgePublicInput = new VerifyAgePublicInput(
          ByteUtils.fromHexString(proof), date.getYear(), date.getMonthValue(), date.getDayOfMonth(), ageThreshold,
                holderPublicKey, authorityPublicKey, challenge, drivingLicense.getId(), verificationKeyPath
        );

        if (zkProofProvider.verifyProof(verifyAgePublicInput)) {
          ctx.render("Proof verified");
        } else {
          ctx.render("Proof not valid");
        }

      } catch (Exception e) {
        ctx.getResponse().status(Status.BAD_REQUEST);
        ctx.render(Jackson.json(new ErrorResponse("Invalid verifiable presentation document received.")));
      }
    });
  }

  public void getSchnorrKeyPair(Context ctx) {
    ctx.getRequest().getBody().then(data -> {
      SchnorrKeyPair keyPair = SchnorrKeyPair.generate();
      SchnorrPublicKey publicKey = keyPair.getPublicKey();
      SchnorrSecretKey secretKey = keyPair.getSecretKey();

      ctx.header(HEADER_SCHNORR_SECRET_KEY, ByteUtils.toHexString(secretKey.serializeSecretKey()));
      ctx.header(SCHNORR_PUBLIC_KEY, ByteUtils.toHexString(publicKey.serializePublicKey()));

      ctx.getResponse().status(Status.OK);
      ctx.render("Schnorr key pair generated");
    });
  }
}
