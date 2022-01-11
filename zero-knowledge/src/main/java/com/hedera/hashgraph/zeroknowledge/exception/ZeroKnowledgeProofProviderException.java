package com.hedera.hashgraph.zeroknowledge.exception;

public class ZeroKnowledgeProofProviderException extends Exception {
    public ZeroKnowledgeProofProviderException(String message, Exception e) {
        super(message, e);
    }
}
