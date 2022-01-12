package com.hedera.hashgraph.zeroknowledge.utils;

import io.horizen.common.librustsidechains.DeserializationException;
import io.horizen.common.librustsidechains.FieldElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.hedera.hashgraph.zeroknowledge.utils.MerkleTreeUtils.computeHash;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MockitoExtension.class)
class MerkleTreeUtilsTest {
    @Test
    public void testCorrectComputeHash() throws DeserializationException {
        // Arrange
        String documentIdString = "https://example.appnet.com/";
        FieldElement documentId = FieldElement.deserialize(documentIdString.getBytes(StandardCharsets.UTF_8));
        long fakeSeed = 123;
        FieldElement merkleTreeRoot = FieldElement.createRandom(fakeSeed);

        // Act
        try {
            computeHash(documentId, merkleTreeRoot);
        } catch (Exception e) {
            fail("An unexpected exception was thrown");
        }

        // Assert
    }
}