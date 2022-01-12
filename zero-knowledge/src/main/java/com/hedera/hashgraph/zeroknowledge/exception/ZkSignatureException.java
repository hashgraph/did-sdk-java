package com.hedera.hashgraph.zeroknowledge.exception;

public class ZkSignatureException extends Exception {
    public ZkSignatureException(String message, Exception e) {
        super(message, e);
    }
}
