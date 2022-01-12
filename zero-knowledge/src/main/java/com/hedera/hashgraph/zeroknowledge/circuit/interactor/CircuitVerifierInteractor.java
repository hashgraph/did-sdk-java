package com.hedera.hashgraph.zeroknowledge.circuit.interactor;

import com.hedera.hashgraph.zeroknowledge.circuit.model.CircuitVerifyPublicInput;

/**
 * An interface representing the direct class interacting with a circuit to verify a proof.
 *
 * @param <T> The circuit public input type.
 */
public interface CircuitVerifierInteractor<T extends CircuitVerifyPublicInput> {
    boolean verifyProof(T circuitVerifyPublicInput);
}
