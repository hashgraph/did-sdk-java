package com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.mapper;

import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.model.AgeCircuitVerifyPublicInput;
import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.model.VerifyAgePublicInput;
import com.hedera.hashgraph.zeroknowledge.circuit.mapper.CircuitVerifierDataMapper;
import com.hedera.hashgraph.zeroknowledge.exception.CircuitPublicInputMapperException;
import com.hedera.hashgraph.zeroknowledge.utils.ByteUtils;
import com.hedera.hashgraph.zeroknowledge.utils.HashUtils;
import io.horizen.common.librustsidechains.DeserializationException;
import io.horizen.common.librustsidechains.FieldElement;
import io.horizen.common.librustsidechains.FinalizationException;
import io.horizen.common.schnorrnative.SchnorrPublicKey;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class AgeCircuitVerifierDataMapper implements CircuitVerifierDataMapper<VerifyAgePublicInput, AgeCircuitVerifyPublicInput> {
    @Override
    public AgeCircuitVerifyPublicInput fromPublicInputVerifyToCircuitInputVerify(VerifyAgePublicInput publicInput) throws CircuitPublicInputMapperException {
        try {
            FieldElement currentYear = FieldElement.createFromLong(publicInput.getCurrentYear());
            FieldElement currentMonth = FieldElement.createFromLong(publicInput.getCurrentMonth());
            FieldElement currentDay = FieldElement.createFromLong(publicInput.getCurrentDay());
            FieldElement ageThreshold = FieldElement.createFromLong(publicInput.getAgeThreshold());

            List<FieldElement> challengeList = FieldElement.deserializeMany(publicInput.getChallenge().getBytes(StandardCharsets.UTF_8));
            FieldElement challenge = HashUtils.hashFieldElementList(challengeList);

            List<FieldElement> documentIds = FieldElement.deserializeMany(publicInput.getDocumentId().getBytes(StandardCharsets.UTF_8));
            FieldElement documentId = HashUtils.hashFieldElementList(documentIds);

            SchnorrPublicKey holderPublicKey = SchnorrPublicKey.deserialize(ByteUtils.hexStringToByteArray(publicInput.getHolderPublicKey()));
            SchnorrPublicKey authorityPublicKey = SchnorrPublicKey.deserialize(ByteUtils.hexStringToByteArray(publicInput.getAuthorityPublicKey()));

            return new AgeCircuitVerifyPublicInput(
                    publicInput.getProof(), currentYear, currentMonth, currentDay,
                    ageThreshold, holderPublicKey, authorityPublicKey, challenge, documentId,
                    publicInput.getVerificationKeyPath()
            );
        } catch (DeserializationException | FinalizationException e) {
            throw new CircuitPublicInputMapperException(
                    String.format("Cannot map public input to verify circuit input. Public input: %s", publicInput),
                    e
            );
        }
    }
}
