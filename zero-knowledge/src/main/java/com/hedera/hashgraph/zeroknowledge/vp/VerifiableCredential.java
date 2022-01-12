package com.hedera.hashgraph.zeroknowledge.vp;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.hedera.hashgraph.zeroknowledge.vp.proof.PresentationProof;
import com.hedera.hashgraph.zeroknowledge.vp.proof.ZkSnarkProofJsonProperties;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class VerifiableCredential extends VerifiableCredentialBase {
    @Expose
    @SerializedName(VerifiableCredentialJsonProperties.PROOF)
    protected PresentationProof proof;

    @Override
    public Map<String, Object> getCustomHashableFieldsHook() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(ZkSnarkProofJsonProperties.ZK_SIGNATURE, proof.getSignature());
        return map;
    }

    public PresentationProof getProof() {
        return proof;
    }

    public void setProof(PresentationProof proof) {
        this.proof = proof;
    }
}
