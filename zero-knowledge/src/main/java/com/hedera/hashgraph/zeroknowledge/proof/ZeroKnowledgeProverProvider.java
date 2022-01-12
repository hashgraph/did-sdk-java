package com.hedera.hashgraph.zeroknowledge.proof;

import com.hedera.hashgraph.zeroknowledge.proof.model.ZeroKnowledgeProofPublicInput;
import com.hedera.hashgraph.zeroknowledge.exception.ZeroKnowledgeProofProviderException;

/**
 * An interface for the zero knowledge proof provider for the prover role.
 *
 * @param <P> The input data type used to generate the zero knowledge proof.
 */
public interface ZeroKnowledgeProverProvider<P extends ZeroKnowledgeProofPublicInput> {
    byte[] createProof(P publicInput) throws ZeroKnowledgeProofProviderException;
}
