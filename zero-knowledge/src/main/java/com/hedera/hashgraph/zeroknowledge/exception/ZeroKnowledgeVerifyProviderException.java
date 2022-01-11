package com.hedera.hashgraph.zeroknowledge.exception;

public class ZeroKnowledgeVerifyProviderException extends Exception {
    public ZeroKnowledgeVerifyProviderException(String message, Exception e) {
        super(message, e);
    }
}
