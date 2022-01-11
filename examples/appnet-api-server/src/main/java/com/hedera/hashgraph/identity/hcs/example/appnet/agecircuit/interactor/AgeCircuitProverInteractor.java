package com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.interactor;

import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.model.AgeCircuitProofPublicInput;
import com.hedera.hashgraph.zeroknowledge.circuit.interactor.CircuitProverInteractor;
import io.horizen.common.librustsidechains.InitializationException;
import io.horizenlabs.agecircuit.AgeCircuitProof;
import io.horizenlabs.agecircuit.AgeCircuitProofException;
import io.horizenlabs.provingsystemnative.ProvingSystem;

public class AgeCircuitProverInteractor implements CircuitProverInteractor<AgeCircuitProofPublicInput> {
    @Override
    public byte[] generateProof(AgeCircuitProofPublicInput publicInput) {
        try {
            ProvingSystem.generateDLogKeys(1 << 17, 1 << 15);

            return AgeCircuitProof.createProof(
                    publicInput.getDayValue(), publicInput.getMonthValue(), publicInput.getYearValue(),
                    publicInput.getDayLabel(), publicInput.getMonthLabel(), publicInput.getYearLabel(),
                    publicInput.getDayMerklePath(), publicInput.getMonthMerklePath(), publicInput.getYearMerklePath(),
                    publicInput.getMerkleTreeRoot(), publicInput.getSignedChallenge(), publicInput.getZkSignature(),
                    publicInput.getCurrentYear(), publicInput.getCurrentMonth(), publicInput.getCurrentDay(),
                    publicInput.getAgeThreshold(), publicInput.getHolderPublicKey(), publicInput.getAuthorityPublicKey(),
                    publicInput.getChallenge(), publicInput.getDocumentId(), publicInput.getProvingKeyPath()
            );
        } catch (InitializationException e) {
            throw new IllegalStateException("", e);
        } catch (AgeCircuitProofException e) {
            throw new IllegalArgumentException("", e);
        }
    }
}
