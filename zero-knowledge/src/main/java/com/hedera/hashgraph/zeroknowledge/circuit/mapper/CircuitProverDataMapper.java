package com.hedera.hashgraph.zeroknowledge.circuit.mapper;

import com.hedera.hashgraph.zeroknowledge.circuit.model.CircuitProofPublicInput;
import com.hedera.hashgraph.zeroknowledge.proof.model.ZeroKnowledgeProofPublicInput;
import com.hedera.hashgraph.zeroknowledge.exception.CircuitPublicInputMapperException;

/**
 * An interface used to convert a general zero knowledge public input entity into the public input used by the circuit to generate the proof.
 *
 * @param <P> The zero knowledge proof public input type.
 * @param <C> The circuit public input type.
 */
public interface CircuitProverDataMapper<P extends ZeroKnowledgeProofPublicInput, C extends CircuitProofPublicInput> {
    C fromPublicInputProofToCircuitInputProof(P publicInput) throws CircuitPublicInputMapperException;
}
