package com.hedera.hashgraph.zeroknowledge.vp.proof;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * A concrete class representing a zero knowledge proof of a verifiable credential inside a verifiable presentation document.
 * It uses a snark proof.
 */
public class ZkSnarkPresentationProof implements PresentationProof {
    private static final String TYPE = "ZkSnarkProof";

    @Expose
    @SerializedName(ZkSnarkProofJsonProperties.TYPE)
    private String type;

    @Expose
    @SerializedName(ZkSnarkProofJsonProperties.ZK_SIGNATURE)
    private String zkSignature;

    @Expose
    @SerializedName(ZkSnarkProofJsonProperties.SNARK_PROOF)
    private String snarkProof;

    public ZkSnarkPresentationProof() {
        this(null, null);
    }

    public ZkSnarkPresentationProof(String zkSignature, String snarkProof) {
        this.type = TYPE;
        this.zkSignature = zkSignature;
        this.snarkProof = snarkProof;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getSignature() {
        return zkSignature;
    }

    @Override
    public String getProof() {
        return snarkProof;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSnarkProof(String snarkProof) {
        this.snarkProof = snarkProof;
    }

    public void setZkSignature(String zkSignature) {
        this.zkSignature = zkSignature;
    }
}
