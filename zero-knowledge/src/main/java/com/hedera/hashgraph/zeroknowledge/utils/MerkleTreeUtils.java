package com.hedera.hashgraph.zeroknowledge.utils;

import io.horizen.common.librustsidechains.DeserializationException;
import io.horizen.common.librustsidechains.FieldElement;
import io.horizen.common.librustsidechains.FinalizationException;
import io.horizen.common.poseidonnative.PoseidonHash;

import java.nio.charset.StandardCharsets;

/**
 * A set of utilities for merkle trees.
 */
public final class MerkleTreeUtils {
    /**
     * Computes the Poseidon hash of the documentId and the merkleTreeRoot.
     *
     * @param documentId The documentId to be hashed.
     * @param merkleTreeRoot The merkleTreeRoot to be hashed.
     * @return The hash of documentId and merkleTreeRoot as a FieldElement.
     * @throws FinalizationException An exception that can occur during the hash finalize step; it means one of the two arguments was not in the correct format.
     */
    public static FieldElement computeHash(FieldElement documentId, FieldElement merkleTreeRoot) throws FinalizationException {
        try (PoseidonHash hash = PoseidonHash.getInstanceConstantLength(2)) {
            hash.update(documentId);
            hash.update(merkleTreeRoot);

            return hash.finalizeHash();
        }
    }

    /**
     * At the moment the supported types for merkle tree leaves are Integer, Long and String.
     *
     * @param field The value to be converted as FieldElement.
     * @return The FieldElement containing the field.
     * @throws IllegalArgumentException The field's type is not yet supported.
     * @throws DeserializationException The field was not deserializable in a FieldElement.
     */
    public static FieldElement getFieldElementByAllowedTypes(Object field) throws IllegalArgumentException, DeserializationException {
        if (field instanceof Long) {
            return FieldElement.createFromLong((long) field);
        } else if (field instanceof Integer) {
            return FieldElement.createFromLong((int) field);
        } else if (field instanceof String) {
            return FieldElement.deserialize(((String) field).getBytes(StandardCharsets.UTF_8));
        } else {
            throw new IllegalArgumentException("Credential subject leaf type must be a long or String");
        }
    }
}
