package com.hedera.hashgraph.identity.hcs;

import com.google.gson.annotations.Expose;
import com.hedera.hashgraph.identity.utils.JsonUtils;
import java.time.Instant;

public abstract class Message {

  @Expose(serialize = true, deserialize = true)
  protected final Instant timestamp;

  protected Message() {
    this.timestamp = Instant.now();
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * Converts this message into JSON string.
   *
   * @return The JSON representation of this message.
   */
  public String toJson() {
    return JsonUtils.getGson().toJson(this);
  }
}