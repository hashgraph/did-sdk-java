package com.hedera.hashgraph.zeroknowledge.vp.proof;

/**
 * An interface to define the verifiable presentation proof.
 */
public interface PresentationProof {
    /**
     * A presentation proof must define its type name.
     *
     * @return The presentation proof's type name.
     */
    String getType();

    /**
     * A presentation proof must define a signature.
     *
     * @return The presentation proof's signature.
     */
    String getSignature();

    /**
     * A presentation proof must define a zero knowledge proof.
     * @return The presentation proof's proof.
     */
    String getProof();
}
