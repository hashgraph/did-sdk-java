package com.hedera.hashgraph.zeroknowledge.vp;

import com.google.common.collect.Lists;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.threeten.bp.Instant;

import java.util.List;

/**
 * A base class used to create a new verifiable presentation document.
 */
public abstract class HcsPresentationBase {
    @Expose(deserialize = false)
    @SerializedName(HcsVpDocumentJsonProperties.CONTEXT)
    protected List<String> context;

    @Expose
    @SerializedName(HcsVpDocumentJsonProperties.ID)
    protected String id;

    @Expose
    @SerializedName(HcsVpDocumentJsonProperties.TYPE)
    protected List<String> type;

    @Expose
    @SerializedName(HcsVpDocumentJsonProperties.HOLDER)
    protected String holder;

    @Expose
    @SerializedName(HcsVpDocumentJsonProperties.ISSUANCE_DATE)
    protected Instant issuanceDate;

    public HcsPresentationBase() {
        this.type = Lists.newArrayList(HcsVpDocumentJsonProperties.VERIFIABLE_CREDENTIAL_TYPE);
        this.issuanceDate = Instant.now();
    }

    public List<String> getContext() {
        return context;
    }

    public void setContext(List<String> context) {
        this.context = context;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getType() {
        return type;
    }

    public void setType(List<String> type) {
        this.type = type;
    }

    public String getHolder() {
        return holder;
    }

    public void setHolder(String holder) {
        this.holder = holder;
    }

    public Instant getIssuanceDate() {
        return issuanceDate;
    }

    public void setIssuanceDate(Instant issuanceDate) {
        this.issuanceDate = issuanceDate;
    }

    /**
     * Add an additional type to `type` field of the VP document.
     *
     * @param type The type to add.
     */
    public void addType(final String type) {
        this.type.add(type);
    }
}
