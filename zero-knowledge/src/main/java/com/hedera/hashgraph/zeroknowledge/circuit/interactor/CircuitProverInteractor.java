package com.hedera.hashgraph.zeroknowledge.circuit.interactor;

import com.hedera.hashgraph.zeroknowledge.circuit.model.CircuitProofPublicInput;
import io.horizen.common.librustsidechains.InitializationException;

/**
 * An interface representing the direct class interacting with a circuit to generate a proof.
 *
 * @param <T> The circuit public input type.
 */
public interface CircuitProverInteractor<T extends CircuitProofPublicInput> {
    byte[] generateProof(T circuitProofPublicInput);
}
