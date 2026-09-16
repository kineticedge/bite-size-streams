package io.kineticedge.ks201;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.domain.OSProcess;
import io.kineticedge.kstutorial.domain.OSWindow;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.api.ContextualFixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorSupplier;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.VersionedKeyValueStore;
import org.apache.kafka.streams.state.VersionedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class VersionedStore2 extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(VersionedStore2.class);

  private static final String STORE_NAME = "versioned-store";

  private static final String OUTPUT_TOPIC = "versioned-store-output";

//  public static class storeUpdater extends ContextualFixedKeyProcessor<String, OSProcess, OSProcess> {
//    @Override
//    public void process(FixedKeyRecord<String, OSProcess> record) {
//      VersionedKeyValueStore<String, OSProcess> store = context().getStateStore(STORE_NAME);
//      store.put(record.key(), record.value(), record.timestamp());
//      context().forward(record);
//    }
//  }
//
//  public static class storeJoiner extends ContextualFixedKeyProcessor<String, OSWindow, String> {
//    @Override
//    public void process(FixedKeyRecord<String, OSWindow> record) {
//      VersionedKeyValueStore<String, OSProcess> store = context().getStateStore(STORE_NAME);
//      VersionedRecord<OSProcess> vr = store.get(record.key(), record.timestamp());
//      context().forward(record.withValue(String.format("windowId=%s, processName=%s, uptime=%,d",
//              record.value().windowId(), vr.value().name(), vr.value().upTime())));
//    }
//  }

  @FunctionalInterface
  interface StoreOperation<K, V> {
    void execute(K key, V value, VersionedKeyValueStore<K, V> store, long timestamp);
  }

  // Create a factory method for store processors
  private <K, V> FixedKeyProcessorSupplier<K, V, V> createStoreProcessor(String storeName, StoreOperation<K, V> operation) {

    return new FixedKeyProcessorSupplier<>() {
      @Override
      public ContextualFixedKeyProcessor<K, V, V> get() {
        return new ContextualFixedKeyProcessor<>() {
          @Override
          public void process(FixedKeyRecord<K, V> record) {
            VersionedKeyValueStore<K, V> store = context().getStateStore(storeName);
            operation.execute(record.key(), record.value(), store, record.timestamp());
            context().forward(record);
          }
        };
      }

      @Override
      public Set<StoreBuilder<?>> stores() {
        StoreBuilder<?> versionedStoreBuilder = Stores.versionedKeyValueStoreBuilder(
                Stores.persistentVersionedKeyValueStore(storeName, windowConfig().retention().orElse(Duration.ofMinutes(1L))),
                null, null);
        return Set.of(versionedStoreBuilder);
      }
    };
  }


  @FunctionalInterface
  interface StoreJoiner<V, VStore, VOut> {
    VOut apply(V value, VStore storeValue);
  }

  private ContextualFixedKeyProcessor<String, OSWindow, String> joiner(StoreJoiner<OSWindow, OSProcess, String> joiner) {
    return new ContextualFixedKeyProcessor<>() {
      @Override
      public void process(FixedKeyRecord<String, OSWindow> record) {
        VersionedKeyValueStore<String, OSProcess> store = context().getStateStore(STORE_NAME);
        VersionedRecord<OSProcess> vr = store.get(record.key(), record.timestamp());
        String result = joiner.apply(record.value(), vr.value());
        context().forward(record.withValue(result));
      }
    };
  }


  @Override
  public String applicationId() {
    return "versioned-store";
  }

  @Override
  protected void build(StreamsBuilder builder) {

    KStream<String, OSProcess> processes = builder
            .<String, OSProcess>stream(Constants.PROCESSES)
            .processValues(
                    createStoreProcessor(STORE_NAME,
                            (key, value, store, timestamp) -> store.put(key, value, timestamp))
            );

    builder.<String, OSWindow>stream(Constants.WINDOWS, Consumed.as("windows-source"))
            .selectKey((k, v) -> "" + v.processId())
            .repartition()
            .processValues(() -> joiner((window, process) ->
                    String.format("windowId=%s, processName=%s, uptime=%,d", window.windowId(), process.name(), process.upTime())
            ), STORE_NAME)
            .to(OUTPUT_TOPIC, Produced.<String, String>as("output-to").withValueSerde(Serdes.String()));
  }

  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }
}
