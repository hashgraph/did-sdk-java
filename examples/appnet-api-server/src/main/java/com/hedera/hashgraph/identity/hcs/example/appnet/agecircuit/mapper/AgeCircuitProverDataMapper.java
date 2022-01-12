package com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.mapper;

import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.model.AgeCircuitProofPublicInput;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.model.ProofAgePublicInput;
import com.hedera.hashgraph.identity.hcs.example.appnet.vc.DrivingLicense;
import com.hedera.hashgraph.zeroknowledge.circuit.mapper.CircuitProverDataMapper;
import com.hedera.hashgraph.zeroknowledge.exception.CircuitPublicInputMapperException;
import com.hedera.hashgraph.zeroknowledge.merkletree.CredentialSubjectMerkleTreeLeaf;
import com.hedera.hashgraph.zeroknowledge.merkletree.factory.MerkleTreeFactory;
import com.hedera.hashgraph.zeroknowledge.utils.ByteUtils;
import com.hedera.hashgraph.zeroknowledge.utils.HashUtils;
import com.hedera.hashgraph.zeroknowledge.vp.proof.ZeroKnowledgeSignature;
import io.github.cdimascio.dotenv.Dotenv;
import io.horizen.common.librustsidechains.FieldElement;
import io.horizen.common.merkletreenative.BaseMerkleTree;
import io.horizen.common.merkletreenative.FieldBasedMerklePath;
import io.horizen.common.poseidonnative.PoseidonHash;
import io.horizen.common.schnorrnative.SchnorrKeyPair;
import io.horizen.common.schnorrnative.SchnorrPublicKey;
import io.horizen.common.schnorrnative.SchnorrSecretKey;
import io.horizen.common.schnorrnative.SchnorrSignature;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class AgeCircuitProverDataMapper implements CircuitProverDataMapper<ProofAgePublicInput<DrivingLicense>, AgeCircuitProofPublicInput> {
    private final MerkleTreeFactory merkleTreeFactory;

    public AgeCircuitProverDataMapper(MerkleTreeFactory merkleTreeFactory) {
        this.merkleTreeFactory = merkleTreeFactory;
    }

    @Override
    public AgeCircuitProofPublicInput fromPublicInputProofToCircuitInputProof(ProofAgePublicInput<DrivingLicense> proofAgePublicInput) throws CircuitPublicInputMapperException {
        Dotenv dotenv = Dotenv.configure().load();

        List<DrivingLicense> credentialSubjects = proofAgePublicInput.getCredentialSubject();
        DrivingLicense credentialSubject = credentialSubjects.get(0);
        ZeroKnowledgeSignature<DrivingLicense> zeroKnowledgeSignature = proofAgePublicInput.getZeroKnowledgeSignature();

        try {
            String dayLabelString = proofAgePublicInput.getDayLabel();
            String monthLabelString = proofAgePublicInput.getMonthLabel();
            String yearLabelString = proofAgePublicInput.getYearLabel();

            CredentialSubjectMerkleTreeLeaf dayLeaf = new CredentialSubjectMerkleTreeLeaf(dayLabelString, credentialSubject.getBirthDateDay());
            CredentialSubjectMerkleTreeLeaf monthLeaf = new CredentialSubjectMerkleTreeLeaf(monthLabelString, credentialSubject.getBirthDateMonth());
            CredentialSubjectMerkleTreeLeaf yearLeaf = new CredentialSubjectMerkleTreeLeaf(yearLabelString, credentialSubject.getBirthDateYear());

            FieldElement dayLabel = FieldElement.deserialize(dayLabelString.getBytes(StandardCharsets.UTF_8));
            FieldElement monthLabel = FieldElement.deserialize(monthLabelString.getBytes(StandardCharsets.UTF_8));
            FieldElement yearLabel = FieldElement.deserialize(yearLabelString.getBytes(StandardCharsets.UTF_8));

            FieldElement dayValue = FieldElement.createFromLong(credentialSubject.getBirthDateDay());
            FieldElement monthValue = FieldElement.createFromLong(credentialSubject.getBirthDateMonth());
            FieldElement yearValue = FieldElement.createFromLong(credentialSubject.getBirthDateYear());

            BaseMerkleTree merkleTree = merkleTreeFactory.getMerkleTree(credentialSubjects);

            FieldBasedMerklePath dayMerklePath = merkleTree.getMerklePath(dayLeaf.toFieldElement());
            FieldBasedMerklePath monthMerklePath = merkleTree.getMerklePath(monthLeaf.toFieldElement());
            FieldBasedMerklePath yearMerklePath = merkleTree.getMerklePath(yearLeaf.toFieldElement());

            merkleTree.finalizeTreeInPlace();
            FieldElement merkleTreeRoot = merkleTree.root();

            byte[] zkSignatureBytes = ByteUtils.hexStringToByteArray(zeroKnowledgeSignature.getSignature());
            SchnorrSignature zkSignature = SchnorrSignature.deserialize(zkSignatureBytes);

            List<FieldElement> challengeList = FieldElement.deserializeMany(proofAgePublicInput.getChallenge().getBytes(StandardCharsets.UTF_8));
            FieldElement challenge = HashUtils.hashFieldElementList(challengeList);
            PoseidonHash hashChallenge = PoseidonHash.getInstanceConstantLength(1);
            hashChallenge.update(challenge);
            FieldElement challengeHashed = hashChallenge.finalizeHash();

            SchnorrSecretKey secretKey = SchnorrSecretKey.deserialize(ByteUtils.hexStringToByteArray(proofAgePublicInput.getSecretKey()));
            SchnorrKeyPair keyPair = new SchnorrKeyPair(
                    secretKey,
                    secretKey.getPublicKey()
            );
            SchnorrSignature signedChallenge = keyPair.signMessage(challengeHashed);

            Instant documentDate = proofAgePublicInput.getVcDocumentDate();
            LocalDateTime date = LocalDateTime.ofInstant(documentDate, ZoneId.systemDefault());
            FieldElement currentYear = FieldElement.createFromLong(date.getYear());
            FieldElement currentMonth = FieldElement.createFromLong(date.getMonthValue());
            FieldElement currentDay = FieldElement.createFromLong(date.getDayOfMonth());
            FieldElement ageThreshold = FieldElement.createFromLong(proofAgePublicInput.getAgeThreshold());

            String holderPublicKeyValue = proofAgePublicInput.getHolderPublicKey();
            String authorityPublicKeyValue = proofAgePublicInput.getAuthorityPublicKey();

            SchnorrPublicKey holderPublicKey = SchnorrPublicKey.deserialize(ByteUtils.hexStringToByteArray(holderPublicKeyValue));
            SchnorrPublicKey authorityPublicKey = SchnorrPublicKey.deserialize(ByteUtils.hexStringToByteArray(authorityPublicKeyValue));

            List<FieldElement> documentIds = FieldElement.deserializeMany(proofAgePublicInput.getDocumentId().getBytes(StandardCharsets.UTF_8));
            FieldElement documentId = HashUtils.hashFieldElementList(documentIds);

            String provingKeyPath = dotenv.get("PROVING_KEY_PATH");

            AgeCircuitProofPublicInput proofPublicInput = new AgeCircuitProofPublicInput(
                    dayValue, monthValue, yearValue, dayLabel, monthLabel, yearLabel,
                    dayMerklePath, monthMerklePath, yearMerklePath, merkleTreeRoot,
                    signedChallenge, zkSignature, currentYear, currentMonth, currentDay, ageThreshold,
                    holderPublicKey, authorityPublicKey, challenge, documentId, provingKeyPath
            );

            manuallyDeallocateMemory(hashChallenge, challengeHashed, secretKey, keyPair);

            return proofPublicInput;
        } catch (Exception e) {
            throw new CircuitPublicInputMapperException(
                    String.format("Cannot map public input to proof circuit input. Public input: %s", proofAgePublicInput),
                    e
            );
        }
    }

    private void manuallyDeallocateMemory(PoseidonHash hashChallenge, FieldElement challengeHashed, SchnorrSecretKey secretKey, SchnorrKeyPair keyPair) {
        secretKey.close();
        keyPair.close();
        hashChallenge.close();
        challengeHashed.close();
    }
}
