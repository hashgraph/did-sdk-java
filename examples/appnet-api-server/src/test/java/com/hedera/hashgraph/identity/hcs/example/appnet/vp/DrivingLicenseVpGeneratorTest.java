package com.hedera.hashgraph.identity.hcs.example.appnet.vp;

import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.provider.ZkSnarkAgeProverProvider;
import com.hedera.hashgraph.identity.hcs.example.appnet.dto.BirthDate;
import com.hedera.hashgraph.identity.hcs.example.appnet.vc.CredentialSchema;
import com.hedera.hashgraph.identity.hcs.example.appnet.vc.DrivingLicense;
import com.hedera.hashgraph.identity.hcs.example.appnet.vc.DrivingLicenseZeroKnowledgeDocument;
import com.hedera.hashgraph.identity.hcs.vc.Issuer;
import com.hedera.hashgraph.zeroknowledge.exception.VerifiablePresentationGenerationException;
import com.hedera.hashgraph.zeroknowledge.exception.ZeroKnowledgeProofProviderException;
import com.hedera.hashgraph.zeroknowledge.vp.proof.ZeroKnowledgeSignature;
import com.hedera.hashgraph.zeroknowledge.utils.ByteUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.threeten.bp.Instant;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DrivingLicenseVpGeneratorTest {
    @Mock
    private DrivingLicenseZeroKnowledgeDocument licenseDocument;

    @Mock
    private ZeroKnowledgeSignature<DrivingLicense> zeroKnowledgeSignature;

    @Mock
    private ZkSnarkAgeProverProvider ageProverProvider;

    @InjectMocks
    private DrivingLicenseVpGenerator generator;

    @Test
    public void drivingLicenseVpGeneratorCanGenerateCorrectlyAPresentationFromAVCDocument() throws VerifiablePresentationGenerationException, ZeroKnowledgeProofProviderException {
        // Arrange
        Map<String, Object> presentationMetadata = new HashMap<>();
        presentationMetadata.put("challenge", "challenge");
        presentationMetadata.put("ageThreshold", 18);
        presentationMetadata.put("secretKey", "fake-secretKey");
        presentationMetadata.put("dayLabel", "dayLabel");
        presentationMetadata.put("monthLabel", "monthLabel");
        presentationMetadata.put("yearLabel", "yearLabel");
        presentationMetadata.put("holderPublicKey", "fake-holderPublicKey");
        presentationMetadata.put("authorityPublicKey", "fake-authorityPublicKey");
        List<DrivingLicenseZeroKnowledgeDocument> vcDocuments = Collections.singletonList(licenseDocument);
        String id = "fake-id";
        when(licenseDocument.getId()).thenReturn(id);
        String issuerId = "fake-issuer";
        Issuer issuer = new Issuer(issuerId);
        when(licenseDocument.getIssuer()).thenReturn(issuer);
        Instant instant = Instant.now();
        when(licenseDocument.getIssuanceDate()).thenReturn(instant);
        List<String> context = Collections.singletonList("fake-context");
        when(licenseDocument.getContext()).thenReturn(context);
        CredentialSchema credentialSchema = new CredentialSchema("fake-credentialSchemaId", "fake-credentialSchemaType");
        when(licenseDocument.getCredentialSchema()).thenReturn(credentialSchema);
        DrivingLicense drivingLicense = new DrivingLicense("fake-owner", "fake-name", "fake-lastName",
                new ArrayList<>(), new BirthDate(1, 1, 1970));
        List<DrivingLicense> drivingLicenses = Collections.singletonList(drivingLicense);
        when(licenseDocument.getCredentialSubject()).thenReturn(drivingLicenses);
        when(licenseDocument.getZeroKnowledgeSignature()).thenReturn(zeroKnowledgeSignature);
        String signature = "fake-signature";
        when(zeroKnowledgeSignature.getSignature()).thenReturn(signature);
        String proof = "015ef04272db6272ef0200a3748feeb2";
        when(ageProverProvider.createProof(any())).thenReturn(ByteUtils.hexStringToByteArray(proof));

        // Act
        DriverAboveAgePresentation presentationResult = generator.generatePresentation(vcDocuments, presentationMetadata);

        // Assert
        assertNotNull(presentationResult.getId());
        assertNotNull(presentationResult.getContext());
        assertNotNull(presentationResult.getType());
        assertNotNull(presentationResult.getIssuanceDate());
        assertNotNull(presentationResult.getHolder());

        assertNotNull(presentationResult.getVerifiableCredential());
        assertEquals(presentationResult.getVerifiableCredential().size(), 1);

        List<DriverAboveAgeVerifiableCredential> verifiableCredentials = presentationResult.getVerifiableCredential();
        DriverAboveAgeVerifiableCredential verifiableCredential = verifiableCredentials.get(0);

        assertEquals(verifiableCredential.getId(), id);
        assertEquals(verifiableCredential.getIssuanceDate(), instant);
        assertEquals(verifiableCredential.getIssuer(), issuer);
        assertEquals(verifiableCredential.getCredentialSubject().size(), 1);
        assertNotNull(verifiableCredential.getCredentialSubject().get("ageOver"));
        assertEquals(verifiableCredential.getCredentialSubject().get("ageOver"), "18");
        assertNotNull(verifiableCredential.getCredentialSchema());
        assertEquals(verifiableCredential.getCredentialSchema().id, "fake-credentialSchemaId");
        assertEquals(verifiableCredential.getCredentialSchema().type, "fake-credentialSchemaType");
        assertEquals(verifiableCredential.getProof().getProof(), proof);
    }

    @Test
    public void whenPassingMoreThanOneCredentialSubjectElementToDrivingLicenseVpGenerator_throwsAnException() {
        // Arrange
        List<DrivingLicenseZeroKnowledgeDocument> vcDocuments = List.of(licenseDocument, licenseDocument);
        Map<String, Object> presentationMetadata = new HashMap<>();
        String expectedMessage = "Cannot generate a driver above-age presentation with multiple VC documents";

        // Act
        Exception exception = assertThrows(
                IllegalStateException.class,
                () -> generator.generatePresentation(vcDocuments, presentationMetadata)
        );

        // Assert
        String actualMessage = exception.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }
}