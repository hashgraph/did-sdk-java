package com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.provider;

import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.interactor.AgeCircuitVerifierInteractor;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.mapper.AgeCircuitVerifierDataMapper;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.model.AgeCircuitVerifyPublicInput;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.model.VerifyAgePublicInput;
import com.hedera.hashgraph.zeroknowledge.proof.ZeroKnowledgeVerifierProvider;
import com.hedera.hashgraph.zeroknowledge.exception.CircuitPublicInputMapperException;
import com.hedera.hashgraph.zeroknowledge.exception.ZeroKnowledgeVerifyProviderException;

public class ZkSnarkAgeVerifierProvider implements ZeroKnowledgeVerifierProvider<VerifyAgePublicInput> {
    private final AgeCircuitVerifierInteractor verifierInteractor;
    private final AgeCircuitVerifierDataMapper dataMapper;

    public ZkSnarkAgeVerifierProvider(AgeCircuitVerifierInteractor verifierInteractor, AgeCircuitVerifierDataMapper dataMapper) {
        this.verifierInteractor = verifierInteractor;
        this.dataMapper = dataMapper;
    }

    @Override
    public boolean verifyProof(VerifyAgePublicInput publicInput) throws ZeroKnowledgeVerifyProviderException {
        try (AgeCircuitVerifyPublicInput circuitInput = dataMapper.fromPublicInputVerifyToCircuitInputVerify(publicInput)) {
            return verifierInteractor.verifyProof(circuitInput);
        } catch (CircuitPublicInputMapperException e) {
            throw new ZeroKnowledgeVerifyProviderException(
                    String.format("Cannot verify proof, error while verifying proof with public input %s", publicInput),
                    e
            );
        }
    }
}
