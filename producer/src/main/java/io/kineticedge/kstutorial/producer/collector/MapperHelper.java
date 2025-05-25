package io.kineticedge.kstutorial.producer.collector;

import org.mapstruct.Named;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class MapperHelper {

  private static final String HOSTNAME;

  private static final AtomicInteger iteration = new AtomicInteger(0);
  private static final AtomicLong ts = new AtomicLong(0L);

  static {
    try {
      HOSTNAME = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  private MapperHelper() {
  }

  @Named("hostname")
  public static String hostname() {
    return HOSTNAME;
  }

  @Named("stateToString")
  public static String stateToString(oshi.software.os.OSProcess.State state) {
    return state == null ? null : state.name();
  }


  public static Instant map(long value) {
    return Instant.ofEpochMilli(value);
  }

  @Named("iteration")
  public static int iteration() {
    return iteration.get();
  }

  @Named("ts")
  public static long ts() {
    return ts.get();
  }

  public static void incrementIteration() {
    iteration.incrementAndGet();
    ts.set(System.currentTimeMillis());
  }

}
