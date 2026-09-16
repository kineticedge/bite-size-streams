package io.kineticedge.ks101;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.common.streams.util.FixedKeyRecordFactory;
import io.kineticedge.kstutorial.domain.OSProcess;
import io.kineticedge.kstutorial.domain.OSWindow;
import io.kineticedge.kstutorial.domain.OSWindowAggregate;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.api.ContextualFixedKeyProcessor;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorSupplier;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class Tombstoning extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(Tombstoning.class);

  private static final String OUTPUT_TOPIC = "latest-window";

  @Override
  public String applicationId() {
    return "tombstoning";
  }

  @Override
  protected void build(StreamsBuilder builder) {

    final String STATE_STORE = "current-window";

    var materialized = Materialized.<String, OSWindow, KeyValueStore<Bytes, byte[]>>as(STATE_STORE);

    if (isCachingDisabled()) {
      materialized.withCachingDisabled();
    }

    builder.<String, OSWindow>stream(Constants.WINDOWS, Consumed.as("windows-source"))
            .groupByKey(Grouped.as("group-by-key"))
            .aggregate(
                    () -> null,
                    (key, window, aggregate) -> window,
                    Named.as("aggregate"),
                    materialized
            )
            .toStream(Named.as("to-stream"))
            .filter((k, v) -> v != null)
            .processValues(new FixedKeyProcessorSupplier<String, OSWindow, OSWindow>() {
              @Override
              public FixedKeyProcessor<String, OSWindow, OSWindow> get() {
                return new ContextualFixedKeyProcessor<>() {
                  @Override
                  public void init(FixedKeyProcessorContext<String, OSWindow> context) {
                    super.init(context);
                    context().schedule(Duration.ofSeconds(10L), punctuationType(), timestamp -> {
                      log.info("schedule: timestamp={}, streamTime={}", format(timestamp), format(context.currentStreamTimeMs()));
                      final KeyValueStore<String, OSWindow> store = context.getStateStore(STATE_STORE);
                      final Map<String, OSWindow> windows = new HashMap<>();
                      try(var iterator = store.all()) {
                        iterator.forEachRemaining(entry -> {
                          System.out.println(entry.key + " -> " + entry.value);
                          store.delete(entry.key);
                        });
                      }
                    });
                  }
                  @Override
                  public void process(FixedKeyRecord<String, OSWindow> rec) {
                    context().forward(rec);
                  }
                };
              }
            }, STATE_STORE)
            .peek((k, v) -> log.info("key={}, value={}", k, v), Named.as("peek-filtered"))
            .to(OUTPUT_TOPIC, Produced.as(OUTPUT_TOPIC + "-sink"));
  }

  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }
}
