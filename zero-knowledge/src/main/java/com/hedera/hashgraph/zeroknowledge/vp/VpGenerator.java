package com.hedera.hashgraph.zeroknowledge.vp;

import com.hedera.hashgraph.identity.hcs.vc.CredentialSubject;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcDocumentBase;
import com.hedera.hashgraph.zeroknowledge.exception.VerifiablePresentationGenerationException;

import java.util.List;
import java.util.Map;

/**
 * Interface for a verifiable presentation generator.
 * @param <T> The verifiable credential document class.
 * @param <U> The verifiable presentation document class.
 */
public interface VpGenerator<T extends HcsVcDocumentBase<? extends CredentialSubject>, U extends HcsVpDocumentBase<? extends VerifiableCredential>> {
    /**
     * It takes a list of verifiable documents and any metadata needed to create a verifiable presentation containing all of them.
     *
     * @param vcDocuments The list of verifiable credential documents to include in the presentation.
     * @param presentationMetadata Any metadata needed to create the presentation.
     * @return The verifiable presentation document.
     * @throws VerifiablePresentationGenerationException An exception representing the error occurred while generating the presentation.
     */
    U generatePresentation(List<T> vcDocuments, Map<String, Object> presentationMetadata) throws VerifiablePresentationGenerationException;
}
