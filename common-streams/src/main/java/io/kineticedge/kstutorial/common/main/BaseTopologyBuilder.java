package io.kineticedge.kstutorial.common.main;

import io.kineticedge.kstutorial.common.config.TopologyConfig;
import io.kineticedge.kstutorial.common.config.WindowConfig;
import io.kineticedge.kstutorial.common.serde.JsonSerde;
import io.kineticedge.kstutorial.common.streams.SimpleProcessingExceptionHandler;
import io.kineticedge.kstutorial.common.streams.ThrottlingDeserializationExceptionHandler;
import io.kineticedge.kstutorial.domain.Id;
import io.kineticedge.kstutorial.domain.OSWindow;
import io.kineticedge.kstutorial.domain.Rectangle;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.EmitStrategy;
import org.apache.kafka.streams.kstream.Window;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.processor.PunctuationType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

public abstract class BaseTopologyBuilder implements TopologyBuilder {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BaseTopologyBuilder.class);

  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  protected static final Random random = new Random();

  private static final Map<String, Object> defaults = Map.ofEntries(
          Map.entry(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"),
          Map.entry(StreamsConfig.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT"),
          Map.entry(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName()),
          Map.entry(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class.getName()),
          Map.entry(StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG, "DEBUG"),
          Map.entry(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 1),
          Map.entry(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 5_000L),
          Map.entry(StreamsConfig.LOG_SUMMARY_INTERVAL_MS_CONFIG, 15_000L),
          Map.entry(StreamsConfig.DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, ThrottlingDeserializationExceptionHandler.class),

          //TODO
          Map.entry(ThrottlingDeserializationExceptionHandler.THROTTLING_DESERIALIZATION_EXCEPTION_THRESHOLD, ".1"),

          Map.entry(StreamsConfig.PROCESSING_EXCEPTION_HANDLER_CLASS_CONFIG, SimpleProcessingExceptionHandler.class),

          Map.entry(ProducerConfig.LINGER_MS_CONFIG, 10),
          Map.entry(ProducerConfig.BATCH_SIZE_CONFIG, 200000),
          //Map.entry(StreamsConfig.PROCESSOR_WRAPPER_CLASS_CONFIG, ProcessWrapperX.class.getName()),

          Map.entry(StreamsConfig.TOPOLOGY_OPTIMIZATION_CONFIG, StreamsConfig.OPTIMIZE),

          // internal kafka consumer property to leave group immediately, helpful for testing kafka streams (and demos)
          Map.entry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),


//          Map.entry(StreamsConfig.TASK_ASSIGNOR_CLASS_CONFIG, "io.kineticedge.ks.CustomTaskAssignor"),

          Map.entry("internal.leave.group.on.close", true)
  );

  private TopologyConfig topologyConfig;

  public void setConfig(TopologyConfig topologyConfig) {
    this.topologyConfig = topologyConfig;
  }

  public Map<String, Object> properties() {

    final Map<String, Object> map = new HashMap<>(defaults);

    map.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId());

    topologyConfig.numThreads().ifPresent(v -> map.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, v));
    topologyConfig.commitInterval().ifPresent(v -> map.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, v));
    topologyConfig.optimization().ifPresent(v -> map.put(StreamsConfig.TOPOLOGY_OPTIMIZATION_CONFIG, v));

    topologyConfig.eosEnabled().ifPresent(v -> {
      if (v) {
        log.info("setting {} to {}", StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
        map.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
      }
    });

    return map;
  }


  public boolean isCachingDisabled() {
    return topologyConfig.cachingDisabled().orElse(false);
  }

  public boolean isFeatureDisabled() {
    return topologyConfig.disableFeature().orElse(false);
  }

  public boolean isFeatureEnabled() {
    return !isFeatureDisabled();
  }

  public EmitStrategy emitStrategy() {
    return switch (topologyConfig.emitStrategy()) {
      case ON_WINDOW_CLOSE -> EmitStrategy.onWindowClose();
      case ON_WINDOW_UPDATE -> EmitStrategy.onWindowUpdate();
      case null -> EmitStrategy.onWindowUpdate();
    };
  }

  protected PunctuationType punctuationType() {
    return topologyConfig.punctuationType();
  }

  protected WindowConfig windowConfig() {
    return topologyConfig.windowConfig();
  }

  @Override
  public Topology topology() {

    final var tc = new org.apache.kafka.streams.TopologyConfig(new StreamsConfig(this.properties()));

    final var builder = new StreamsBuilder(tc);

    build(builder);

    return builder.build(asProperties(this.properties()));
  }

  @Override
  public List<String> topics() {
    return List.of();
  }

  abstract protected void build(StreamsBuilder builder);


  private Properties asProperties(Map<String, Object> map) {
    final Properties properties = new Properties();
    properties.putAll(map);
    return properties;
  }

  protected static void print(String k, Id v) {
    log.info("key={}, value={}", k, v);
  }

  protected static void print(String k, String v) {
    log.info("key={}, value={}", k, v);
  }

  protected static String format(long timestamp) {
    return formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()));
  }

  protected static String format(Windowed<String> k) {
    return k.key() + " [" + format(k.window().start()) + ", " + format(k.window().end()) + ")";
  }

  protected static void sleep(long duration) {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  protected static String rectangleToString(Rectangle rectangle) {
    return String.format("pos=(%d,%d) dimensions=%dx%d", rectangle.x(), rectangle.y(), rectangle.width(), rectangle.height());
  }

  protected static String rectangleToString(OSWindow window) {
    return String.format("pos=(%d,%d) dimensions=%dx%d", window.x(), window.y(), window.width(), window.height());
  }

}
