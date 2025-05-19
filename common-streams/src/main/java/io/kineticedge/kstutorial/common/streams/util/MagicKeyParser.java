package io.kineticedge.kstutorial.common.streams.util;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * This is not "for production" in that it is driven for the bite-size examples, knowing that the keys are strings or windowed strings, and all windowed times
 * are over a given period of time.
 */
public class MagicKeyParser {

  private final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneOffset.UTC);
  //private final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneOffset.UTC);

  // demo assumes we are using timestamps around now, so lets just pick a 3 year window, easy.
  private static final long DEFAULT_MIN_TIMESTAMP;
  private static final long DEFAULT_MAX_TIMESTAMP;
  static {
    LocalDate today = LocalDate.now();
    Instant start = LocalDate.of(today.getYear() - 1, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant end = LocalDate.of(today.getYear() + 1, 12, 31).atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
    DEFAULT_MIN_TIMESTAMP = start.toEpochMilli();
    DEFAULT_MAX_TIMESTAMP = end.toEpochMilli();
  }

  public static String parse(byte[] bytes) {

    if (bytes == null) {
      return null;
    } else if (bytes.length < 8) {
      return new String(bytes);
    }

    long ts1 = getTimestamp(bytes, 8);
    long ts2 = getTimestamp(bytes, 16);

    long ts1Changelog = getTimestamp(bytes, 12);

    if (ts1 == -1 && ts2 == -1 && ts1Changelog == -1) {
      return new String(bytes);
    } else if (ts2 == -1 && ts1Changelog == -1) {
      byte[] keyBytes = new byte[bytes.length - 8];
      System.arraycopy(bytes, 0, keyBytes, 0, bytes.length - 8);
      return new String(keyBytes) + " (" + FORMATTER.format(Instant.ofEpochMilli(ts1)) + ")";
    } else if (ts1Changelog != -1) {
      byte[] keyBytes = new byte[bytes.length - 12];
      System.arraycopy(bytes, 0, keyBytes, 0, bytes.length - 12);
      return new String(keyBytes) + " (" + FORMATTER.format(Instant.ofEpochMilli(ts1Changelog)) + ")";
    } else {
      byte[] keyBytes = new byte[bytes.length - 16];
      System.arraycopy(bytes, 0, keyBytes, 0, bytes.length - 16);
      return new String(keyBytes) + " (" + FORMATTER.format(Instant.ofEpochMilli(ts2)) + "," + FORMATTER.format(Instant.ofEpochMilli(ts1)) + ")";
    }
  }


  private static long getTimestamp(byte[] bytes, int offset) {

    if (bytes.length < offset) {
      return -1;
    }

    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    buffer.position(bytes.length - offset);
    long value =  buffer.getLong();

    if (isValidTimestamp(value)) {
      return value;
    }

    return -1;
  }

  private static boolean isValidTimestamp(long timestamp) {
    return timestamp >= DEFAULT_MIN_TIMESTAMP && timestamp <= DEFAULT_MAX_TIMESTAMP;
  }

}
