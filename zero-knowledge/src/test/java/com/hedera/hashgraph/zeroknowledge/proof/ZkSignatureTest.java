package com.hedera.hashgraph.zeroknowledge.proof;

import com.hedera.hashgraph.identity.hcs.vc.HcsVcDocumentBase;
import com.hedera.hashgraph.zeroknowledge.merkletree.factory.MerkleTreeFactory;
import com.hedera.hashgraph.zeroknowledge.mock.credentialsubject.TestCredentialSubject;
import com.hedera.hashgraph.zeroknowledge.vp.proof.ZkSignature;
import io.horizen.common.librustsidechains.FieldElement;
import io.horizen.common.librustsidechains.FieldElementConversionException;
import io.horizen.common.librustsidechains.FinalizationException;
import io.horizen.common.librustsidechains.InitializationException;
import io.horizen.common.merkletreenative.BaseMerkleTree;
import io.horizen.common.merkletreenative.MerkleTreeException;
import io.horizen.common.schnorrnative.SchnorrKeyPair;
import io.horizen.common.schnorrnative.SchnorrSecretKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZkSignatureTest {
    @Mock
    private HcsVcDocumentBase<TestCredentialSubject> vcDocumentBase;

    @Mock
    private MerkleTreeFactory merkleTreeFactory;

    @Mock
    private BaseMerkleTree baseMerkleTree;

    @InjectMocks
    private ZkSignature<TestCredentialSubject> zkSignature;

    @Test
    public void callingSignMethod_InitializeTheSignatureField() throws FieldElementConversionException, MerkleTreeException, InitializationException, InvocationTargetException, IllegalAccessException, FinalizationException {
        // Arrange
        String docId = "fake-docId";
        List<TestCredentialSubject> credentialSubjects = Collections.singletonList(
                new TestCredentialSubject("fake-name", "fake-surname", 22)
        );
        SchnorrKeyPair keyPair = SchnorrKeyPair.generate();
        SchnorrSecretKey secretKey = keyPair.getSecretKey();

        when(vcDocumentBase.getId()).thenReturn(docId);
        when(vcDocumentBase.getCredentialSubject()).thenReturn(credentialSubjects);
        when(merkleTreeFactory.getMerkleTree(eq(credentialSubjects))).thenReturn(baseMerkleTree);
        when(baseMerkleTree.root()).thenReturn(FieldElement.createFromLong(1234));

        // Assert
        assertNull(zkSignature.getSignature());

        // Act
        try {
            zkSignature.sign(secretKey.serializeSecretKey(), vcDocumentBase);
        } catch (Exception e) {
            fail("An unexpected exception occurred while signing");
        }

        // Assert
        assertNotNull(zkSignature.getSignature());
    }
}