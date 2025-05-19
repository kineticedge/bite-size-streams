package io.kineticedge.kstutorial.common.streams;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Create a singleton PrometheusRegistry class to ensure only one registry exists
public class PrometheusRegistryManager {
  private static final Logger log = LoggerFactory.getLogger(PrometheusRegistryManager.class);
  private static final PrometheusMeterRegistry REGISTRY;

  static {
    // Create registry with configuration that helps with duplicate registrations
    REGISTRY = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    // Add filters to handle duplicates gracefully
    REGISTRY.config().meterFilter(new MeterFilter() {
      @Override
      public MeterFilterReply accept(Meter.Id id) {
        return MeterFilterReply.NEUTRAL;
      }
    });

    // Add the registry to the global composite
    Metrics.globalRegistry.add(REGISTRY);

    log.info("Prometheus registry initialized");
  }

  public static PrometheusMeterRegistry getRegistry() {
    return REGISTRY;
  }
}