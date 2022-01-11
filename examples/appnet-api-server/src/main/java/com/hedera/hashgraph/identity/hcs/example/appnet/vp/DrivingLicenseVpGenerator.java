package com.hedera.hashgraph.identity.hcs.example.appnet.vp;

import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.provider.ZkSnarkAgeProverProvider;
import com.hedera.hashgraph.identity.hcs.example.appnet.vc.DrivingLicense;
import com.hedera.hashgraph.identity.hcs.example.appnet.vc.DrivingLicenseZeroKnowledgeDocument;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.model.ProofAgePublicInput;
import com.hedera.hashgraph.zeroknowledge.exception.VerifiablePresentationGenerationException;
import com.hedera.hashgraph.zeroknowledge.exception.VpDocumentGeneratorException;
import com.hedera.hashgraph.zeroknowledge.vp.proof.ZkSnarkPresentationProof;
import com.hedera.hashgraph.zeroknowledge.utils.ByteUtils;
import com.hedera.hashgraph.zeroknowledge.vp.VpZeroKnowledgeGenerator;
import org.jetbrains.annotations.NotNull;
import org.threeten.bp.Instant;

import java.util.List;
import java.util.Map;

public class DrivingLicenseVpGenerator extends VpZeroKnowledgeGenerator<DrivingLicenseZeroKnowledgeDocument, DriverAboveAgePresentation, ProofAgePublicInput<DrivingLicense>> {

    public DrivingLicenseVpGenerator(ZkSnarkAgeProverProvider zeroKnowledgeProofProvider) {
        super(zeroKnowledgeProofProvider);
    }

    @Override
    protected ProofAgePublicInput<DrivingLicense> getProofPublicInput(DrivingLicenseZeroKnowledgeDocument document, Map<String, Object> presentationMetadata) {
        int ageThreshold = Integer.parseInt(presentationMetadata.get("ageThreshold").toString());
        Instant vcDocumentDate = document.getIssuanceDate();

        return new ProofAgePublicInput<>(
                document.getCredentialSubject(),
                document.getZeroKnowledgeSignature(),
                presentationMetadata.get("challenge").toString(),
                presentationMetadata.get("secretKey").toString(),
                presentationMetadata.get("dayLabel").toString(),
                presentationMetadata.get("monthLabel").toString(),
                presentationMetadata.get("yearLabel").toString(),
                ageThreshold,
                presentationMetadata.get("holderPublicKey").toString(),
                presentationMetadata.get("authorityPublicKey").toString(),
                document.getId(),
                vcDocumentDate
        );
    }

    @Override
    public DriverAboveAgePresentation generatePresentation(List<DrivingLicenseZeroKnowledgeDocument> vcDocuments, Map<String, Object> presentationMetadata) throws VerifiablePresentationGenerationException {
        if (vcDocuments.size() != 1) {
            throw new IllegalStateException("Cannot generate a driver above-age presentation with multiple VC documents");
        }

        DrivingLicenseZeroKnowledgeDocument licenseDocument = vcDocuments.get(0);

        DriverAboveAgePresentation driverAboveAgePresentation = new DriverAboveAgePresentation();

        // Set the credential subjects
        setCredentialSubjects(licenseDocument, driverAboveAgePresentation);

        // Get the verifiable credentials
        DriverAboveAgeVerifiableCredential verifiableCredential = getVerifiableCredential(licenseDocument, presentationMetadata);

        // Set the proof and verifiable credential
        try {
            setProof(licenseDocument, driverAboveAgePresentation, verifiableCredential, presentationMetadata);

            return driverAboveAgePresentation;
        } catch (Exception e) {
            e.printStackTrace();

            throw new VerifiablePresentationGenerationException("Cannot generate presentation, error while computing the snark proof", e);
        }
    }

    private void setCredentialSubjects(DrivingLicenseZeroKnowledgeDocument licenseDocument, DriverAboveAgePresentation driverAboveAgePresentation) {
        List<DrivingLicense> credentialSubjects = licenseDocument.getCredentialSubject();
        DrivingLicense credentialSubject = credentialSubjects.get(0);
        driverAboveAgePresentation.setHolder(credentialSubject.getId());
    }

    @NotNull
    private DriverAboveAgeVerifiableCredential getVerifiableCredential(DrivingLicenseZeroKnowledgeDocument licenseDocument, Map<String, Object> presentationMetadata) {
        DriverAboveAgeVerifiableCredential verifiableCredential = new DriverAboveAgeVerifiableCredential();
        verifiableCredential.setContext(licenseDocument.getContext());
        verifiableCredential.setId(licenseDocument.getId());
        verifiableCredential.setType(licenseDocument.getType());
        verifiableCredential.setCredentialSchema(licenseDocument.getCredentialSchema());
        verifiableCredential.setIssuer(licenseDocument.getIssuer());
        verifiableCredential.setIssuanceDate(licenseDocument.getIssuanceDate());
        verifiableCredential.addCredentialSubjectClaim("ageOver", presentationMetadata.get("ageThreshold").toString());
        return verifiableCredential;
    }

    private void setProof(
            DrivingLicenseZeroKnowledgeDocument licenseDocument,
            DriverAboveAgePresentation driverAboveAgePresentation,
            DriverAboveAgeVerifiableCredential verifiableCredential,
            Map<String, Object> presentationMetadata
    ) throws VpDocumentGeneratorException {
        ZkSnarkPresentationProof proof = new ZkSnarkPresentationProof();
        String signature = licenseDocument.getZeroKnowledgeSignature().getSignature();
        proof.setZkSignature(signature);

        byte[] snarkProof = generateZeroKnowledgeProof(licenseDocument, presentationMetadata);
        proof.setSnarkProof(ByteUtils.bytesToHex(snarkProof));

        verifiableCredential.setProof(proof);

        driverAboveAgePresentation.addVerifiableCredential(verifiableCredential);
    }
}
