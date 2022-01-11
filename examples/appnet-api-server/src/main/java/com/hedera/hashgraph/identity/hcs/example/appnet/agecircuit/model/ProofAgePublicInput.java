package com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.model;

import com.hedera.hashgraph.identity.hcs.vc.CredentialSubject;
import com.hedera.hashgraph.zeroknowledge.proof.model.ZeroKnowledgeProofPublicInput;
import com.hedera.hashgraph.zeroknowledge.vp.proof.ZeroKnowledgeSignature;
import org.threeten.bp.Instant;

import java.util.List;

public final class ProofAgePublicInput<T extends CredentialSubject> implements ZeroKnowledgeProofPublicInput {
    private final List<T> credentialSubject;
    private final ZeroKnowledgeSignature<T> zeroKnowledgeSignature;
    private final String challenge;
    private final String secretKey;
    private final String dayLabel;
    private final String monthLabel;
    private final String yearLabel;
    private final int ageThreshold;
    private final String holderPublicKey;
    private final String authorityPublicKey;
    private final String documentId;
    private final Instant vcDocumentDate;

    public ProofAgePublicInput(List<T> credentialSubject, ZeroKnowledgeSignature<T> zeroKnowledgeSignature, String challenge, String secretKey, String dayLabel, String monthLabel, String yearLabel, int ageThreshold, String holderPublicKey, String authorityPublicKey, String documentId, Instant vcDocumentDate) {
        this.credentialSubject = credentialSubject;
        this.zeroKnowledgeSignature = zeroKnowledgeSignature;
        this.challenge = challenge;
        this.secretKey = secretKey;
        this.dayLabel = dayLabel;
        this.monthLabel = monthLabel;
        this.yearLabel = yearLabel;
        this.ageThreshold = ageThreshold;
        this.holderPublicKey = holderPublicKey;
        this.authorityPublicKey = authorityPublicKey;
        this.documentId = documentId;
        this.vcDocumentDate = vcDocumentDate;
    }

    public List<T> getCredentialSubject() {
        return credentialSubject;
    }

    public ZeroKnowledgeSignature<T> getZeroKnowledgeSignature() {
        return zeroKnowledgeSignature;
    }

    public String getChallenge() {
        return challenge;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getDayLabel() {
        return dayLabel;
    }

    public String getMonthLabel() {
        return monthLabel;
    }

    public String getYearLabel() {
        return yearLabel;
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

    public String getDocumentId() {
        return documentId;
    }

    public Instant getVcDocumentDate() {
        return vcDocumentDate;
    }
}
