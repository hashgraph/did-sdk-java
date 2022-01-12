package com.hedera.hashgraph.zeroknowledge.exception;

public class ZeroKnowledgeProofGenerationException extends Exception {
    public ZeroKnowledgeProofGenerationException(String message, Exception exception) {
        super(message, exception);
    }
}
