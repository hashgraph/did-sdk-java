package com.hedera.hashgraph.identity.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Iterator;

/**
 * Gson type adapter for {@link Instant} type and configurable date/time string format.
 */
public class InstantTypeAdapter extends TypeAdapter<Instant> {

  private final DateTimeFormatter outputDateTimeFormatter;
  private final DateTimeFormatter firstParser;
  private final ImmutableList<DateTimeFormatter> otherParsers;

  /**
   * Creates a new type adapter instance.
   *
   * @param dateTimeFormatter    Formatter.
   * @param inputDateTimeParsers Date/time parsers list.
   */
  public InstantTypeAdapter(final DateTimeFormatter dateTimeFormatter,
      final Iterable<DateTimeFormatter> inputDateTimeParsers) {
    this.outputDateTimeFormatter = checkNotNull(dateTimeFormatter);
    Iterator<DateTimeFormatter> parsers = inputDateTimeParsers.iterator();
    checkArgument(parsers.hasNext(), "input parsers list must be nonempty");
    this.firstParser = parsers.next();
    this.otherParsers = ImmutableList.copyOf(parsers);
  }

  @Override
  public void write(final JsonWriter out, final Instant value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else {
      String instantStr = outputDateTimeFormatter.format(value);
      out.value(instantStr);
    }
  }

  @Override
  public Instant read(final JsonReader in) throws IOException {
    JsonToken token = in.peek();
    if (token != JsonToken.STRING) {
      in.skipValue();
      return null;
    }
    return parse(in.nextString());
  }

  /**
   * Parses the given string into {@link Instant} object.
   *
   * @param  instantStr               Instant as string.
   * @return                          {@link Instant} object.
   * @throws IllegalArgumentException In case parsing fails.
   */
  protected Instant parse(final String instantStr) {
    if (instantStr == null) {
      return null;
    }

    TemporalAccessor accessor = null;
    try {
      accessor = firstParser.parse(instantStr);
    } catch (DateTimeParseException ignore) {
      for (DateTimeFormatter parser : otherParsers) {
        try {
          accessor = parser.parse(instantStr);
        } catch (DateTimeParseException ignoreToo) {
          // ignore the error and move to the next parser
          continue;
        }
      }
    }

    if (accessor == null) {
      throw new IllegalArgumentException("Input string does not match any parsing formats used by this adapter");
    }

    return Instant.from(accessor);
  }
}
