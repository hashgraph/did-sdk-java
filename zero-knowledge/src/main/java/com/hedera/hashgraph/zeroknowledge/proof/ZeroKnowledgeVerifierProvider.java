package com.hedera.hashgraph.zeroknowledge.proof;

import com.hedera.hashgraph.zeroknowledge.proof.model.ZeroKnowledgeVerifyPublicInput;
import com.hedera.hashgraph.zeroknowledge.exception.ZeroKnowledgeVerifyProviderException;

/**
 * An interface for the zero knowledge proof provider for the verifier role.
 *
 * @param <V> The input data type used to verify the zero knowledge proof.
 */
public interface ZeroKnowledgeVerifierProvider<V extends ZeroKnowledgeVerifyPublicInput> {
    boolean verifyProof(V publicInput) throws ZeroKnowledgeVerifyProviderException;
}
