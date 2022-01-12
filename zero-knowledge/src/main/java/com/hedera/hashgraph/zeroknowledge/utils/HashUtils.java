package com.hedera.hashgraph.zeroknowledge.utils;

import io.horizen.common.librustsidechains.FieldElement;
import io.horizen.common.librustsidechains.FinalizationException;
import io.horizen.common.poseidonnative.PoseidonHash;

import java.util.List;

public final class HashUtils {
    public static FieldElement hashFieldElementList(List<FieldElement> fieldElements) throws FinalizationException {
        int elementsToHash = fieldElements.size();
        try (PoseidonHash hash = PoseidonHash.getInstanceConstantLength(elementsToHash)) {
            for (FieldElement currElement : fieldElements) {
                hash.update(currElement);
            }

            return hash.finalizeHash();
        }
    }
}
