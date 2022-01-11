package com.hedera.hashgraph.identity.hcs.vc.marshaller;

import com.hedera.hashgraph.identity.hcs.vc.CredentialSubject;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcDocumentBase;

/**
 * The marshaller for a Verifiable Credential
 *
 * @param <T> A verifiable document that extends HcsVcDocumentBase
 */
public interface VcMarshaller<T extends HcsVcDocumentBase<? extends CredentialSubject>> {
    /**
     * Transforms the internal document representation into a formatted document compliant to the w3c standard.
     *
     * @param vcDocument The document to be formatted.
     * @return The formatted document as a string.
     */
    String fromDocumentToString(T vcDocument);

    /**
     * Transform a formatted string document into its internal representation.
     *
     * @param stringDocument The formatted document as string.
     * @return The internal representation of the verifiable document.
     */
    T fromStringToDocument(String stringDocument);
}