package io.kineticedge.ks201;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.domain.OSProcess;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;

@SuppressWarnings("unused")
public class KTableTTL extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(KTableTTL.class);

  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  private static final String OUTPUT_TOPIC = "ktable-ttl";

  private static final Duration PUNCTUATE_INTERVAL = Duration.ofSeconds(5L);
  private static final long MAX_PUNCTUATE_TIME = 500L;
  private static final long MAX_TIME_TO_LIVE = 10_000L;

  @Override
  public String applicationId() {
    return "table-ttl";
  }

  @Override
  protected void build(StreamsBuilder builder) {

    final String KTABLE_STORE = "process-table-statestore";

    final Materialized<String, OSProcess, KeyValueStore<Bytes, byte[]>> store = Materialized.as(KTABLE_STORE);

    if (isCachingDisabled()) {
      store.withCachingDisabled();
    }

    builder.<String, OSProcess>table(Constants.PROCESSES, Consumed.as("processes-source"), store)
            .toStream(Named.as("to-stream"))
            .peek(BaseTopologyBuilder::print, Named.as("peek"))
            .processValues(() -> new FixedKeyProcessor<String, OSProcess, OSProcess>() {
                  private String last = null;
                  private FixedKeyProcessorContext<String, OSProcess> context;
                  @Override
                  public void init(final FixedKeyProcessorContext<String, OSProcess> context) {
                    this.context = context;
                    this.context.schedule(PUNCTUATE_INTERVAL, punctuationType(), timestamp -> {
                      log.info("schedule: timestamp={}, streamTime={}", format(timestamp), format(this.context.currentStreamTimeMs()));
                      final KeyValueStore<String, ValueAndTimestamp<OSProcess>> store = this.context.getStateStore(KTABLE_STORE);
                      long started = System.currentTimeMillis();
                      store.flush();
                      try (KeyValueIterator<String, ValueAndTimestamp<OSProcess>> iterator = store.range(last, null)) {
                        while (iterator.hasNext()) {
                          final KeyValue<String, ValueAndTimestamp<OSProcess>> item = iterator.next();
                          log.info("key={}, timestamp={}", item.key, format(item.value.timestamp()));
                          if (this.context.currentStreamTimeMs() > item.value.timestamp() + MAX_TIME_TO_LIVE) {
                            log.info("removing key={}", item.key);
                            store.delete(item.key);
                          }
                          if (System.currentTimeMillis() > started + MAX_PUNCTUATE_TIME) {
                            last = item.key;
                            log.info("stopping schedule after {}ms at {}", System.currentTimeMillis() - started, last);
                            break;
                          }
                        }
                        if (!iterator.hasNext()) {
                          log.info("stopping schedule at end -- {} ms", System.currentTimeMillis() - started);
                          last = null;
                        }
                      }
                    });
                  }
                  @Override
                  public void process(FixedKeyRecord<String, OSProcess> record) {
                    if (record.value() != null) {
                      context.forward(record.withValue(record.value()));
                    }
                  }
            }, Named.as("process-values"), KTABLE_STORE)
            .to(OUTPUT_TOPIC, Produced.as("output-sink"));
  }


  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }

}
