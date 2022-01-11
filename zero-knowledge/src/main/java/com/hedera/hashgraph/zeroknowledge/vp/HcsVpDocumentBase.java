package com.hedera.hashgraph.zeroknowledge.vp;

import com.google.common.collect.Lists;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.hedera.hashgraph.identity.utils.SingleToArrayTypeAdapterFactory;

import java.util.List;

public abstract class HcsVpDocumentBase<T extends VerifiableCredentialBase> extends HcsPresentationBase {
    @Expose
    @SerializedName(HcsVpDocumentJsonProperties.VERIFIABLE_CREDENTIAL)
    @JsonAdapter(SingleToArrayTypeAdapterFactory.class)
    protected List<T> verifiableCredential;

    /**
     * Creates a new VP Document instance.
     */
    public HcsVpDocumentBase() {
        super();
        this.context = Lists.newArrayList(HcsVpDocumentJsonProperties.FIRST_CONTEXT_ENTRY);
    }

    public void addVerifiableCredential(T verifiableCredential) {
        this.verifiableCredential.add(verifiableCredential);
    }

    public List<T> getVerifiableCredential() {
        return verifiableCredential;
    }

    public void setVerifiableCredential(List<T> verifiableCredential) {
        this.verifiableCredential = verifiableCredential;
    }

    public String getCredentialHashForVerifiableCredentialByIndex(int verifiableCredentialIndex) {
        if (verifiableCredentialIndex < 0 || verifiableCredentialIndex > this.verifiableCredential.size()) {
            throw new IllegalStateException();
        }

        return this.verifiableCredential.get(verifiableCredentialIndex).toCredentialHash();
    }
}
