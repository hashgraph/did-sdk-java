package com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.interactor;

import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.model.AgeCircuitVerifyPublicInput;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.utils.ProvingSystemUtils;
import com.hedera.hashgraph.zeroknowledge.circuit.interactor.CircuitVerifierInteractor;
import io.horizen.common.librustsidechains.InitializationException;
import io.horizenlabs.agecircuit.AgeCircuitProof;
import io.horizenlabs.agecircuit.AgeCircuitProofException;
import io.horizenlabs.provingsystemnative.ProvingSystem;

public class AgeCircuitVerifierInteractor implements CircuitVerifierInteractor<AgeCircuitVerifyPublicInput> {
    @Override
    public boolean verifyProof(AgeCircuitVerifyPublicInput publicInput) {
        try {
            ProvingSystemUtils.generateDLogKeys();

            return AgeCircuitProof.verifyProof(
                    publicInput.getProof(), publicInput.getCurrentYear(), publicInput.getCurrentMonth(), publicInput.getCurrentDay(),
                    publicInput.getAgeThreshold(), publicInput.getHolderPublicKey(), publicInput.getAuthorityPublicKey(),
                    publicInput.getChallenge(), publicInput.getDocumentId(), publicInput.getVerificationKeyPath()
            );
        } catch (InitializationException e) {
            throw new IllegalStateException("Cannot initialize age circuit", e);
        } catch (AgeCircuitProofException e) {
            throw new IllegalArgumentException("Cannot verify proof using age circuit", e);
        }
    }
}
