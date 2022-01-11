package com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.model;

import com.hedera.hashgraph.zeroknowledge.proof.model.ZeroKnowledgeVerifyPublicInput;

public final class VerifyAgePublicInput implements ZeroKnowledgeVerifyPublicInput {
    private final byte[] proof;
    private final long currentYear;
    private final long currentMonth;
    private final long currentDay;
    private final int ageThreshold;
    private final String holderPublicKey;
    private final String authorityPublicKey;
    private final String challenge;
    private final String documentId;
    private final String verificationKeyPath;

    public VerifyAgePublicInput(byte[] proof, long currentYear, long currentMonth, long currentDay, int ageThreshold, String holderPublicKey, String authorityPublicKey, String challenge, String documentId, String verificationKeyPath) {
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

    public long getCurrentYear() {
        return currentYear;
    }

    public long getCurrentMonth() {
        return currentMonth;
    }

    public long getCurrentDay() {
        return currentDay;
    }

    public int getAgeThreshold() {
        return ageThreshold;
    }

    public String getHolderPublicKey() {
        return holderPublicKey;
    }

    public String getAuthorityPublicKey() {
        return authorityPublicKey;
    }

    public String getChallenge() {
        return challenge;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getVerificationKeyPath() {
        return verificationKeyPath;
    }
}
