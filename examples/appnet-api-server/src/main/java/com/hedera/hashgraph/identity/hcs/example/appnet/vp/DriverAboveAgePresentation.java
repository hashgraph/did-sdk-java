package com.hedera.hashgraph.identity.hcs.example.appnet.vp;

import com.hedera.hashgraph.zeroknowledge.vp.HcsVpDocumentBase;
import org.threeten.bp.Instant;

import java.util.ArrayList;

public class DriverAboveAgePresentation extends HcsVpDocumentBase<DriverAboveAgeVerifiableCredential> {
    public static final String ID = "http://example.gov/credentials/3732";
    public static final String DRIVER_ABOVE_AGE_CONTEXT = "https://www.w3.org/2018/credentials/examples/v1";
    private static final String DRIVER_ABOVE_AGE_TYPE = "DriverAboveAge";

    public DriverAboveAgePresentation() {
        super();

        this.id = ID;
        this.issuanceDate = Instant.now();
        this.context.add(DRIVER_ABOVE_AGE_CONTEXT);
        addType(DRIVER_ABOVE_AGE_TYPE);
        this.verifiableCredential = new ArrayList<>();
    }

    public void setHolder(String holder) {
        this.holder = holder;
    }

    public void addVerifiableCredential(DriverAboveAgeVerifiableCredential vcDocuments) {
        this.verifiableCredential.add(vcDocuments);
    }
}
