package com.hedera.hashgraph.zeroknowledge.merkletree;

import io.horizen.common.librustsidechains.*;
import io.horizen.common.poseidonnative.PoseidonHash;

import java.nio.charset.StandardCharsets;

import static com.hedera.hashgraph.zeroknowledge.utils.MerkleTreeUtils.getFieldElementByAllowedTypes;

/**
 * A class to handle the merkle tree credential subjects leaves, composed of the hash of the label and value.
 */
public class CredentialSubjectMerkleTreeLeaf implements FieldElementConvertible {
    private final String propertyLabel;
    private final Object propertyValue;

    public CredentialSubjectMerkleTreeLeaf(String propertyLabel, Object propertyValue) {
        this.propertyLabel = propertyLabel;
        this.propertyValue = propertyValue;
    }

    @Override
    public FieldElement toFieldElement() throws FieldElementConversionException {
        try (
                PoseidonHash hash = PoseidonHash.getInstanceConstantLength(2);
                FieldElement keyField = FieldElement.deserialize(propertyLabel.getBytes(StandardCharsets.UTF_8));
                FieldElement valueFields = getFieldElementByAllowedTypes(propertyValue)
        ) {
            hash.update(keyField);
            hash.update(valueFields);

            return hash.finalizeHash();
        } catch (DeserializationException | IllegalArgumentException e) {
            throw new FieldElementConversionException(
                    String.format("Cannot deserialize label or value to field element. Label: '%s', value: '%s'", propertyLabel, propertyValue),
                    e
            );
        } catch (FinalizationException e) {
            throw new FieldElementConversionException("Cannot finalize merkle leaf hash");
        }
    }
}
