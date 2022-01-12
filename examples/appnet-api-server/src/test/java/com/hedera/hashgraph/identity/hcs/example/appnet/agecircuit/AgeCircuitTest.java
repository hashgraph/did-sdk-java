package com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit;

import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.interactor.AgeCircuitProverInteractor;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.interactor.AgeCircuitVerifierInteractor;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.mapper.AgeCircuitProverDataMapper;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.mapper.AgeCircuitVerifierDataMapper;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.model.VerifyAgePublicInput;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.provider.ZkSnarkAgeProverProvider;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.provider.ZkSnarkAgeVerifierProvider;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.utils.ProvingSystemUtils;
import com.hedera.hashgraph.identity.hcs.example.appnet.dto.BirthDate;
import com.hedera.hashgraph.identity.hcs.example.appnet.marshaller.DrivingLicenseZeroKnowledgeVcMarshaller;
import com.hedera.hashgraph.identity.hcs.example.appnet.vc.*;
import com.hedera.hashgraph.identity.hcs.example.appnet.vp.DriverAboveAgePresentation;
import com.hedera.hashgraph.identity.hcs.example.appnet.vp.DriverAboveAgeVerifiableCredential;
import com.hedera.hashgraph.identity.hcs.example.appnet.vp.DrivingLicenseVpGenerator;
import com.hedera.hashgraph.identity.hcs.vc.Issuer;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.zeroknowledge.exception.VerifiablePresentationGenerationException;
import com.hedera.hashgraph.zeroknowledge.exception.ZeroKnowledgeVerifyProviderException;
import com.hedera.hashgraph.zeroknowledge.exception.ZkSignatureException;
import com.hedera.hashgraph.zeroknowledge.merkletree.factory.MerkleTreeFactoryImpl;
import com.hedera.hashgraph.zeroknowledge.utils.ByteUtils;
import com.hedera.hashgraph.zeroknowledge.vp.proof.PresentationProof;
import com.hedera.hashgraph.zeroknowledge.vp.proof.ZkSignature;
import io.github.cdimascio.dotenv.Dotenv;
import io.horizen.common.librustsidechains.InitializationException;
import io.horizen.common.schnorrnative.SchnorrKeyPair;
import io.horizen.common.schnorrnative.SchnorrPublicKey;
import io.horizen.common.schnorrnative.SchnorrSecretKey;
import io.horizenlabs.agecircuit.AgeCircuitProof;
import io.horizenlabs.agecircuit.AgeCircuitProofException;
import io.horizenlabs.provingsystemnative.ProvingSystem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AgeCircuitTest {
    private final AgeCircuitProverInteractor ageCircuitInteractor = new AgeCircuitProverInteractor();
    private final MerkleTreeFactoryImpl merkleTreeFactory = new MerkleTreeFactoryImpl();
    private final AgeCircuitProverDataMapper circuitDataMapper = new AgeCircuitProverDataMapper(merkleTreeFactory);
    private final ZkSnarkAgeProverProvider ageProverProvider = new ZkSnarkAgeProverProvider(ageCircuitInteractor, circuitDataMapper);

    private final AgeCircuitVerifierInteractor verifierInteractor = new AgeCircuitVerifierInteractor();
    private final AgeCircuitVerifierDataMapper ageCircuitVerifierDataMapper = new AgeCircuitVerifierDataMapper();
    private final ZkSnarkAgeVerifierProvider verifierProvider = new ZkSnarkAgeVerifierProvider(verifierInteractor, ageCircuitVerifierDataMapper);

    private static String provingKeyPath;
    private static String verificationKeyPath;

    @BeforeAll
    public static void init() {
        Dotenv dotenv = Dotenv.configure().load();

        provingKeyPath = dotenv.get("PROVING_KEY_PATH");
        verificationKeyPath = dotenv.get("VERIFICATION_KEY_PATH");
    }

    @Test
    public void testCreateProofFullFlow_CorrectGenerationAndVerify() throws VerifiablePresentationGenerationException, ZeroKnowledgeVerifyProviderException, ZkSignatureException, InitializationException, AgeCircuitProofException {
        // Arrange
        // Creation of keys
        SchnorrKeyPair holderKeyPair = SchnorrKeyPair.generate();
        SchnorrPublicKey holderPublicKey = holderKeyPair.getPublicKey();
        SchnorrSecretKey holderSecretKey = holderKeyPair.getSecretKey();
        String holderSecretKeyHex = ByteUtils.bytesToHex(holderSecretKey.serializeSecretKey());
        String holderPublicKeyHex = ByteUtils.bytesToHex(holderPublicKey.serializePublicKey());

        SchnorrKeyPair authorityKeyPair = SchnorrKeyPair.generate();
        SchnorrPublicKey authorityPublicKey = authorityKeyPair.getPublicKey();
        SchnorrSecretKey authoritySecretKey = authorityKeyPair.getSecretKey();
        String authorityPublicKeyHex = ByteUtils.bytesToHex(authorityPublicKey.serializePublicKey());

        DrivingLicenseZeroKnowledgeVcMarshaller presenter = new DrivingLicenseZeroKnowledgeVcMarshaller();

        // VC document creation
        DrivingLicenseZeroKnowledgeDocument licenseDocument = new DrivingLicenseZeroKnowledgeDocument();
        licenseDocument.setId("fake-id");
        licenseDocument.setIssuer(new Issuer(ByteUtils.bytesToHex(authorityPublicKey.serializePublicKey())));
        Instant currentInstant = Instant.now();
        licenseDocument.setIssuanceDate(currentInstant);
        DrivingLicense drivingLicense = new DrivingLicense(ByteUtils.bytesToHex(holderPublicKey.serializePublicKey()), "fake-firstName", "fake-lastName",
                new ArrayList<>(), new BirthDate(3, 12, 1991));

        licenseDocument.addCredentialSubject(drivingLicense);
        CredentialSchema schema = new CredentialSchema(
                "http://localhost:5050/driving-license-schema.json",
                DrivingLicenseDocument.CREDENTIAL_SCHEMA_TYPE
        );

        licenseDocument.setCredentialSchema(schema);

        Ed25519CredentialProof proof = new Ed25519CredentialProof(ByteUtils.bytesToHex(authorityPublicKey.serializePublicKey()));
        proof.sign(PrivateKey.generate(), presenter.fromDocumentToString(licenseDocument));
        licenseDocument.setProof(proof);

        ZkSignature<DrivingLicense> zkSignature = new ZkSignature<>(
                new MerkleTreeFactoryImpl()
        );
        zkSignature.sign(authoritySecretKey.serializeSecretKey(), licenseDocument);
        licenseDocument.setZeroKnowledgeSignature(zkSignature);

        Map<String, Object> presentationMetadata = new HashMap<>();
        presentationMetadata.put("ageThreshold", 18);
        presentationMetadata.put("challenge", "fake-challenge");
        presentationMetadata.put("secretKey", holderSecretKeyHex);
        presentationMetadata.put("dayLabel", "day");
        presentationMetadata.put("monthLabel", "month");
        presentationMetadata.put("yearLabel", "year");
        presentationMetadata.put("holderPublicKey", holderPublicKeyHex);
        presentationMetadata.put("authorityPublicKey", authorityPublicKeyHex);

        DrivingLicenseVpGenerator generator = new DrivingLicenseVpGenerator(ageProverProvider);

        // Create the temporary keys for the test
        ProvingSystemUtils.generateDLogKeys();
        AgeCircuitProof.setup(provingKeyPath, verificationKeyPath);

        // Act
        DriverAboveAgePresentation presentation = generator.generatePresentation(Collections.singletonList(licenseDocument), presentationMetadata);
        List<DriverAboveAgeVerifiableCredential> verifiableCredentials = presentation.getVerifiableCredential();
        PresentationProof presentationProof = verifiableCredentials.get(0).getProof();
        byte[] proofResult = ByteUtils.hexStringToByteArray(presentationProof.getProof());

        // Arrange
        LocalDateTime date = LocalDateTime.ofInstant(currentInstant, ZoneId.systemDefault());
        long currentYear = date.getYear();
        long currentMonth = date.getMonthValue();
        long currentDay = date.getDayOfMonth();
        int ageThreshold = 18;
        String challenge = "fake-challenge";
        String documentId = "fake-id";

        VerifyAgePublicInput verifyPublicInput = new VerifyAgePublicInput(
                proofResult, currentYear, currentMonth, currentDay, ageThreshold, ByteUtils.bytesToHex(holderPublicKey.serializePublicKey()),
                ByteUtils.bytesToHex(authorityPublicKey.serializePublicKey()), challenge, documentId, verificationKeyPath
        );

        // Act
        boolean verifyResult = verifierProvider.verifyProof(verifyPublicInput);

        // Assert
        assertTrue(verifyResult);
    }

    @Test
    public void testCreateProofFullFlow_CorrectGenerationButVerifyChallengeIsDifferent() throws VerifiablePresentationGenerationException, ZeroKnowledgeVerifyProviderException, ZkSignatureException, InitializationException, AgeCircuitProofException {
        // Arrange
        // Creation of keys
        SchnorrKeyPair holderKeyPair = SchnorrKeyPair.generate();
        SchnorrPublicKey holderPublicKey = holderKeyPair.getPublicKey();
        SchnorrSecretKey holderSecretKey = holderKeyPair.getSecretKey();
        String holderSecretKeyHex = ByteUtils.bytesToHex(holderSecretKey.serializeSecretKey());
        String holderPublicKeyHex = ByteUtils.bytesToHex(holderPublicKey.serializePublicKey());

        SchnorrKeyPair authorityKeyPair = SchnorrKeyPair.generate();
        SchnorrPublicKey authorityPublicKey = authorityKeyPair.getPublicKey();
        SchnorrSecretKey authoritySecretKey = authorityKeyPair.getSecretKey();
        String authorityPublicKeyHex = ByteUtils.bytesToHex(authorityPublicKey.serializePublicKey());

        DrivingLicenseZeroKnowledgeVcMarshaller presenter = new DrivingLicenseZeroKnowledgeVcMarshaller();

        // VC document creation
        DrivingLicenseZeroKnowledgeDocument licenseDocument = new DrivingLicenseZeroKnowledgeDocument();
        licenseDocument.setId("fake-id");
        licenseDocument.setIssuer(new Issuer(ByteUtils.bytesToHex(authorityPublicKey.serializePublicKey())));
        Instant currentInstant = Instant.now();
        licenseDocument.setIssuanceDate(currentInstant);
        DrivingLicense drivingLicense = new DrivingLicense(ByteUtils.bytesToHex(holderPublicKey.serializePublicKey()), "fake-firstName", "fake-lastName",
                new ArrayList<>(), new BirthDate(3, 12, 1991));

        licenseDocument.addCredentialSubject(drivingLicense);
        CredentialSchema schema = new CredentialSchema(
                "http://localhost:5050/driving-license-schema.json",
                DrivingLicenseDocument.CREDENTIAL_SCHEMA_TYPE
        );

        licenseDocument.setCredentialSchema(schema);

        Ed25519CredentialProof proof = new Ed25519CredentialProof(ByteUtils.bytesToHex(authorityPublicKey.serializePublicKey()));
        proof.sign(PrivateKey.generate(), presenter.fromDocumentToString(licenseDocument));
        licenseDocument.setProof(proof);

        ZkSignature<DrivingLicense> zkSignature = new ZkSignature<>(
                new MerkleTreeFactoryImpl()
        );
        zkSignature.sign(authoritySecretKey.serializeSecretKey(), licenseDocument);
        licenseDocument.setZeroKnowledgeSignature(zkSignature);

        Map<String, Object> presentationMetadata = new HashMap<>();
        presentationMetadata.put("ageThreshold", 18);
        presentationMetadata.put("challenge", "fake-challenge");
        presentationMetadata.put("secretKey", holderSecretKeyHex);
        presentationMetadata.put("dayLabel", "day");
        presentationMetadata.put("monthLabel", "month");
        presentationMetadata.put("yearLabel", "year");
        presentationMetadata.put("holderPublicKey", holderPublicKeyHex);
        presentationMetadata.put("authorityPublicKey", authorityPublicKeyHex);

        DrivingLicenseVpGenerator generator = new DrivingLicenseVpGenerator(ageProverProvider);

        // Create the temporary keys for the test
        ProvingSystemUtils.generateDLogKeys();
        AgeCircuitProof.setup(provingKeyPath, verificationKeyPath);

        // Act
        DriverAboveAgePresentation presentation = generator.generatePresentation(Collections.singletonList(licenseDocument), presentationMetadata);
        List<DriverAboveAgeVerifiableCredential> verifiableCredentials = presentation.getVerifiableCredential();
        PresentationProof presentationProof = verifiableCredentials.get(0).getProof();
        byte[] proofResult = ByteUtils.hexStringToByteArray(presentationProof.getProof());

        // Arrange
        LocalDateTime date = LocalDateTime.ofInstant(currentInstant, ZoneId.systemDefault());
        long currentYear = date.getYear();
        long currentMonth = date.getMonthValue();
        long currentDay = date.getDayOfMonth();
        int ageThreshold = 18;
        String challenge = "fake-differentChallenge";
        String documentId = "fake-id";

        VerifyAgePublicInput verifyPublicInput = new VerifyAgePublicInput(
                proofResult, currentYear, currentMonth, currentDay, ageThreshold, ByteUtils.bytesToHex(holderPublicKey.serializePublicKey()),
                ByteUtils.bytesToHex(authorityPublicKey.serializePublicKey()), challenge, documentId, verificationKeyPath
        );

        // Act
        boolean verifyResult = verifierProvider.verifyProof(verifyPublicInput);

        // Assert
        assertFalse(verifyResult);
    }

    @Test
    public void testCreateProofFullFlow_CorrectGenerationButVerifyAgeThresholdIsDifferent() throws VerifiablePresentationGenerationException, ZeroKnowledgeVerifyProviderException, ZkSignatureException, InitializationException, AgeCircuitProofException {
        // Arrange
        // Creation of keys
        SchnorrKeyPair holderKeyPair = SchnorrKeyPair.generate();
        SchnorrPublicKey holderPublicKey = holderKeyPair.getPublicKey();
        SchnorrSecretKey holderSecretKey = holderKeyPair.getSecretKey();
        String holderSecretKeyHex = ByteUtils.bytesToHex(holderSecretKey.serializeSecretKey());
        String holderPublicKeyHex = ByteUtils.bytesToHex(holderPublicKey.serializePublicKey());

        SchnorrKeyPair authorityKeyPair = SchnorrKeyPair.generate();
        SchnorrPublicKey authorityPublicKey = authorityKeyPair.getPublicKey();
        SchnorrSecretKey authoritySecretKey = authorityKeyPair.getSecretKey();
        String authorityPublicKeyHex = ByteUtils.bytesToHex(authorityPublicKey.serializePublicKey());

        DrivingLicenseZeroKnowledgeVcMarshaller presenter = new DrivingLicenseZeroKnowledgeVcMarshaller();

        // VC document creation
        DrivingLicenseZeroKnowledgeDocument licenseDocument = new DrivingLicenseZeroKnowledgeDocument();
        licenseDocument.setId("fake-id");
        licenseDocument.setIssuer(new Issuer(ByteUtils.bytesToHex(authorityPublicKey.serializePublicKey())));
        Instant currentInstant = Instant.now();
        licenseDocument.setIssuanceDate(currentInstant);
        DrivingLicense drivingLicense = new DrivingLicense(ByteUtils.bytesToHex(holderPublicKey.serializePublicKey()), "fake-firstName", "fake-lastName",
                new ArrayList<>(), new BirthDate(3, 12, 1991));

        licenseDocument.addCredentialSubject(drivingLicense);
        CredentialSchema schema = new CredentialSchema(
                "http://localhost:5050/driving-license-schema.json",
                DrivingLicenseDocument.CREDENTIAL_SCHEMA_TYPE
        );

        licenseDocument.setCredentialSchema(schema);

        Ed25519CredentialProof proof = new Ed25519CredentialProof(ByteUtils.bytesToHex(authorityPublicKey.serializePublicKey()));
        proof.sign(PrivateKey.generate(), presenter.fromDocumentToString(licenseDocument));
        licenseDocument.setProof(proof);

        ZkSignature<DrivingLicense> zkSignature = new ZkSignature<>(
                new MerkleTreeFactoryImpl()
        );
        zkSignature.sign(authoritySecretKey.serializeSecretKey(), licenseDocument);
        licenseDocument.setZeroKnowledgeSignature(zkSignature);

        Map<String, Object> presentationMetadata = new HashMap<>();
        presentationMetadata.put("ageThreshold", 18);
        presentationMetadata.put("challenge", "fake-challenge");
        presentationMetadata.put("secretKey", holderSecretKeyHex);
        presentationMetadata.put("dayLabel", "day");
        presentationMetadata.put("monthLabel", "month");
        presentationMetadata.put("yearLabel", "year");
        presentationMetadata.put("holderPublicKey", holderPublicKeyHex);
        presentationMetadata.put("authorityPublicKey", authorityPublicKeyHex);

        DrivingLicenseVpGenerator generator = new DrivingLicenseVpGenerator(ageProverProvider);

        // Create the temporary keys for the test
        ProvingSystemUtils.generateDLogKeys();
        AgeCircuitProof.setup(provingKeyPath, verificationKeyPath);

        // Act
        DriverAboveAgePresentation presentation = generator.generatePresentation(Collections.singletonList(licenseDocument), presentationMetadata);
        List<DriverAboveAgeVerifiableCredential> verifiableCredentials = presentation.getVerifiableCredential();
        PresentationProof presentationProof = verifiableCredentials.get(0).getProof();
        byte[] proofResult = ByteUtils.hexStringToByteArray(presentationProof.getProof());

        // Arrange
        LocalDateTime date = LocalDateTime.ofInstant(currentInstant, ZoneId.systemDefault());
        long currentYear = date.getYear();
        long currentMonth = date.getMonthValue();
        long currentDay = date.getDayOfMonth();
        int ageThreshold = 22;
        String challenge = "fake-challenge";
        String documentId = "fake-id";

        VerifyAgePublicInput verifyPublicInput = new VerifyAgePublicInput(
                proofResult, currentYear, currentMonth, currentDay, ageThreshold, ByteUtils.bytesToHex(holderPublicKey.serializePublicKey()),
                ByteUtils.bytesToHex(authorityPublicKey.serializePublicKey()), challenge, documentId, verificationKeyPath
        );

        // Act
        boolean verifyResult = verifierProvider.verifyProof(verifyPublicInput);

        // Assert
        assertFalse(verifyResult);
    }
}