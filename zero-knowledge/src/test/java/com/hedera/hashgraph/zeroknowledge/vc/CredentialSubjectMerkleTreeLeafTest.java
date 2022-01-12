package com.hedera.hashgraph.zeroknowledge.vc;

import com.hedera.hashgraph.zeroknowledge.merkletree.CredentialSubjectMerkleTreeLeaf;
import io.horizen.common.librustsidechains.DeserializationException;
import io.horizen.common.librustsidechains.FieldElement;
import io.horizen.common.librustsidechains.FieldElementConversionException;
import io.horizen.common.librustsidechains.FinalizationException;
import io.horizen.common.poseidonnative.PoseidonHash;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CredentialSubjectMerkleTreeLeafTest {
    /*
     * This test makes sure that the hash computation procedure is not altered by mistake
     * */
    @Test
    public void merkleTreeLeafHashIsCorrectlyComputed() throws DeserializationException, FieldElementConversionException, FinalizationException {
        // Arrange
        String key = "key";
        String value = "value";

        CredentialSubjectMerkleTreeLeaf merkleTreeLeaf = new CredentialSubjectMerkleTreeLeaf(key, value);

        FieldElement keyFieldElement = FieldElement.deserialize(key.getBytes(StandardCharsets.UTF_8));
        FieldElement valueFieldElement = FieldElement.deserialize(value.getBytes(StandardCharsets.UTF_8));

        PoseidonHash hash = PoseidonHash.getInstanceConstantLength(2);
        hash.update(keyFieldElement);
        hash.update(valueFieldElement);

        // Act
        FieldElement merkleTreeLeafHash = merkleTreeLeaf.toFieldElement();
        FieldElement computedHash = hash.finalizeHash();

        // Assert
        assertEquals(merkleTreeLeafHash, computedHash);
    }
}