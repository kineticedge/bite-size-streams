package io.kineticedge.kstutorial.common.streams.ws;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kineticedge.kstutorial.common.streams.util.MagicKeyParser;
import io.kineticedge.kstutorial.common.util.JsonUtil;
import io.kineticedge.kstutorial.common.util.XmlUtil;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.IsolationLevel;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.helpers.AttributesImpl;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

public class EventsHandler  {

  private static final String BOOTSTRAP_SERVERS = "localhost:9092";

  private static final Logger log = LoggerFactory.getLogger(EventsHandler.class);

  private static final LongDeserializer deserializer = new LongDeserializer();

  private final WebSocket webSocket;

  private List<TopicPartition> tps;

  private boolean running = true;

  private AtomicBoolean rewind = new AtomicBoolean(false);

  public EventsHandler(final WebSocket webSocket) {
    this.webSocket = webSocket;
  }

  private Map<String, Object> adminConfig() {
    Map<String, Object> map = new HashMap<>();
    map.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
    return map;
  }

  private Map<String, Object> config() {
    Map<String, Object> map = new HashMap<>();
    map.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
    //map.put(ConsumerConfig.GROUP_ID_CONFIG, "sse-consumer-group");
    map.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
    map.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
    map.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    map.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, IsolationLevel.READ_COMMITTED.toString());
    return map;
  }

  private int partitionCount(final String topic) {
    try (Admin admin = Admin.create(adminConfig())) {
      try {
        TopicDescription description = admin.describeTopics(List.of(topic)).topicNameValues().get(topic).get();
        return description.partitions().size();
      } catch (ExecutionException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private List<TopicPartition> partitions(final String topic, final Integer partition) {
    if (partition == null) {
      return IntStream.range(0, partitionCount(topic))
              .mapToObj(p -> new TopicPartition(topic, p))
              .toList();
    } else {
      return List.of(new TopicPartition(topic, partition));
    }
  }

  public void close() {
    running = false;
  }

  public void rewind() {
    rewind.set(true);
  }

  public void handle(final String topic, final Integer partition)  {

    log.info("handle: topic={}, partition={}", topic, partition);

    this.tps = partitions(topic, partition);

    final KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(config());
    consumer.assign(tps);
    consumer.seekToEnd(tps);

    try {
      while (running) {

        if (rewind.getAndSet(false)) {
          consumer.seekToBeginning(tps);
        }

        ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofMillis(100L));
        for (ConsumerRecord<byte[], byte[]> record : records) {
          String vv = "";
          String datatype = "unknown";
          String type = null;
          if (isJson(record.value())) {
            datatype = "JSON";
            Map<String, Object> value = JsonUtil.objectMapper().readValue(record.value(), new TypeReference<>() {});
            type = value.get("_type") != null ? (String) value.get("_type") : "<unknown (json)>";
            if (type != null && type.contains(".")) {
              type = type.substring(type.lastIndexOf('.') + 1);
            }
            //TODO MapUtil.removeKeyRecursively(value, "_type");
            vv = new String(record.value());
          } else if (isJsonArray(record.value())) {
            datatype = "JSON";
            List<Map<String, Object>> value = JsonUtil.objectMapper().readValue(record.value(), new TypeReference<>() {});
            //vv = new String(JsonUtil.objectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(value));
            vv = new String(record.value());
          } else if (isXml(record.value())) {
            datatype = "XML";
            Map<String, Object> value = XmlUtil.objectMapper().readValue(record.value(), new TypeReference<>() {});
            type = value.get("_type") != null ? (String) value.get("_type") : "<unknown (xml)>";
            if (type != null && type.contains(".")) {
              type = type.substring(type.lastIndexOf('.') + 1);
            }
            vv = new String(record.value());
          } else if (record.value() != null && record.value().length == 4) {
            type = "Integer";
            Integer integer = new IntegerDeserializer().deserialize("", record.value());
            vv = String.valueOf(integer);
          } else if (record.value() != null && record.value().length == 8) {
            type = "Long";
            Long lng = new LongDeserializer().deserialize("", record.value());
            vv = String.valueOf(lng);
          } else if (record.value() == null) {
            type = "null";
            vv = "<em>null</em>";
          } else {
            type = "String";
            //TODO HTML ENCODE!
            vv = new String(record.value());
          }

          if (type == null) {
            type = "<unknown>";
          }

          String key = record.key() != null ? MagicKeyParser.parse(record.key()) : "<em>null</em>";

          long timestamp = -1;
          byte[] ts = record.headers().lastHeader("ts").value();
          if (ts != null) {
            timestamp = deserializer.deserialize(null, ts);
          }

          Map<String, Object> data = Map.ofEntries(
                  Map.entry("key", key),
                  Map.entry("partition", record.partition()),
                  Map.entry("offset", record.offset()),
                  Map.entry("timestamp", record.timestamp()),
                  Map.entry("ptimestamp", timestamp),
                  Map.entry("datatype", datatype),
                  Map.entry("type", type),
                  //Map.entry("value", MapUtil.removeKeyRecursively(value, "_type"))
                  Map.entry("value", vv)
          );

          webSocket.send(JsonUtil.objectMapper().writeValueAsString(data));
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      consumer.close();
    }

  }

  private static String getTopicFromPath(String path) {
    // Check if the path starts with "/events/"
    if (path.startsWith("/events/")) {
      String[] parts = path.split("/"); // Split path into segments
      if (parts.length > 2) {
        return parts[2]; // Return the topic (third segment)
      }
    }

    // No topic found
    return null;
  }

  private static Integer getPartitionFromPath(String path) {
    // Check if the path starts with "/events/"
    if (path.startsWith("/events/")) {
      String[] parts = path.split("/"); // Split path into segments
      if (parts.length > 3) {
        return Integer.parseInt(parts[3]); // Return the partition
      }
    }

    // No topic found
    return null;
  }


  private static boolean isXml(final byte[] value) {
    return value != null && value.length >= 1 && value[0] == '<';
  }

  private static boolean isJson(final byte[] value) {
    return value != null && value.length >= 1 && (value[0] == '{');
  }

  private static boolean isJsonArray(final byte[] value) {
    return value != null && value.length >= 1 && (value[0] == '[');
  }

  private static String formatEpoch(long epochMillis) {
    // Convert epoch millis to Instant
    Instant instant = Instant.ofEpochMilli(epochMillis);

    // Convert to LocalDateTime using system default time zone
    LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

    // Format the LocalDateTime to a string
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    return dateTime.format(formatter);
  }

}
