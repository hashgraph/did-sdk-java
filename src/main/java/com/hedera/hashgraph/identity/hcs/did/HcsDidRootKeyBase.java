package com.hedera.hashgraph.identity.hcs.did;

import com.google.gson.annotations.Expose;

public abstract class HcsDidRootKeyBase {
    @Expose
    private final String id;

    @Expose
    private final String type;

    @Expose
    private final String controller;

    @Expose
    private final String publicKeyBase58;

    public HcsDidRootKeyBase(final String id, final String type, final String controller, final String publicKeyBase58) {
        this.id = id;
        this.type = type;
        this.controller = controller;
        this.publicKeyBase58 = publicKeyBase58;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getController() {
        return controller;
    }

    public String getPublicKeyBase58() {
        return publicKeyBase58;
    }
}
