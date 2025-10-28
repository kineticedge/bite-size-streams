package io.kineticedge.ks301;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.common.streams.util.DurationParser;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.TimestampedKeyValueStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class KTableTTL2 extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(KTableTTL2.class);

  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  private static final String OUTPUT_TOPIC = "ktable-ttl-2";

  private static final Duration PUNCTUATE_INTERVAL = Duration.ofSeconds(5L);


  // if task is in its own thread, make larger...
  private static final long MAX_PUNCTUATE_TIME = 10_000L;

  private static final long TTL = 10_000L;


  @Override
  public String applicationId() {
    return "table-ttl-2";
  }

    @Override
    public Map<String, String> metadata() {

        String ttlBasedOn = isFeatureEnabled() ? "stream-time" : "system-time";

        return map(coreMetadata(),
                Map.entry("caching", isCachingDisabled() ? "disabled" : "enabled"),
                Map.entry("punctuation", punctuationType().name().toLowerCase()),
                Map.entry("TTL age on", ttlBasedOn),
                Map.entry("TTL", DurationParser.toString(Duration.ofMillis(TTL)))
        );
    }

  @Override
  protected void build(StreamsBuilder builder) {

    final String KTABLE_STORE = "process-table-statestore";

    final String storeName = "processes-store";

    builder
            .stream(Constants.PROCESSES, Consumed.<byte[], byte[]>with(Serdes.ByteArray(), Serdes.ByteArray()).withName(storeName + "-source"))
            .filter((k, v) -> k != null, Named.as("filter-null-keys"))
            .to(OUTPUT_TOPIC, Produced.<byte[], byte[]>with(Serdes.ByteArray(), Serdes.ByteArray()).withName(storeName + "-sink"));

    builder
            .<byte[], byte[]>table(
                    OUTPUT_TOPIC,
                    Consumed.<byte[], byte[]>with(Serdes.ByteArray(), Serdes.ByteArray()).withName(storeName + "-table"),
                    Materialized.as(storeName))
            .toStream(Named.as("to-stream"))
            .process(() -> new ContextualProcessor<byte[], byte[], byte[], byte[]>() {
                      byte[] lastProcessedKey = null;
                      public void init(ProcessorContext<byte[], byte[]> context) {
                        super.init(context);
                        this.context().schedule(Duration.ofSeconds(10), punctuationType(), timestamp -> {
                          final long startTime = System.currentTimeMillis();
                          try (KeyValueIterator<byte[], ValueAndTimestamp<byte[]>> iter = getStateStore().range(lastProcessedKey, null)) {
                            while (iter.hasNext()) {
                              KeyValue<byte[], ValueAndTimestamp<byte[]>> kv = iter.next();
                              if (timestamp - TTL > kv.value.timestamp()) {
                                log.info("tombstoning...");
                                context.forward(new org.apache.kafka.streams.processor.api.Record<>(kv.key, null, timestamp, null));
                              }
                              if (System.currentTimeMillis() - startTime > MAX_PUNCTUATE_TIME) {
                                log.info("stopping early - {} took {} ms", storeName, System.currentTimeMillis() - startTime);
                                lastProcessedKey = kv.key;
                                break;
                              }
                            }
                            if (!iter.hasNext()) {
                              lastProcessedKey = null;
                            }
                          }
                        });
                      }

                      private TimestampedKeyValueStore<byte[], byte[]> getStateStore() {
                        return context().getStateStore(storeName);
                      }

                      public void process(Record<byte[], byte[]> record) {
                      }
                    }
                    , Named.as("ttl-processor-" + OUTPUT_TOPIC), storeName)
            .to(OUTPUT_TOPIC, Produced.<byte[], byte[]>with(Serdes.ByteArray(), Serdes.ByteArray()).withName(storeName + "-ttl-sink"));

  }


  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }

}
