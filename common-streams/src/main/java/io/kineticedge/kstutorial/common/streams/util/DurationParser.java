package io.kineticedge.kstutorial.common.streams.util;


import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Support standard Duration ISO8601 format and a simplified format.
 * <p>
 * Simplified format includes support for the following, case insensitive provided case is consistent
 * <p>
 * (e.g. 'ns' and 'NS' are allowed but 'nS' or 'nS' are not.)
 * <p>
 * 1h  = 1 hour
 * 1m  = 1 minute
 * 1s  = 1 second
 * 1m  = 1 millisecond
 * 1us = 1 microsecond
 * 1ns = 1 nanosecond
 *
 */
public class DurationParser {

  private static final Pattern SIMPLE = Pattern.compile("^([+-]?\\d+)(d|D|h|H|m|M|s|S|ms|MS|us|US|ns|NS)$");
  private static final Pattern ISO8601 = Pattern.compile("^[+-]?P.*$");

  private static final Map<String, Function<Long, Duration>> CONVERTER;

  static {
    final Map<String, Function<Long, Duration>> map = new HashMap<>();

    map.put("d", value -> Duration.of(value, ChronoUnit.DAYS));
    map.put("h", value -> Duration.of(value, ChronoUnit.HOURS));
    map.put("m", value -> Duration.of(value, ChronoUnit.MINUTES));
    map.put("s", value -> Duration.of(value, ChronoUnit.SECONDS));
    map.put("ms", value -> Duration.of(value, ChronoUnit.MILLIS));
    map.put("us", value -> Duration.of(value, ChronoUnit.MICROS));
    map.put("ns", value -> Duration.of(value, ChronoUnit.NANOS));

    CONVERTER = Collections.unmodifiableMap(map);
  }

  public static Optional<Duration> parse(final String value) {

    if (StringUtils.isBlank(value)) {
      return Optional.empty();
    }

    Matcher matcher = SIMPLE.matcher(value);

    if (matcher.matches()) {

      final Long duration = Long.parseLong(matcher.group(1));
      final String unit = matcher.group(2).toLowerCase(Locale.ROOT);

      return Optional.of(CONVERTER.get(unit).apply(duration));
    } else if (ISO8601.matcher(value).matches()) {
      return Optional.of(Duration.parse(value));
    } else {
      throw new IllegalArgumentException("invalid formatted duration " + value + ".");
    }
  }

  public static String toString(final Duration duration) {

    if (duration == null) {
      return "";
    }
    if (duration.isZero()) {
      return "0s";
    }

    long totalSeconds = duration.getSeconds();
    boolean negative = totalSeconds < 0;
    long abs = Math.abs(totalSeconds);

    long days = abs / 86_400; abs %= 86_400;
    long hours = abs / 3_600; abs %= 3_600;
    long minutes = abs / 60;  long seconds = abs % 60;

    String formatted;
    if (days > 0) {
      // days, optionally with hours
      formatted = (hours == 0) ? (days + "d") : (days + "d " + hours + "h");
    } else if (hours > 0) {
      // hours, optionally with minutes as H:MMh
      formatted = (minutes == 0)
              ? (hours + "h")
              : String.format("%d:%02dh", hours, minutes);
    } else if (minutes > 0) {
      // minutes, optionally with seconds as M:SSm
      formatted = (seconds == 0)
              ? (minutes + "m")
              : String.format("%d:%02dm", minutes, seconds);
    } else {
      // seconds only
      formatted = seconds + "s";
    }

    return negative ? "-" + formatted : formatted;
  }
}