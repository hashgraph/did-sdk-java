package com.hedera.hashgraph.zeroknowledge.merkletree.factory;

import com.hedera.hashgraph.identity.hcs.vc.CredentialSubject;
import io.horizen.common.librustsidechains.FieldElementConversionException;
import io.horizen.common.librustsidechains.FinalizationException;
import io.horizen.common.librustsidechains.InitializationException;
import io.horizen.common.merkletreenative.BaseMerkleTree;
import io.horizen.common.merkletreenative.MerkleTreeException;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * A factory to build a merkle tree having the credential subjects' fields as leaves.
 */
public interface MerkleTreeFactory {
    /**
     * Builds a merkle tree from a list of credential subjects.
     *
     * @param credentialSubject List of credential subjects.
     * @param <T> The type of credential subject.
     * @return A merkle tree having the credential subjects' fields as leaves.
     * @throws FieldElementConversionException
     * @throws MerkleTreeException
     * @throws InitializationException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws FinalizationException
     */
    <T extends CredentialSubject> BaseMerkleTree getMerkleTree(List<T> credentialSubject) throws FieldElementConversionException, MerkleTreeException, InitializationException, InvocationTargetException, IllegalAccessException, FinalizationException;

    /**
     * Get the tree max height.
     * @return The tree height.
     */
    int getTreeHeight();

    /**
     * Get the tree processing steps.
     * @return The tree processing steps.
     */
    int getTreeProcessingSteps();
}
