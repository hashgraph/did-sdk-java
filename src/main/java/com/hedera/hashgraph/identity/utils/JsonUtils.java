package com.hedera.hashgraph.identity.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.Instant;

/**
 * JSON utilities for Hedera identity.
 */
public final class JsonUtils {

  /**
   * This is a utility class, never to be instantiated.
   */
  private JsonUtils() {
    // Empty on purpose.
  }

  /**
   * Builds {@link Gson} instance with default configuration for Hedera identity.
   *
   * @return {@link Gson} instance.
   */
  public static Gson getGson() {
    return new GsonBuilder()
        .disableHtmlEscaping()
        .excludeFieldsWithoutExposeAnnotation()
        .registerTypeAdapter(Instant.class, Iso8601InstantTypeAdapter.getInstance())
        .create();
  }
}
