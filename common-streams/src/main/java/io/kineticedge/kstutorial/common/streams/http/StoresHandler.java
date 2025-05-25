package io.kineticedge.kstutorial.common.streams.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.kineticedge.kstutorial.common.streams.metadata.StateStoreType;
import io.kineticedge.kstutorial.common.streams.metadata.StoreTypeChecker;
import io.kineticedge.kstutorial.common.util.JsonUtil;
import io.kineticedge.kstutorial.common.util.MapUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.kstream.internals.SessionWindow;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ReadOnlySessionStore;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class StoresHandler implements HttpHandler {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StoresHandler.class);

  private final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

  private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
  };

  private final KafkaStreams kafkaStreams;

  public StoresHandler(final KafkaStreams kafkaStreams) {
    this.kafkaStreams = kafkaStreams;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String path = exchange.getRequestURI().getPath();
    String store = getStoreFromPath(path);
    Integer partition = getPartitionFromPath(path);

    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.getResponseHeaders().set("Cache-Control", "no-cache");
    exchange.getResponseHeaders().set("Connection", "keep-alive");
    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
    exchange.sendResponseHeaders(200, 0);

    StateStoreType type = StoreTypeChecker.checkStoreType(kafkaStreams, store);

    log.info("!!!! getting state store {} partition {} type {}", store, partition, type);

    switch (type) {
      case SESSION -> sessionStore(exchange, store, partition);
      case WINDOWED -> windowStore(exchange, store, partition);
      case TIMESTAMPED_KEYVALUE -> timestampedKeyvalue(exchange, store, partition);
      case TIMESTAMPED_KEYVALUE_BYTESKEY -> timestampedKeyvalueFK(exchange, store, partition);
      case KEYVALUE -> keyvalue(exchange, store, partition);
      default -> {
        System.out.println(">>>>!!!!>>> " + type);
        exchange.close();
      }
    }

//    // SESSION WINDOW --
////    if (true) {
////      x(exchange, store, partition);
////      return;
////    }
//
//    StoreQueryParameters<ReadOnlyKeyValueStore<String, ValueAndTimestamp<Object>>> parameters = StoreQueryParameters.fromNameAndType(store, QueryableStoreTypes.timestampedKeyValueStore());
//    if (partition != null) {
//      log.debug("getting state store {} partition {}", store, partition);
//      parameters = parameters.withPartition(partition);
//    }
//
//    ReadOnlyKeyValueStore<String, ValueAndTimestamp<Object>> stateStore = kafkaStreams.store(parameters);
//
//    try (OutputStream os = exchange.getResponseBody()) {
//
//      os.write("[".getBytes());
//      try (KeyValueIterator<String, ValueAndTimestamp<Object>> storeIterator = stateStore.all()) {
//        while (storeIterator.hasNext()) {
//          var entry = storeIterator.next();
//          final Pair<Object, String> output = convert(entry.value.value());
//          final Map<String, Object> record = Map.ofEntries(
//                  Map.entry("key", entry.key),
//                  Map.entry("timestamp", entry.value.timestamp()),
//                  Map.entry("value", output.getLeft()),
//                  Map.entry("type", output.getRight())
//          );
//
//          os.write(JsonUtil.objectMapper().writeValueAsBytes(record));
//
//          if (storeIterator.hasNext()) {
//            os.write(",".getBytes());
//          }
//        }
//      } catch (Exception e) {
//        log.error("unable to parse", e);
//      }
//      os.write("]".getBytes());
//    } finally {
//      exchange.close();
//    }

  }

  private void timestampedKeyvalue(HttpExchange exchange, String store, Integer partition) throws IOException {
    StoreQueryParameters<ReadOnlyKeyValueStore<Object, ValueAndTimestamp<Object>>> parameters = StoreQueryParameters.fromNameAndType(store, QueryableStoreTypes.timestampedKeyValueStore());
    if (partition != null) {
      log.debug("getting state store {} partition {}", store, partition);
      parameters = parameters.withPartition(partition);
    }

    ReadOnlyKeyValueStore<Object, ValueAndTimestamp<Object>> stateStore = kafkaStreams.store(parameters);

    try (OutputStream os = exchange.getResponseBody()) {

      os.write("[".getBytes());
      try (KeyValueIterator<Object, ValueAndTimestamp<Object>> storeIterator = stateStore.all()) {
        while (storeIterator.hasNext()) {
          var entry = storeIterator.next();
          final String key = entry.key instanceof String ? (String) entry.key : new String((byte[]) entry.key);
          final Pair<Object, String> output = convert(entry.value.value());
          final Map<String, Object> record = Map.ofEntries(
                  Map.entry("key", key),
                  Map.entry("timestamp", entry.value.timestamp()),
                  Map.entry("value", output.getLeft()),
                  Map.entry("type", output.getRight())
          );

          os.write(JsonUtil.objectMapper().writeValueAsBytes(record));

          if (storeIterator.hasNext()) {
            os.write(",".getBytes());
          }
        }
      } catch (Exception e) {
        log.error("unable to parse", e);
      }
      os.write("]".getBytes());
    } finally {
      exchange.close();
    }
  }


  private void timestampedKeyvalueFK(HttpExchange exchange, String store, Integer partition) throws IOException {
    StoreQueryParameters<ReadOnlyKeyValueStore<Bytes, ValueAndTimestamp<Object>>> parameters = StoreQueryParameters.fromNameAndType(store, QueryableStoreTypes.timestampedKeyValueStore());
    if (partition != null) {
      log.debug("getting state store {} partition {}", store, partition);
      parameters = parameters.withPartition(partition);
    }

    ReadOnlyKeyValueStore<Bytes, ValueAndTimestamp<Object>> stateStore = kafkaStreams.store(parameters);

    try (OutputStream os = exchange.getResponseBody()) {

      os.write("[".getBytes());
      try (KeyValueIterator<Bytes, ValueAndTimestamp<Object>> storeIterator = stateStore.all()) {
        while (storeIterator.hasNext()) {
          var entry = storeIterator.next();
          final Pair<Object, String> output = convert(entry.value.value());
          final Map<String, Object> record = Map.ofEntries(
                  Map.entry("key", ">>|" +entry.key.toString()),
                  Map.entry("timestamp", entry.value.timestamp()),
                  Map.entry("value", output.getLeft()),
                  Map.entry("type", output.getRight())
          );

          //TODO writing this as a string implies all state-stores are strings -- reconsider.
          os.write(JsonUtil.objectMapper().writeValueAsBytes(record));

          if (storeIterator.hasNext()) {
            os.write(",".getBytes());
          }
        }
      } catch (Exception e) {
        log.error("unable to parse", e);
      }
      os.write("]".getBytes());
    } finally {
      exchange.close();
    }
  }


  private void keyvalue(HttpExchange exchange, String store, Integer partition) throws IOException {
    StoreQueryParameters<ReadOnlyKeyValueStore<String, Object>> parameters = StoreQueryParameters.fromNameAndType(store, QueryableStoreTypes.keyValueStore());
    if (partition != null) {
      log.debug("getting state store {} partition {}", store, partition);
      parameters = parameters.withPartition(partition);
    }

    ReadOnlyKeyValueStore<String, Object> stateStore = kafkaStreams.store(parameters);

    try (OutputStream os = exchange.getResponseBody()) {

      os.write("[".getBytes());
      try (KeyValueIterator<String, Object> storeIterator = stateStore.all()) {
        while (storeIterator.hasNext()) {
          var entry = storeIterator.next();
          final Pair<Object, String> output = convert(entry.value);
          final Map<String, Object> record = Map.ofEntries(
                  Map.entry("key", entry.key),
                  //Map.entry("timestamp", 0),
                  Map.entry("value", output.getLeft()),
                  Map.entry("type", output.getRight())
          );

          os.write(JsonUtil.objectMapper().writeValueAsBytes(record));

          if (storeIterator.hasNext()) {
            os.write(",".getBytes());
          }
        }
      } catch (Exception e) {
        log.error("unable to parse", e);
      }
      os.write("]".getBytes());
    } finally {
      exchange.close();
    }
  }

  private void sessionStore(HttpExchange exchange, String store, Integer partition) throws IOException {

    StoreQueryParameters<ReadOnlySessionStore<String, Object>> parameters = StoreQueryParameters.fromNameAndType(store, QueryableStoreTypes.sessionStore());
    if (partition != null) {
      log.debug("getting state store {} partition {}", store, partition);
      parameters = parameters.withPartition(partition);
    }

    ReadOnlySessionStore<String, Object> stateStore = kafkaStreams.store(parameters);

    try (OutputStream os = exchange.getResponseBody()) {

      os.write("[".getBytes());
      try (KeyValueIterator<Windowed<String>, Object> storeIterator = stateStore.findSessions(null, null, 0, Long.MAX_VALUE)) {
        while (storeIterator.hasNext()) {
          var entry = storeIterator.next();
          final Pair<Object, String> output = convert(entry.value);
          final Map<String, Object> record = Map.ofEntries(
                  Map.entry("key", entry.key.key() + "&nbsp;[" + formatEpoch(entry.key.window().start()) + ",&nbsp;" + formatEpoch(entry.key.window().end()) + "]"),
                  Map.entry("timestamp", System.currentTimeMillis()),
                  Map.entry("value", output.getLeft()),
                  Map.entry("type", output.getRight())
          );

          os.write(JsonUtil.objectMapper().writeValueAsBytes(record));

          if (storeIterator.hasNext()) {
            os.write(",".getBytes());
          }
        }
      } catch (Exception e) {
        log.error("unable to parse", e);
      }
      os.write("]".getBytes());
    } finally {
      exchange.close();
    }

  }

  private void windowStore(HttpExchange exchange, String store, Integer partition) throws IOException {

    StoreQueryParameters<ReadOnlyWindowStore<String, Object>> parameters = StoreQueryParameters.fromNameAndType(store, QueryableStoreTypes.windowStore());
    if (partition != null) {
      log.debug("getting state store {} partition {}", store, partition);
      parameters = parameters.withPartition(partition);
    }

    ReadOnlyWindowStore<String, Object> stateStore = kafkaStreams.store(parameters);

    try (OutputStream os = exchange.getResponseBody()) {

      os.write("[".getBytes());
      try (KeyValueIterator<Windowed<String>, Object> storeIterator = stateStore.all()) {
        while (storeIterator.hasNext()) {
          var entry = storeIterator.next();
          final Pair<Object, String> output = convert(entry.value);
          final Map<String, Object> record = Map.ofEntries(
                  Map.entry("key", entry.key.key() + "&nbsp;[" + formatEpoch(entry.key.window().start()) + ",&nbsp;" + formatEpoch(entry.key.window().end()) + "]"),
                  Map.entry("timestamp", System.currentTimeMillis()),
                  Map.entry("value", output.getLeft()),
                  Map.entry("type", output.getRight())
          );

          os.write(JsonUtil.objectMapper().writeValueAsBytes(record));

          if (storeIterator.hasNext()) {
            os.write(",".getBytes());
          }
        }
      } catch (Exception e) {
        log.error("unable to parse", e);
      }
      os.write("]".getBytes());
    } finally {
      exchange.close();
    }

  }


  private static Pair<Object, String> convert(Object object) {

    if (object == null) {
      return Pair.of(null, "null");
    } else if (object instanceof Number) {
      return Pair.of(object, object.getClass().getSimpleName());
    }

    try {
      final Map<String, Object> output = JsonUtil.objectMapper().convertValue(object, MAP_TYPE_REFERENCE);

      String type = (String) output.get("_type");
      type = type.substring(type.lastIndexOf('.') + 1);

      MapUtil.removeKeyRecursively(output, "_type");

      return Pair.of(output, type);
    } catch (Exception e) {
      return Pair.of(object.toString(), object.getClass().getSimpleName());
    }
  }

  private static String getStoreFromPath(String path) {
    // Check if the path starts with "/events/"
    if (path.startsWith("/stores/")) {
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
    if (path.startsWith("/stores/")) {
      String[] parts = path.split("/"); // Split path into segments
      if (parts.length > 3) {
        return Integer.parseInt(parts[3]); // Return the partition
      }
    }

    // No topic found
    return null;
  }

  private static String formatEpoch(long epoch) {
    return FORMATTER.format(Instant.ofEpochMilli(epoch));
  }

}
