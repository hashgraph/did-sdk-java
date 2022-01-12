package com.hedera.hashgraph.zeroknowledge.merkletree;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation used to mark all fields to be included as leaves in the merkle tree.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MerkleTreeLeaf {
    /**
     * The label that will be used when creating the merkle tree leaf.
     *
     * @return The label name.
     */
    String labelName();
}
