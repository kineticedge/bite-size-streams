package io.kineticedge.kstutorial.common.main;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import io.kineticedge.kstutorial.common.config.Options;
import io.kineticedge.kstutorial.common.config.OptionsUtil;
import io.kineticedge.kstutorial.common.config.WindowConfig;
import io.kineticedge.kstutorial.common.streams.KafkaStreamsRunner;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.EmitStrategy;
import org.apache.kafka.streams.processor.PunctuationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);

  private static final List<String> TOPICS = List.of("processes", "windows", "threads", "services", "sessions");

  private static void create(final String bootstrapServers, List<String> topics, String applicationId) {
    try (AdminClient client = AdminClient.create(Map.ofEntries(
            Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers),
            Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
    ))) {


      log.info("******************************************");

      try {

        final List<String> tt = Stream.concat(TOPICS.stream(), topics.stream()).collect(Collectors.toCollection(ArrayList::new));
        final List<String> t = new ArrayList<>(tt);

        final List<String> internal = new ArrayList<>();

        client.listTopics().listings().get().forEach(existing -> {
          //log.info("existing topic: {}", existing.name());
          t.remove(existing.name());
          if (existing.name().startsWith(applicationId + "-")) {
            if (existing.name().endsWith("-changelog") ||
                    existing.name().endsWith("-repartition") ||
                    existing.name().endsWith("-topic")) {
              internal.add(existing.name());
            }
          }
        });

        log.info("creating topics: {}", t);
        final CreateTopicsResult result = client.createTopics(
                t.stream()
                        .map(topic -> new NewTopic(topic, 2, (short) -1))
                        .toList()
        );
        result.all().get();
        Thread.sleep(500L);

        // delete the internal application topics so it starts new/fresh...
        log.info("deleting internal topics: {}", internal);
        client.deleteTopics(internal).all().get();

        // truncate the input topics, so demo had a clean start.
        log.info("truncating topics: {}", tt);
        truncateRecords(client, tt);

        Thread.sleep(500L);

        // under kraft, creating a topic could have metadata not propagated in time, so adding this since metadata
        // updates can be at 500ms updates.
      } catch (InterruptedException e) {
        e.printStackTrace();
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        e.printStackTrace();
        // ignore, assuming they already exists
      }
    }

    log.info("******************************************\n");

  }

  public static void main(String[] args) {

//		loadLogbackFromDependencyJar();
    final Options options = OptionsUtil.parse(Options.class, args);

    final TopologyBuilder topologyBuilder = options.topologyBuilder();

    create(options.bootstrapServers(), topologyBuilder.topics(), topologyBuilder.applicationId());

//    final String tmpDir = System.getProperty("TMPDIR");
//    if (tmpDir != null) {
//      final File kafkaStreams = new File(tmpDir, "kafka-streams");
//      if (kafkaStreams.isDirectory() && kafkaStreams.canWrite()) {
//        final File stateDir = new File(kafkaStreams, topologyBuilder.applicationId());
//        if (stateDir.exists() || stateDir.isDirectory()) {
//
//          try {
//            Files.walk(stateDir.toPath())
//                    .sorted(Comparator.reverseOrder())
//                    .forEach(path -> {
//                      try {
//                        Files.delete(path);
//                        System.out.println("Deleted: " + path);
//                      } catch (IOException e) {
//                        System.err.println("Failed to delete " + path + " " + e.getMessage());
//                      }
//                    });
//          } catch (IOException e) {
//            throw new RuntimeException(e);
//          }
//
//          boolean deleted = stateDir.delete();
//          if (deleted) {
//            log.info("deleted state dir: {}", stateDir.getAbsolutePath());
//          } else {
//            log.warn("state dir: {} was not deleted!", stateDir.getAbsolutePath());
//          }
//        }
//      }
//    }

    Map<String, Object> config = topologyBuilder.properties();

//    Map<String, Object> config = KafkaStreamsConfigUtil.properties(
//            topologyBuilder.applicationId(),
//            options.optimization(),
//            options.numThreads(),
//            options.commitInterval()
//    );

    final Topology topology = topologyBuilder.topology();

//    Map<String, String> metadata = new LinkedHashMap<>();

//    metadata.put("caching", topologyBuilder.isCachingDisabled() ? "disabled" : "enabled");
//    metadata.put("feature", topologyBuilder.isFeatureDisabled() ? "disabled" : "enabled");
//
//    metadata.put("punctuationType", topologyBuilder.punctuationType().name());
//    metadata.put("emitStrategy", topologyBuilder.emitStrategy().toString());
//    metadata.put("windowConfig", topologyBuilder.windowConfig().toString());

    final KafkaStreamsRunner runner = new KafkaStreamsRunner(topologyBuilder.applicationId(), topology, config, topologyBuilder.metadata());

    runner.start();

//    runner.getStreams()
//    {
//      System.out.println("________");
//      runner.getStreams().metadataForLocalThreads().stream().forEach(s -> {
//        s.activeTasks().stream().forEach(t -> {
//          t.topicPartitions().stream().forEach(tp -> {
//            System.out.println(tp);
//          });
//        });
//      });
//      System.out.println("________");
//
//      System.out.println("x________");
//      runner.getStreams().metadataForAllStreamsClients().stream().forEach(s -> {
//        s.topicPartitions().stream().forEach(tp -> {
//          System.out.println(tp);
//        });
//      });
//      System.out.println("x________");
//    }


    //TODO StreamThread.lastCommitMs
    // StreamTask.partitionGroup.streamTime

//    try {
//      WebServer server = new WebServer(8080, topologyBuilder.applicationId(), topology, runner.getStreams());
//      server.start();
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
//
//    SimpleWebSocketServer wsServer = new SimpleWebSocketServer(8081);
//    wsServer.start();
//
//    System.out.println("STARTED");
//
//    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//      System.out.println("SHUTTING DOWN");
//      try {
//        wsServer.stop();
//        //server.stop(5000, "CLOSING!");
//      } catch (InterruptedException e) {
//        throw new RuntimeException(e);
//      }
//    }));
  }

  @SuppressWarnings("unchecked")
  private static TopologyBuilder load(String className) {
    try {
      final Class<?> cls = Class.forName(className);
      final Constructor<TopologyBuilder> constructor = (Constructor<TopologyBuilder>) cls.getDeclaredConstructor();
      return constructor.newInstance();
    } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }


  public static void loadLogbackFromDependencyJar() {
    // Get the current LoggerContext (from SLF4J)
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

    // Reset the current configuration
    context.reset();

    // Use the ClassLoader to find the logback.xml in the dependent JAR
    try (InputStream configStream = Main.class.getClassLoader().getResourceAsStream("logback.xml")) {
      if (configStream == null) {
        System.err.println("logback.xml not found in the dependent JAR!");
        return;
      }

      // Configure Logback with the retrieved configuration file
      JoranConfigurator configurator = new JoranConfigurator();
      configurator.setContext(context);
      configurator.doConfigure(configStream);

      System.out.println("Logback configuration loaded successfully from the dependent JAR.");
    } catch (JoranException e) {
      System.err.println("Error configuring Logback: " + e.getMessage());
      e.printStackTrace();
    } catch (Exception e) {
      System.err.println("Unable to load logback.xml: " + e.getMessage());
      e.printStackTrace();
    }
  }

  //

  private void clear(AdminClient admin) {
    truncateRecords(admin, TOPICS);
  }

  private static void truncateRecords(final AdminClient admin, final List<String> topics) {

    List<TopicPartition> topicPartitions = topicPartitions(admin, topics);
    final Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestOffsets = getLatest(admin, topicPartitions);
    deleteRecords(admin, latestOffsets);
  }

  private static void deleteRecords(final AdminClient admin, Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestOffsets) {
    try {
      admin.deleteRecords(generateRecordsToDelete(latestOffsets)).all().get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("deleteRecords operation interrupted, deleteRecords will not executed.");
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }


  private static List<TopicPartition> topicPartitions(final AdminClient admin, final List<String> topics) {
    try {
      return admin.describeTopics(topics).allTopicNames().get().entrySet().stream()
              .flatMap(entry -> {
                String topicName = entry.getKey();
                TopicDescription topicDescription = entry.getValue();
                return topicDescription.partitions().stream()
                        .map(info -> new TopicPartition(topicName, info.partition()));
              })
              .collect(Collectors.toList());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("describeTopics operation interrupted, deleteRecords will not executed.");
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }


  private static Map<TopicPartition, RecordsToDelete> generateRecordsToDelete(Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestOffsets) {
    return latestOffsets.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> RecordsToDelete.beforeOffset(entry.getValue().offset())));
  }


  private static Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> getLatest(AdminClient admin, List<TopicPartition> partitions) {
    Map<TopicPartition, OffsetSpec> input = partitions.stream().collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));
    return getOffsets(admin, input);
  }


  private static Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> getOffsets(AdminClient admin, Map<TopicPartition, OffsetSpec> offsetSpecs) {
    try {
      return admin.listOffsets(offsetSpecs).all().get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("listOffsets operation interrupted, deleteRecords will not executed.");
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }


}

