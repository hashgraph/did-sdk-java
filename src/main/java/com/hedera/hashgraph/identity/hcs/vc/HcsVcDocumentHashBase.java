package com.hedera.hashgraph.identity.hcs.vc;

import com.google.common.collect.Lists;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.util.List;

/**
 * The part of the VC document that is used for hash calculation.
 */
abstract class HcsVcDocumentHashBase {
  @Expose(serialize = true, deserialize = true)
  @SerializedName(HcsVcDocumentJsonProperties.ID)
  protected String id;

  @Expose(serialize = true, deserialize = true)
  @SerializedName(HcsVcDocumentJsonProperties.TYPE)
  protected List<String> type;

  @Expose(serialize = true, deserialize = true)
  @SerializedName(HcsVcDocumentJsonProperties.ISSUER)
  @JsonAdapter(IssuerTypeAdapterFactory.class)
  protected Issuer issuer;

  @Expose(serialize = true, deserialize = true)
  @SerializedName(HcsVcDocumentJsonProperties.ISSUANCE_DATE)
  protected Instant issuanceDate;

  /**
   * Creates a new VC document instance.
   */
  protected HcsVcDocumentHashBase() {
    this.type = Lists.newArrayList(HcsVcDocumentJsonProperties.VERIFIABLE_CREDENTIAL_TYPE);
  }
}
