package com.hedera.hashgraph.identity.utils;

import com.google.common.collect.ImmutableList;
import com.google.gson.TypeAdapter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Gson type adapter for serialization/deserialization between {@link Instant} and ISO 8601 date format.
 */
public final class Iso8601InstantTypeAdapter extends InstantTypeAdapter {

  public static final DateTimeFormatter DEFAULT_OUTPUT_FORMATTER = DateTimeFormatter.ISO_INSTANT;

  public static final ImmutableList<DateTimeFormatter> DEFAULT_INPUT_PARSERS = ImmutableList.of(
      DateTimeFormatter.ISO_INSTANT,
      DateTimeFormatter.ISO_OFFSET_DATE_TIME,
      DateTimeFormatter.ISO_DATE_TIME,
      DateTimeFormatter.ISO_LOCAL_DATE_TIME);

  private static final Iso8601InstantTypeAdapter DEFAULT_INSTANCE = new Iso8601InstantTypeAdapter();

  /**
   * Returns the statically defined instance constructed with the default formatter.
   *
   * @return the instance
   * @see    #DEFAULT_OUTPUT_FORMATTER
   * @see    #DEFAULT_INPUT_PARSERS
   */
  public static TypeAdapter<Instant> getInstance() {
    return DEFAULT_INSTANCE;
  }

  public Iso8601InstantTypeAdapter() {
    super(DEFAULT_OUTPUT_FORMATTER, DEFAULT_INPUT_PARSERS);
  }
}
