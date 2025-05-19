package io.kineticedge.kstutorial.common.streams.metadata;

import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.*;
import org.apache.kafka.streams.state.internals.MeteredVersionedKeyValueStore;

public class StoreTypeChecker {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StoreTypeChecker.class);

  public static StateStoreType checkStoreType(KafkaStreams streams, String storeName) {

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

      return StateStoreType.NA;

    } catch (Exception e) {
      return StateStoreType.NA;
    }
  }
}