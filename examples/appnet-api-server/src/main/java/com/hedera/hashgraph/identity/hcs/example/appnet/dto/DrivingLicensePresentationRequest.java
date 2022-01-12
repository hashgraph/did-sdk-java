package com.hedera.hashgraph.identity.hcs.example.appnet.dto;

import com.google.gson.annotations.Expose;
import com.hedera.hashgraph.identity.hcs.example.appnet.vc.DrivingLicenseDocument;

public class DrivingLicensePresentationRequest {
    @Expose
    private DrivingLicenseDocument verifiableCredential;

    public DrivingLicenseDocument getVerifiableCredential() {
        return verifiableCredential;
    }

    public void setVerifiableCredential(DrivingLicenseDocument verifiableCredential) {
        this.verifiableCredential = verifiableCredential;
    }
}
