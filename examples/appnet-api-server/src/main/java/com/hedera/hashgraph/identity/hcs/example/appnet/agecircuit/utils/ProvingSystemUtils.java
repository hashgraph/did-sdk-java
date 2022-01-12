package com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.utils;

import io.horizen.common.librustsidechains.InitializationException;
import io.horizenlabs.provingsystemnative.ProvingSystem;

public final class ProvingSystemUtils {
    private static final int MAX_SEGMENT_SIZE = 1 << 17;
    private static final int SUPPORTED_SEGMENT_SIZE = 1 << 15;

    public static void generateDLogKeys() throws InitializationException {
        ProvingSystem.generateDLogKeys(MAX_SEGMENT_SIZE, SUPPORTED_SEGMENT_SIZE);
    }
}
