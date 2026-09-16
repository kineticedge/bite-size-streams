package io.kineticedge.kstutorial.producer;

import com.beust.jcommander.Parameter;
import io.kineticedge.kstutorial.common.config.BaseOptions;

public class Options extends BaseOptions {

  @Parameter(names = { "--processes-frequency" }, description = "frequency of publishing process information")
  private long processesFrequency = 10_000L;

  @Parameter(names = { "--windows-frequency" }, description = "frequency of publishing window information")
  private long windowFrequency = 1_000L;

  @Parameter(names = { "--services-frequency" }, description = "frequency of publishing services information")
  private long servicesFrequency = 10_000L;

  @Parameter(names = { "--sessions-frequency" }, description = "frequency of publishing sessions information")
  private long sessionsFrequency = 10_000L;

  @Parameter(names = { "--threads-frequency" }, description = "frequency of publishing threads information")
  private long threadsFrequency = 60_000L;


  public long getProcessesFrequency() {
    return processesFrequency;
  }

  public long getWindowFrequency() {
    return windowFrequency;
  }

  public long getServicesFrequency() {
    return servicesFrequency;
  }

  public long getSessionsFrequency() {
    return sessionsFrequency;
  }

  public long getThreadsFrequency() {
    return threadsFrequency;
  }
}
