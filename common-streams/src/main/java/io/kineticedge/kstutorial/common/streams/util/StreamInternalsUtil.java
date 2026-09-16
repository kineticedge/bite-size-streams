package io.kineticedge.kstutorial.common.streams.util;

import io.kineticedge.kstutorial.common.streams.metadata.SubtopologyInfo;
import io.kineticedge.kstutorial.common.streams.metadata.ThreadInfo;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.processor.TaskId;
import org.apache.kafka.streams.processor.internals.StreamTask;
import org.apache.kafka.streams.processor.internals.StreamThread;
import org.apache.kafka.streams.processor.internals.TaskManager;
import org.apache.kafka.streams.processor.internals.TasksRegistry;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class StreamInternalsUtil {

  private static final Pattern THREAD_SIMPLIFIER = Pattern.compile("-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());


  private StreamInternalsUtil() {
  }

  public static void x(KafkaStreams kafkaStreams) {

  }

//  public static List<StoreInfo> storeMetadata(KafkaStreams streams) {
//
//    Collection<StreamsMetadata> x = streams.streamsMetadataForStore("f");
//
//    streams.metadataForAllStreamsClients().forEach(streamsMetadata -> {
//      streamsMetadata.stateStoreNames().forEach(storeName -> {
//
//        streams.
//        Collection<StreamsMetadata> sm = streams.streamsMetadataForStore(storeName);
//        sm.forEach(m -> {
//          m.
//        });
//        //streams.store(StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.keyValueStore()))
//      });
//    });
//
//  }

  public static String printApplicationId(KafkaStreams streams) {
    try {
      final Field applicationConfigs = KafkaStreams.class.getDeclaredField("applicationConfigs");
      applicationConfigs.setAccessible(true);
      final StreamsConfig streamsConfig = (StreamsConfig) applicationConfigs.get(streams);
      return streamsConfig.getString(StreamsConfig.APPLICATION_ID_CONFIG);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      return "<unknown>";
    }
  }

  public static List<SubtopologyInfo> printStreamTimesBySubtopology(KafkaStreams streams) {

    List<SubtopologyInfo> list = new ArrayList<>();

    try {
      // Step 1: Access StreamThreads
      Field threadsField = KafkaStreams.class.getDeclaredField("threads");
      threadsField.setAccessible(true);
      List<StreamThread> streamThreads = (List<StreamThread>) threadsField.get(streams);

      Map<Integer, Long> subtopologyMinStreamTimes = new HashMap<>();

      for (StreamThread thread : streamThreads) {
        // Step 2: TaskManager access
        Field taskManagerField = StreamThread.class.getDeclaredField("taskManager");
        taskManagerField.setAccessible(true);
        TaskManager taskManager = (TaskManager) taskManagerField.get(thread);

        Field tasksRegistryField = taskManager.getClass().getDeclaredField("tasks");
        tasksRegistryField.setAccessible(true);
        TasksRegistry tasksRegistry = (TasksRegistry) tasksRegistryField.get(taskManager);

        // Step 3: Iterate through tasks and access stream times
        for (Object task : tasksRegistry.activeTasks()) {
          if (task instanceof StreamTask streamTask) {

            // Access PartitionGroup
            Field partitionGroupField = StreamTask.class.getDeclaredField("partitionGroup");
            partitionGroupField.setAccessible(true);
            Object partitionGroup = partitionGroupField.get(streamTask);

            // Retrieve stream time (minTimestamp)
            Field minTimestampField = partitionGroup.getClass().getDeclaredField("streamTime");
            minTimestampField.setAccessible(true);
            long streamTime = (long) minTimestampField.get(partitionGroup);

            // Map stream time by subtopology
            TaskId taskId = streamTask.id();
            int subtopologyId = taskId.subtopology();
            subtopologyMinStreamTimes.merge(subtopologyId, streamTime, Math::max);

            list.add(new SubtopologyInfo(taskId.toString(),streamTime));

          }
        }
      }

//      // Print Stream Times by Subtopology
//      //System.out.println("Stream Times by Subtopology : " + subtopologyMinStreamTimes);
//      for (Map.Entry<Integer, Long> entry : subtopologyMinStreamTimes.entrySet()) {
//        //System.out.println("Subtopology: " + entry.getKey() + ", Min Stream Time: " + Instant.ofEpochMilli(entry.getValue()));
//        list.add(new SubtopologyInfo(entry.getKey(), entry.getValue()));
//      }
    } catch (NoSuchFieldException | IllegalAccessException e) {
      e.printStackTrace();
    }

    list.sort(Comparator.comparing(SubtopologyInfo::id));

    return list;
  }

  public static List<ThreadInfo> monitorLastCommit(KafkaStreams streams) {
    List<ThreadInfo> list = new ArrayList<>();

    try {
      // Step 1: Access private `threads` field in KafkaStreams
      Field threadsField = KafkaStreams.class.getDeclaredField("threads");
      threadsField.setAccessible(true);
      List<StreamThread> streamThreads = (List<StreamThread>) threadsField.get(streams);

      // Step 2: Access `lastCommit` field from each StreamThread
      Field lastCommitField = StreamThread.class.getDeclaredField("lastCommitMs");
      lastCommitField.setAccessible(true);

      for (StreamThread thread : streamThreads) {
        // Get the `lastCommitMs` value for each thread
        long lastCommitMs = (long) lastCommitField.get(thread);
        Instant lastCommitInstant = Instant.ofEpochMilli(lastCommitMs);

        Matcher matcher = THREAD_SIMPLIFIER.matcher(thread.getName());
        String result = matcher.replaceAll("");
        result = result.substring(result.lastIndexOf("-") + 1);

        // Step 2: TaskManager access
        Field taskManagerField = StreamThread.class.getDeclaredField("taskManager");
        taskManagerField.setAccessible(true);
        TaskManager taskManager = (TaskManager) taskManagerField.get(thread);

        Field tasksRegistryField = taskManager.getClass().getDeclaredField("tasks");
        tasksRegistryField.setAccessible(true);
        TasksRegistry tasksRegistry = (TasksRegistry) tasksRegistryField.get(taskManager);

        List<String> tasks = new ArrayList<>();
        // Step 3: Iterate through tasks and access stream times
        for (Object task : tasksRegistry.activeTasks()) {
          if (task instanceof StreamTask streamTask) {
            tasks.add(streamTask.id().toString());
          }
        }

        list.add(new ThreadInfo(result, lastCommitInstant.toEpochMilli(), String.join(", ", tasks)));
      }
    } catch (NoSuchFieldException | IllegalAccessException e) {
      e.printStackTrace();
    }

    return list;
  }
}
