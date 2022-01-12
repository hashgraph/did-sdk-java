package com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.provider;

import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.interactor.AgeCircuitProverInteractor;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.mapper.AgeCircuitProverDataMapper;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.model.AgeCircuitProofPublicInput;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.model.ProofAgePublicInput;
import com.hedera.hashgraph.identity.hcs.example.appnet.vc.DrivingLicense;
import com.hedera.hashgraph.zeroknowledge.proof.ZeroKnowledgeProverProvider;
import com.hedera.hashgraph.zeroknowledge.exception.CircuitPublicInputMapperException;
import com.hedera.hashgraph.zeroknowledge.exception.ZeroKnowledgeProofProviderException;

public class ZkSnarkAgeProverProvider implements ZeroKnowledgeProverProvider<ProofAgePublicInput<DrivingLicense>> {
    private final AgeCircuitProverInteractor circuitInteractor;
    private final AgeCircuitProverDataMapper circuitProverDataMapper;

    public ZkSnarkAgeProverProvider(AgeCircuitProverInteractor circuitInteractor, AgeCircuitProverDataMapper circuitProverDataMapper) {
        this.circuitInteractor = circuitInteractor;
        this.circuitProverDataMapper = circuitProverDataMapper;
    }

    @Override
    public byte[] createProof(ProofAgePublicInput<DrivingLicense> publicInput) throws ZeroKnowledgeProofProviderException {
        try (AgeCircuitProofPublicInput circuitInputProof = circuitProverDataMapper.fromPublicInputProofToCircuitInputProof(publicInput)) {
            return circuitInteractor.generateProof(circuitInputProof);
        } catch (CircuitPublicInputMapperException e) {
            throw new ZeroKnowledgeProofProviderException(
                    String.format("Cannot create proof, error while creating proof with public input %s", publicInput),
                    e
            );
        }
    }
}
