package io.kineticedge.ks301;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.common.serde.JsonSerde;
import io.kineticedge.kstutorial.domain.OSProcess;
import io.kineticedge.kstutorial.domain.OSWindow;
import io.kineticedge.kstutorial.domain.OSWindowList;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Suppressed;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.ValueTransformerWithKey;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.DslStoreSuppliers;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.ValueAndTimestamp;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class MicroBatch extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(MicroBatch.class);

  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  private static final String OUTPUT_TOPIC = "microbatch";

  @Override
  public String applicationId() {
    return "microbatch";
  }

  @Override
  protected void build(StreamsBuilder builder) {

    final String STORE = "state-store";
    final int BATCH_SIZE = 5;

    builder.<String, OSWindow>stream(Constants.WINDOWS, Consumed.as("windows-stream"))
            .process(new ProcessorSupplier<String, OSWindow, String, List<OSWindow>>() {
              public ContextualProcessor<String, OSWindow, String, List<OSWindow>> get() {
                return new ContextualProcessor<>() {
                  private int id;
                  public void process(Record<String, OSWindow> record) {
                    KeyValueStore<String, OSWindow> store = context().getStateStore(STORE);
                    if (store.approximateNumEntries() >= BATCH_SIZE) {
                      ArrayList<OSWindow> windows = new ArrayList<>((int) store.approximateNumEntries());
                      try (KeyValueIterator<String, OSWindow> iterator = store.all()) {
                        while (iterator.hasNext()) {
                          KeyValue<String, OSWindow> item = iterator.next();
                          windows.add(item.value);
                          store.delete(item.key);
                        }
                      }
                      windows.add(record.value()); //do not forget to add the event that triggered the batching!
                      id = 0;
                      context().forward(new Record<>(null, windows, System.currentTimeMillis()));
                    } else {
                      store.put(String.valueOf(id++), record.value());
                    }
                  }
                };
              }
              public Set<StoreBuilder<?>> stores() {
                StoreBuilder<KeyValueStore<String, OSWindow>> stateStore =
                        Stores.<String, OSWindow>keyValueStoreBuilder(Stores.inMemoryKeyValueStore(STORE), null, null)
                                .withLoggingDisabled();
                return Collections.singleton(stateStore);
              }
            }, Named.as("microbatch-processor"))
            .peek((k, v) -> log.error("key={}, value={}", k, v), Named.as("peek"))
            .to(OUTPUT_TOPIC, Produced.as("microbatch-sink"));

  }


  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }

}
