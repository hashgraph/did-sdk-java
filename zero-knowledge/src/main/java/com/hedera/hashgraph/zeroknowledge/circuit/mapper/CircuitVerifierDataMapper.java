package com.hedera.hashgraph.zeroknowledge.circuit.mapper;

import com.hedera.hashgraph.zeroknowledge.circuit.model.CircuitVerifyPublicInput;
import com.hedera.hashgraph.zeroknowledge.proof.model.ZeroKnowledgeVerifyPublicInput;
import com.hedera.hashgraph.zeroknowledge.exception.CircuitPublicInputMapperException;

/**
 * An interface used to convert a general zero knowledge public input entity into the public input used by the circuit to verify the proof.
 *
 * @param <V> The zero knowledge proof public input type.
 * @param <C> The circuit public input type.
 */
public interface CircuitVerifierDataMapper<V extends ZeroKnowledgeVerifyPublicInput, C extends CircuitVerifyPublicInput> {
    C fromPublicInputVerifyToCircuitInputVerify(V publicInput) throws CircuitPublicInputMapperException;
}
