package com.hedera.hashgraph.zeroknowledge.vp;

import com.hedera.hashgraph.identity.hcs.vc.CredentialSubject;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcDocumentBase;
import com.hedera.hashgraph.zeroknowledge.proof.ZeroKnowledgeProverProvider;
import com.hedera.hashgraph.zeroknowledge.proof.model.ZeroKnowledgeProofPublicInput;
import com.hedera.hashgraph.zeroknowledge.exception.VpDocumentGeneratorException;
import com.hedera.hashgraph.zeroknowledge.exception.ZeroKnowledgeProofProviderException;

import java.util.Map;

/**
 * An abstract verifiable presentation generator that uses a snark proof.
 * @param <T> The verifiable credential document class.
 * @param <U> The verifiable presentation document class.
 * @param <P> The public input for the snark proof generation.
 */
public abstract class VpZeroKnowledgeGenerator<T extends HcsVcDocumentBase<? extends CredentialSubject>, U extends HcsVpDocumentBase<? extends VerifiableCredential>, P extends ZeroKnowledgeProofPublicInput>
        implements VpGenerator<T, U> {
    private final ZeroKnowledgeProverProvider<P> zeroKnowledgeProofProvider;

    public VpZeroKnowledgeGenerator(ZeroKnowledgeProverProvider<P> zeroKnowledgeProofProvider) {
        this.zeroKnowledgeProofProvider = zeroKnowledgeProofProvider;
    }

    /**
     * A method to generate a zero knowledge proof for a verifiable presentation document.
     * @param document The verifiable credential document to include in the verifiable presentation.
     * @param presentationMetadata Any needed metadata to include in the zero knowledge proof computation.
     * @return The zero knowledge proof in byte array format.
     * @throws VpDocumentGeneratorException An exception occurred while generating the proof.
     */
    protected byte[] generateZeroKnowledgeProof(T document, Map<String, Object> presentationMetadata) throws VpDocumentGeneratorException {
        P publicInput = getProofPublicInput(document, presentationMetadata);
        try {
            return zeroKnowledgeProofProvider.createProof(publicInput);
        } catch (ZeroKnowledgeProofProviderException e) {
            throw new VpDocumentGeneratorException(
                    String.format("Cannot generate verifiable presentation from document: %s", document),
                    e
            );
        }
    }

    /**
     * The specific public input to generate the proof.
     *
     * @param document The verifiable credential document.
     * @param presentationMetadata Any metadata needed to generate the public input.
     * @return The public input to generate the proof.
     */
    protected abstract P getProofPublicInput(T document, Map<String, Object> presentationMetadata);
}
