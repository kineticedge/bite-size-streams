package io.kineticedge.ks301;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.domain.OSProcess;
import io.kineticedge.kstutorial.domain.OSWindow;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.api.ContextualFixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorSupplier;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@SuppressWarnings("unused")
public class RangeQuery extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(RangeQuery.class);

  private static final String OUTPUT_TOPIC = "range-scan-output";

  @Override
  public String applicationId() {
    return "range-scan";
  }

  @Override
  protected void build(StreamsBuilder builder) {

    final String storeName = "range-store";

    final Serializer<String> prefixScanSerializer = new StringSerializer();

    StoreBuilder<KeyValueStore<String, OSWindow>> storeBuilder = Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(storeName), null,  null);

    builder.<String, OSWindow>stream(Constants.WINDOWS)
            .selectKey((k, v) -> "" + v.processId())
            .repartition()
            .peek(RangeQuery::print)
            .processValues(new FixedKeyProcessorSupplier<String, OSWindow, OSWindow>() {
              public ContextualFixedKeyProcessor<String, OSWindow, OSWindow> get() {
                return new ContextualFixedKeyProcessor<>() {
                  public void process(FixedKeyRecord<String, OSWindow> record) {
                    KeyValueStore<String, OSWindow> store = context().getStateStore(storeName);
                    store.put(record.key() + "_" + record.value().windowId(), record.value());
                    context().forward(record.withValue(record.value()));
                  }
                };
              }
              public Set<StoreBuilder<?>> stores() {
                return Set.of(storeBuilder);
              }
            });

    builder
            .<String, OSProcess>stream(Constants.PROCESSES)
            .peek(RangeQuery::print)
            .processValues(() -> new ContextualFixedKeyProcessor<String, OSProcess, String>() {
              public void process(FixedKeyRecord<String, OSProcess> record) {
                KeyValueStore<String, OSWindow> store = context().getStateStore(storeName);
                Set<Long> windowIds = new TreeSet<>();
                try (KeyValueIterator<String, OSWindow> it = store.prefixScan(record.key() + "_", prefixScanSerializer)) {
                  while (it.hasNext()) {
                    OSWindow window = it.next().value;
                    windowIds.add(window.windowId());
                    context().forward(record.withValue(convert(record.value(), window)));
                  }
                  if (!windowIds.isEmpty()) {
                    context().forward(record.withValue(convert(record.value(), windowIds)));
                  }
                }
              }
            }, storeName)
            .to(OUTPUT_TOPIC, Produced.with(null, Serdes.String()));
  }

  private static String convert(final OSProcess process, final OSWindow window) {
    return ":individual: name=" + process.name() + ", processId=" + process.processId() + ", windowId=" + window.windowId();
  }

  private static String convert(final OSProcess process, final Set<Long> windowIds) {
    return ":composite: name=" + process.name()  + ", processId=" + process.processId() + ", windowIds=" + windowIds;
  }

  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }
}
