package io.kineticedge.kstutorial.common.streams;

import io.kineticedge.kstutorial.common.streams.http.WebServer;
import io.kineticedge.kstutorial.common.streams.ws.SimpleWebSocketServer;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class KafkaStreamsRunner {

  private static final Duration SHUTDOWN = Duration.ofSeconds(30);

  private static final Logger log = LoggerFactory.getLogger(KafkaStreamsRunner.class);

  private final String applicationId;
  private final Topology topology;
  private final Map<String, Object> config;

  private KafkaStreams streams;
  private Map<String, String> metadata;

  private long lingerMs;

  public KafkaStreamsRunner(String applicationId, Topology topology, Map<String, Object> config, Map<String, String> metadata, long lingerMs) {
    this.applicationId = applicationId;
    this.topology = topology;
    this.config = config;
    this.metadata = metadata;
    this.lingerMs = lingerMs;
  }

  public KafkaStreams getStreams() {
    return streams;
  }

  public void start() {

    log.info("Topology:\n{}", topology.describe());

    streams = new KafkaStreams(topology, toProperties(config));


    streams.setUncaughtExceptionHandler(e -> {
      log.error("unhandled streams exception, shutting down (a warning of 'Detected that shutdown was requested. All clients in this app will now begin to shutdown' will repeat every 100ms for the duration of session timeout).", e);

      // Schedule a delayed exit to allow streams to attempt clean shutdown
      new Thread(() -> {
        try {
          // Give KafkaStreams a chance to begin shutdown
          Thread.sleep(5000);
          log.warn("Forcing application exit after unhandled exception");
          System.exit(1);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          log.warn("Shutdown thread interrupted, forcing immediate exit");
          System.exit(1);
        }
      }).start();

      return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION;
    });

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {

      if (streams.state().isRunningOrRebalancing()) {

        // New to Kafka Streams 3.3, you can have the application leave the group on shutting down (when member.id / static membership is used).
        //
        // There are reasons to do this and not to do it; from a development standpoint this makes starting/stopping
        // the application a lot easier reducing the time needed to rejoin the group.
        boolean leaveGroup = true;

        log.info("closing KafkaStreams with leaveGroup={}", leaveGroup);

        // can I interrupt the thread....

        KafkaStreams.CloseOptions closeOptions = new KafkaStreams.CloseOptions().timeout(SHUTDOWN).leaveGroup(leaveGroup);

        boolean isClean = streams.close(closeOptions);
        if (!isClean) {
          System.out.println("KafkaStreams was not closed cleanly");
        }

      } else if (streams.state().isShuttingDown()) {
        log.info("Kafka Streams is already shutting down with state={}, will wait {} to ensure proper shutdown.", streams.state(), SHUTDOWN);
        boolean isClean = streams.close(SHUTDOWN);
        if (!isClean) {
          System.out.println("KafkaStreams was not closed cleanly");
        }
        System.out.println("final KafkaStreams state=" + streams.state());
      }
    }));

    AtomicBoolean first = new AtomicBoolean(false);

//    final KafkaStreamsMetrics kafkaStreamsMetrics = new KafkaStreamsMetrics(streams);
//    kafkaStreamsMetrics.bindTo(Metrics.globalRegistry);
//    Metrics.globalRegistry.gauge(
//            "kafka_stream_application",
//            Tags.of(Tag.of("application.id", applicationId)),
//            streams,
//            s -> switch (s.state()) {
//              case RUNNING -> 1.0;
//              case REBALANCING -> 0.5;
//              default -> 0.0;
//            }
//    );

    final PrometheusMeterRegistry prometheusMeterRegistry = PrometheusRegistryManager.getRegistry();
    final KafkaStreamsMetrics kafkaStreamsMetrics = new KafkaStreamsMetrics(streams);
    kafkaStreamsMetrics.bindTo(prometheusMeterRegistry);
    prometheusMeterRegistry.gauge(
            "kafka_stream_application",
            Tags.of(Tag.of("application.id", applicationId)),
            streams,
            s -> switch (s.state()) {
              case RUNNING -> 1.0;
              case REBALANCING -> 0.5;
              default -> 0.0;
            }
    );

    AtomicBoolean ranOnce = new AtomicBoolean(false);

    streams.setStateListener((newState, oldState) -> {

        if (newState == KafkaStreams.State.RUNNING) {

          if (first.compareAndSet(false, true)) {

            System.out.println("FIRST TO RUNNING __ TODO");
          //  misconfigure();

            try {
              WebServer server = new WebServer(8080, applicationId, topology, streams, metadata, lingerMs);
              server.start();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }

            SimpleWebSocketServer wsServer = new SimpleWebSocketServer(8081);
            wsServer.start();

            System.out.println("STARTED");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
              System.out.println("SHUTTING DOWN");
              try {
                wsServer.stop();
                //server.stop(5000, "CLOSING!");
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
            }));


          }

          System.out.println("________");
          streams.metadataForLocalThreads().stream().forEach(s -> {
            s.activeTasks().stream().forEach(t -> {
              t.topicPartitions().stream().forEach(tp -> {
                System.out.println(tp);
              });
            });
          });
          System.out.println("________");

          System.out.println("x________");
          streams.metadataForAllStreamsClients().stream().forEach(s -> {
            s.topicPartitions().stream().forEach(tp -> {
              System.out.println(tp);
            });
          });
          System.out.println("x________");
        }
    });

    if (true) {
      streams.cleanUp();
    }

    streams.start();
  }

  private static Properties toProperties(final Map<String, Object> map) {
    final Properties properties = new Properties();
    properties.putAll(map);
    return properties;
  }

  private void misconfigure() {
    try (AdminClient client = AdminClient.create(Map.ofEntries(
            Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, config.get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)),
            Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
    ))) {
      client.listTopics().listings().get().forEach(existing -> {
        if (existing.name().startsWith(applicationId + "-")) {
          if (existing.name().endsWith("-repartition")) {
              ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, existing.name());
              Collection<AlterConfigOp> configOps = List.of(
                      new AlterConfigOp(new ConfigEntry("message.timestamp.type", "LogAppendTime"), AlterConfigOp.OpType.SET)
              );
              Map<ConfigResource, Collection<AlterConfigOp>> updates = Map.of(resource, configOps);
              try {
                client.incrementalAlterConfigs(updates).all().get();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              } catch (ExecutionException e) {
                throw new RuntimeException(e);
              }
          }
        }
      });

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
}
