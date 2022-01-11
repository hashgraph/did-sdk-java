package com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.model;

import com.hedera.hashgraph.zeroknowledge.circuit.model.CircuitProofPublicInput;
import io.horizen.common.librustsidechains.FieldElement;
import io.horizen.common.merkletreenative.FieldBasedMerklePath;
import io.horizen.common.schnorrnative.SchnorrPublicKey;
import io.horizen.common.schnorrnative.SchnorrSignature;

public final class AgeCircuitProofPublicInput implements CircuitProofPublicInput, AutoCloseable {
    private final FieldElement dayValue;
    private final FieldElement monthValue;
    private final FieldElement yearValue;

    private final FieldElement dayLabel;
    private final FieldElement monthLabel;
    private final FieldElement yearLabel;

    private final FieldBasedMerklePath dayMerklePath;
    private final FieldBasedMerklePath monthMerklePath;
    private final FieldBasedMerklePath yearMerklePath;

    private final FieldElement merkleTreeRoot;
    private final SchnorrSignature signedChallenge;
    private final SchnorrSignature zkSignature;

    private final FieldElement currentYear;
    private final FieldElement currentMonth;
    private final FieldElement currentDay;
    private final FieldElement ageThreshold;

    private final SchnorrPublicKey holderPublicKey;
    private final SchnorrPublicKey authorityPublicKey;

    private final FieldElement challenge;
    private final FieldElement documentId;

    private final String provingKeyPath;

    public AgeCircuitProofPublicInput(FieldElement dayValue, FieldElement monthValue, FieldElement yearValue, FieldElement dayLabel, FieldElement monthLabel, FieldElement yearLabel, FieldBasedMerklePath dayMerklePath, FieldBasedMerklePath monthMerklePath, FieldBasedMerklePath yearMerklePath, FieldElement merkleTreeRoot, SchnorrSignature signedChallenge, SchnorrSignature zkSignature, FieldElement currentYear, FieldElement currentMonth, FieldElement currentDay, FieldElement ageThreshold, SchnorrPublicKey holderPublicKey, SchnorrPublicKey authorityPublicKey, FieldElement challenge, FieldElement documentId, String provingKeyPath) {
        this.dayValue = dayValue;
        this.monthValue = monthValue;
        this.yearValue = yearValue;
        this.dayLabel = dayLabel;
        this.monthLabel = monthLabel;
        this.yearLabel = yearLabel;
        this.dayMerklePath = dayMerklePath;
        this.monthMerklePath = monthMerklePath;
        this.yearMerklePath = yearMerklePath;
        this.merkleTreeRoot = merkleTreeRoot;
        this.signedChallenge = signedChallenge;
        this.zkSignature = zkSignature;
        this.currentYear = currentYear;
        this.currentMonth = currentMonth;
        this.currentDay = currentDay;
        this.ageThreshold = ageThreshold;
        this.holderPublicKey = holderPublicKey;
        this.authorityPublicKey = authorityPublicKey;
        this.challenge = challenge;
        this.documentId = documentId;
        this.provingKeyPath = provingKeyPath;
    }

    public FieldElement getDayValue() {
        return dayValue;
    }

    public FieldElement getMonthValue() {
        return monthValue;
    }

    public FieldElement getYearValue() {
        return yearValue;
    }

    public FieldElement getDayLabel() {
        return dayLabel;
    }

    public FieldElement getMonthLabel() {
        return monthLabel;
    }

    public FieldElement getYearLabel() {
        return yearLabel;
    }

    public FieldBasedMerklePath getDayMerklePath() {
        return dayMerklePath;
    }

    public FieldBasedMerklePath getMonthMerklePath() {
        return monthMerklePath;
    }

    public FieldBasedMerklePath getYearMerklePath() {
        return yearMerklePath;
    }

    public FieldElement getMerkleTreeRoot() {
        return merkleTreeRoot;
    }

    public SchnorrSignature getSignedChallenge() {
        return signedChallenge;
    }

    public SchnorrSignature getZkSignature() {
        return zkSignature;
    }

    public FieldElement getCurrentYear() {
        return currentYear;
    }

    public FieldElement getCurrentMonth() {
        return currentMonth;
    }

    public FieldElement getCurrentDay() {
        return currentDay;
    }

    public FieldElement getAgeThreshold() {
        return ageThreshold;
    }

    public SchnorrPublicKey getHolderPublicKey() {
        return holderPublicKey;
    }

    public SchnorrPublicKey getAuthorityPublicKey() {
        return authorityPublicKey;
    }

    public FieldElement getChallenge() {
        return challenge;
    }

    public FieldElement getDocumentId() {
        return documentId;
    }

    public String getProvingKeyPath() {
        return provingKeyPath;
    }

    @Override
    public void close() {
        dayValue.close();
        monthValue.close();
        yearValue.close();
        dayLabel.close();
        monthLabel.close();
        yearLabel.close();
        dayMerklePath.close();
        monthMerklePath.close();
        yearMerklePath.close();
        merkleTreeRoot.close();
        signedChallenge.close();
        zkSignature.close();
        currentYear.close();
        currentMonth.close();
        currentDay.close();
        ageThreshold.close();
        holderPublicKey.close();
        authorityPublicKey.close();
        challenge.close();
        documentId.close();
    }
}
