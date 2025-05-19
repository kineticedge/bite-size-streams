package io.kineticedge.kstutorial.producer.collector;

import org.mapstruct.Named;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;

public final class MapperHelper {

  private static final String HOSTNAME;

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

}
