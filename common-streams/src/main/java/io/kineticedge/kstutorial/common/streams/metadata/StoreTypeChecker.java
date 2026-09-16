package io.kineticedge.kstutorial.common.streams.metadata;

import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.processor.internals.ProcessorStateManager;
import org.apache.kafka.streams.processor.internals.StreamTask;
import org.apache.kafka.streams.processor.internals.StreamThread;
import org.apache.kafka.streams.processor.internals.Task;
import org.apache.kafka.streams.processor.internals.TaskManager;
import org.apache.kafka.streams.processor.internals.TasksRegistry;
import org.apache.kafka.streams.query.QueryConfig;
import org.apache.kafka.streams.query.QueryResult;
import org.apache.kafka.streams.query.RangeQuery;
import org.apache.kafka.streams.query.StateQueryRequest;
import org.apache.kafka.streams.query.StateQueryResult;
import org.apache.kafka.streams.state.*;
import org.apache.kafka.streams.state.internals.ChangeLoggingVersionedKeyValueBytesStore;
import org.apache.kafka.streams.state.internals.LogicalKeyValueSegments;
import org.apache.kafka.streams.state.internals.MeteredVersionedKeyValueStore;
import org.apache.kafka.streams.state.internals.QueryableStoreProvider;
import org.apache.kafka.streams.state.internals.RocksDBVersionedStore;
import org.apache.kafka.streams.state.internals.StreamThreadStateStoreProvider;
import org.apache.kafka.streams.state.internals.VersionedKeyValueToBytesStoreAdapter;
import org.rocksdb.RocksDB;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class StoreTypeChecker {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StoreTypeChecker.class);

  public static StateStoreType checkStoreType(KafkaStreams streams, String storeName) {

//    try {
//
//
//      StateQueryRequest<KeyValueIterator<String, String>> request =
//              StateQueryRequest.inStore(storeName)
//                      .withQuery(RangeQuery.withNoBounds());
//                      //.withQueryConfig(QueryConfig.withPartition(0)); // optional: target specific partition
//      StateQueryResult<KeyValueIterator<String, String>> result = streams.query(request);
//      QueryResult<KeyValueIterator<String, String>> partitionResult = result.getPartitionResults().get(0);
//      if (partitionResult.isSuccess()) {
//        try (KeyValueIterator<String, String> iterator = partitionResult.getResult()) {
//          while (iterator.hasNext()) {
//            var entry = iterator.next();
//            System.out.println("Key: " + entry.key + ", Value: " + entry.value);
//          }
//        }
//      } else {
//        System.out.println("Query failed: " + partitionResult.getFailureReason());
//      }
//
//
//
//      Field f = KafkaStreams.class.getDeclaredField("threads");
//      f.setAccessible(true);
//      List<StreamThread> threads = (List<StreamThread>) f.get(streams);
//
//      Field ff = StreamThread.class.getDeclaredField("taskManager");
//      ff.setAccessible(true);
//      TaskManager taskManager = (TaskManager) ff.get(threads.get(0));
//
//      Field fff = TaskManager.class.getDeclaredField("tasks");
//      fff.setAccessible(true);
//
//      TasksRegistry tasksRegistry = (TasksRegistry) fff.get(taskManager);
//
//      Collection<Task> tasks = tasksRegistry.activeTasks();
//
//      for (Task task : tasks) {
//        if (task instanceof StreamTask) {
//          ProcessorStateManager stateManager = ((StreamTask) task).stateManager();
//          StateStore store = stateManager.store(storeName);
//          System.out.println(store.name() + "____" + store.getClass().getName());
//
//          if (store instanceof VersionedKeyValueStore) {
//            MeteredVersionedKeyValueStore<?, ?> versionedStore = (MeteredVersionedKeyValueStore<?, ?>) store;
//            // Now you can call get(key, asOfTimestamp)
//
//            System.out.println("*!!!!!!!!!!!!!!!!!!!!*");
//
//            ChangeLoggingVersionedKeyValueBytesStore x = ((ChangeLoggingVersionedKeyValueBytesStore) versionedStore.wrapped());
//
//            Field ffff = ChangeLoggingVersionedKeyValueBytesStore.class.getDeclaredField("inner");
//            ffff.setAccessible(true);
//            VersionedKeyValueToBytesStoreAdapter y = (VersionedKeyValueToBytesStoreAdapter) ffff.get(x);
//
//            Field fffff = VersionedKeyValueToBytesStoreAdapter.class.getDeclaredField("inner");
//            fffff.setAccessible(true);
//            RocksDBVersionedStore rocksDBVersionedStore = (RocksDBVersionedStore) fffff.get(y);
//
//
//            Field latestValueStoreField = RocksDBVersionedStore.class.getDeclaredField("latestValueStore");
//            latestValueStoreField.setAccessible(true);
//
//            Object latestValueStore = latestValueStoreField.get(rocksDBVersionedStore);
//
//            Method rangeMethod = latestValueStore.getClass().getMethod("range", Object.class, Object.class);
//            rangeMethod.setAccessible(true);
//
//            LongDeserializer longDeserializer = new LongDeserializer();
//
//            try (KeyValueIterator<Bytes, byte[]> i = (KeyValueIterator<Bytes, byte[]>) rangeMethod.invoke(latestValueStore, null, null)) {
//              while (i.hasNext()) {
//                var kv = i.next();
//
//                System.out.println("KEY_LENGTH >> " + kv.key.get().length);
//                System.out.println("VALUE_LENGTH >> " + kv.value.length);
//                System.out.println(new String(kv.key.get()) + " _ " + new String(kv.value));
//
//                byte[] ts = new byte[8];
//                System.arraycopy(kv.value, 0, ts, 0, 8);
//
//                Long l = longDeserializer.deserialize("", ts);
//                System.out.println(Instant.ofEpochMilli(l));
//
//                for (int ii =0; ii < 16; ii++) {
//                  System.out.println(ii + " " + kv.value[ii] + " " + (char) kv.value[ii]);
//                }
//              }
//            }
//          }
//        }
//      }
//
////      try (VersionedKeyValueStore.KeyQuery<String, String> query = versionedStore.query()) {
////        try (KeyValueIterator<K, VersionedRecord<V>> iterator = query.all()) {
////          while (iterator.hasNext()) {
////            KeyValue<K, VersionedRecord<V>> entry = iterator.next();
////            K key = entry.key;
////            V value = entry.value.value(); // latest value
////            long timestamp = entry.value.timestamp();
////
////            System.out.println("Key: " + key + ", Value: " + value + ", Timestamp: " + timestamp);
////          }
////        }
////      }
//
////      Field field = KafkaStreams.class.getDeclaredField("queryableStoreProvider");
////      field.setAccessible(true);
////      QueryableStoreProvider queryableStoreProvider = (QueryableStoreProvider) field.get(streams);
////
////      System.out.println(queryableStoreProvider.getClass().getName());
////
////      Field field2 = QueryableStoreProvider.class.getDeclaredField("storeProviders");
////      field2.setAccessible(true);
////      Map<String, StreamThreadStateStoreProvider> storeProviders = (Map<String, StreamThreadStateStoreProvider>) field2.get(queryableStoreProvider);
////
////      System.out.println("***");
////      System.out.println("***");
////      storeProviders.forEach((k, v) -> { System.out.println(k + " : " + v.getClass().getName());});
////
////      StreamThreadStateStoreProvider provider = storeProviders.values().stream().findAny().get();
//
//
//
//      System.out.println("***");
//      System.out.println("***");
//
//    } catch (Exception e) {
//      log.error("Error getting store providers", e);
//    }

    try {

      try {
        StoreQueryParameters<ReadOnlyKeyValueStore<Object, ValueAndTimestamp<Object>>> storeQueryParameters = StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.timestampedKeyValueStore());

        final ReadOnlyKeyValueStore<Object, ValueAndTimestamp<Object>> store = streams.store(storeQueryParameters);
        try (KeyValueIterator<Object, ValueAndTimestamp<Object>> it = store.all()) {
          if (it.hasNext()) {
            KeyValue<Object, ValueAndTimestamp<Object>> x = it.next();

            if (x.key instanceof Bytes) {
              return StateStoreType.TIMESTAMPED_KEYVALUE_BYTESKEY;
            }

            return StateStoreType.TIMESTAMPED_KEYVALUE;
          }
        }
      } catch (Exception e) {
          log.error("Error getting store providers", e);
      }

      try {
        StoreQueryParameters<ReadOnlyKeyValueStore<Object, Object>> storeQueryParameters = StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.keyValueStore());

        final ReadOnlyKeyValueStore<Object, Object> store = streams.store(storeQueryParameters);
        store.get("");
        return StateStoreType.KEYVALUE;
      } catch (Exception e) {
      }

      try {
        final StoreQueryParameters<ReadOnlyWindowStore<Object, Object>> windowParams = StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.windowStore());
        var store = streams.store(windowParams);
        store.fetch("", System.currentTimeMillis());
        return StateStoreType.WINDOWED;
      } catch (Exception e) {
      }

      try {
        final StoreQueryParameters<ReadOnlySessionStore<Object, Object>> sessionParams = StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.sessionStore());
        var store = streams.store(sessionParams);
        store.fetchSession("", System.currentTimeMillis(), System.currentTimeMillis());
        return StateStoreType.SESSION;
      } catch (Exception e) {
      }

      log.info("*********** ASSUMING VERSIONED KEYVALUE");
      return StateStoreType.VERSIONED_KEYVALUE;
//      return StateStoreType.TIMESTAMPED_KEYVALUE;
//      return StateStoreType.TIMESTAMPED_KEYVALUE;

    } catch (Exception e) {
      return StateStoreType.NA;
    }
  }
}