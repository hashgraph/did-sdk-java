package com.hedera.hashgraph.zeroknowledge.vp.proof;

import com.hedera.hashgraph.identity.hcs.vc.CredentialSubject;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcDocumentBase;
import com.hedera.hashgraph.zeroknowledge.exception.ZkSignatureException;
import com.hedera.hashgraph.zeroknowledge.merkletree.factory.MerkleTreeFactory;
import com.hedera.hashgraph.zeroknowledge.utils.ByteUtils;
import com.hedera.hashgraph.zeroknowledge.utils.HashUtils;
import com.hedera.hashgraph.zeroknowledge.utils.MerkleTreeUtils;
import io.horizen.common.librustsidechains.FieldElement;
import io.horizen.common.librustsidechains.FinalizationException;
import io.horizen.common.merkletreenative.BaseMerkleTree;
import io.horizen.common.schnorrnative.SchnorrKeyPair;
import io.horizen.common.schnorrnative.SchnorrPublicKey;
import io.horizen.common.schnorrnative.SchnorrSecretKey;
import io.horizen.common.schnorrnative.SchnorrSignature;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class ZkSignature<T extends CredentialSubject> implements ZeroKnowledgeSignature<T> {
    private String signature;
    private final MerkleTreeFactory merkleTreeFactory;
    private String merkleTreeRoot;

    public ZkSignature(MerkleTreeFactory merkleTreeFactory) {
        this(null, null, merkleTreeFactory);
    }

    public ZkSignature(String signature, String merkleTreeRoot, MerkleTreeFactory merkleTreeFactory) {
        this.signature = signature;
        this.merkleTreeRoot = merkleTreeRoot;
        this.merkleTreeFactory = merkleTreeFactory;
    }

    @Override
    public String getSignature() {
        return this.signature;
    }

    @Override
    public String getMerkleTreeRoot() {
        return merkleTreeRoot;
    }

    private void setMerkleTreeRoot(String merkleTreeRoot) {
        this.merkleTreeRoot = merkleTreeRoot;
    }

    @Override
    public void sign(byte[] privateKey, HcsVcDocumentBase<T> vcDocument) throws ZkSignatureException {
        FieldElement merkleTreeRoot = null, hash = null;
        SchnorrSignature schnorrSignature = null;
        List<FieldElement> documentIds = null;

        try (
                BaseMerkleTree merkleTree = merkleTreeFactory.getMerkleTree(vcDocument.getCredentialSubject());
                SchnorrSecretKey secretKey = SchnorrSecretKey.deserialize(privateKey);
                SchnorrPublicKey publicKey = secretKey.getPublicKey();
                SchnorrKeyPair keyPair = new SchnorrKeyPair(secretKey, publicKey)
        ) {
            documentIds = FieldElement.deserializeMany(vcDocument.getId().getBytes(StandardCharsets.UTF_8));

            merkleTree.finalizeTreeInPlace();
            merkleTreeRoot = merkleTree.root();
            setMerkleTreeRoot(ByteUtils.bytesToHex(merkleTreeRoot.serializeFieldElement()));

            hash = computeHash(documentIds, merkleTreeRoot);
            schnorrSignature = keyPair.signMessage(hash);

            this.signature = ByteUtils.bytesToHex(schnorrSignature.serializeSignature());
        } catch (Exception e) {
            throw new ZkSignatureException("Cannot sign document", e);
        } finally {
            closeAll(merkleTreeRoot, hash, schnorrSignature, documentIds);
        }
    }

    private void closeAll(FieldElement merkleTreeRoot, FieldElement hash, SchnorrSignature schnorrSignature, List<FieldElement> documentIds) {
        if (merkleTreeRoot != null) merkleTreeRoot.close();
        if (hash != null) hash.close();
        if (schnorrSignature != null) schnorrSignature.close();
        if (documentIds != null) {
            for (FieldElement currElement : documentIds) currElement.close();
        }
    }

    private FieldElement computeHash(List<FieldElement> documentIds, FieldElement merkleTreeRoot) throws FinalizationException {
        FieldElement documentId = HashUtils.hashFieldElementList(documentIds);
        return MerkleTreeUtils.computeHash(documentId, merkleTreeRoot);
    }
}
