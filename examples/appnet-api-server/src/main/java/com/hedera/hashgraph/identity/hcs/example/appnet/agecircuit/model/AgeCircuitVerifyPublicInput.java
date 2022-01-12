package com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.model;

import com.hedera.hashgraph.zeroknowledge.circuit.model.CircuitVerifyPublicInput;
import io.horizen.common.librustsidechains.FieldElement;
import io.horizen.common.schnorrnative.SchnorrPublicKey;

public final class AgeCircuitVerifyPublicInput implements CircuitVerifyPublicInput, AutoCloseable {
    private final byte[] proof;
    private final FieldElement currentYear;
    private final FieldElement currentMonth;
    private final FieldElement currentDay;
    private final FieldElement ageThreshold;
    private final SchnorrPublicKey holderPublicKey;
    private final SchnorrPublicKey authorityPublicKey;
    private final FieldElement challenge;
    private final FieldElement documentId;
    private final String verificationKeyPath;

    public AgeCircuitVerifyPublicInput(byte[] proof, FieldElement currentYear, FieldElement currentMonth, FieldElement currentDay, FieldElement ageThreshold, SchnorrPublicKey holderPublicKey, SchnorrPublicKey authorityPublicKey, FieldElement challenge, FieldElement documentId, String verificationKeyPath) {
        this.proof = proof;
        this.currentYear = currentYear;
        this.currentMonth = currentMonth;
        this.currentDay = currentDay;
        this.ageThreshold = ageThreshold;
        this.holderPublicKey = holderPublicKey;
        this.authorityPublicKey = authorityPublicKey;
        this.challenge = challenge;
        this.documentId = documentId;
        this.verificationKeyPath = verificationKeyPath;
    }

    public byte[] getProof() {
        return proof;
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

    public String getVerificationKeyPath() {
        return verificationKeyPath;
    }

    @Override
    public void close() {
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
