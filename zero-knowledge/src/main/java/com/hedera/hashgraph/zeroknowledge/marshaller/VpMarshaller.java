package com.hedera.hashgraph.zeroknowledge.marshaller;

import com.hedera.hashgraph.zeroknowledge.vp.HcsVpDocumentBase;
import com.hedera.hashgraph.zeroknowledge.vp.VerifiableCredentialBase;

/**
 * The marshaller for a Verifiable Presentation
 *
 * @param <T> A verifiable presentation that extends HcsVpDocumentBase
 */

public interface VpMarshaller<T extends HcsVpDocumentBase<? extends VerifiableCredentialBase>> {
    /**
     * Transforms the internal presentation representation into a formatted document compliant to the w3c standard.
     *
     * @param vpDocument The presentation to be formatted.
     * @return The string representation of the presentation.
     */
    String fromDocumentToString(T vpDocument);

    /**
     * Transform a formatted string presentation into its internal representation.
     *
     * @param stringDocument The formatted presentation as string.
     * @return The internal representation of the verifiable presentation.
     */
    T fromStringToDocument(String stringDocument);
}
