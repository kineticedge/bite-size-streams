package io.kineticedge.ks301;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.common.streams.util.FixedKeyRecordFactory;
import io.kineticedge.kstutorial.domain.OSProcess;
import io.kineticedge.kstutorial.domain.OSProcessCount;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Repartitioned;
import org.apache.kafka.streams.kstream.SessionWindows;
import org.apache.kafka.streams.processor.api.ContextualFixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.state.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.kineticedge.kstutorial.common.streams.util.FixedKeyRecordFactory.create;

@SuppressWarnings("unused")
public class EmitOnWindowClose2 extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(EmitOnWindowClose2.class);

  private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
  };

  private static final String OUTPUT_TOPIC = "eowc-output";

  @Override
  public String applicationId() {
    return "eowc-2";
  }

  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }

  @Override
  protected void build(StreamsBuilder builder) {

    final Materialized<String, OSProcessCount, SessionStore<Bytes, byte[]>> store = Materialized.<String, OSProcessCount, SessionStore<Bytes, byte[]>>as("store");

    if (isCachingDisabled()) {
      store.withCachingDisabled();
    }

    final String repartitionedAs = "eowc";

    KStream<String, OSProcessCount> aggregate = builder.<String, OSProcess>stream(Constants.PROCESSES, Consumed.as("processes-source"))
            .peek(EmitOnWindowClose2::print, Named.as("peek"))
            .repartition(Repartitioned.as(repartitionedAs))
            //.filter((k, v) -> !k.startsWith("_")) <-- This does not work, advancing stream time is not enough
            .groupByKey(Grouped.as("group-by-key"))
            .windowedBy(SessionWindows.ofInactivityGapAndGrace(Duration.ofSeconds(5L), Duration.ofSeconds(1L)))
            .emitStrategy(emitStrategy())
            .aggregate(
                    () -> null,
                    (k, v, a) -> {
                      if (v == null) return null; // handle the special _# advancing messages.
                      else if (a == null) return OSProcessCount.create(v.processId());
                      else return a.increment();
                    },
                    (k, l, r) -> {
                      if (l == null) return r;
                      else if (r == null) return l;
                      else return l.increment(r);
                    },
                    Named.as("aggregate"),
                    store)
            .toStream(Named.as("to-stream"))
            .selectKey((k, v) -> k.key(), Named.as("select-key"))
            .processValues(() -> new ContextualFixedKeyProcessor<>() {
              public void init(final FixedKeyProcessorContext<String, OSProcessCount> context) {
                super.init(context);
                if (isFeatureEnabled()) {
                  context().schedule(Duration.ofSeconds(5L), punctuationType(), timestamp -> {
                    context().forward(create("_" + context().taskId().partition(), null, now(), null), "advance");
                  });
                }
              }
              public void process(FixedKeyRecord<String, OSProcessCount> record) {
                this.context().forward(record, "main");
              }
            }, Named.as("___advancer___"));

    aggregate
            .filter((k, v) -> k.startsWith("_"), Named.as("advance"))
            .mapValues((k, v) -> (OSProcess) null, Named.as("cast"))
            .to(applicationId() + "-" + repartitionedAs + "-repartition",
                    Produced.<String, OSProcess>as("eowc-advance-sink")
                            .withStreamPartitioner((topic, key, value, numPartitions) -> Optional.of(Set.of(Integer.parseInt(key.substring(1))))));

    aggregate
            .filter((k, v) -> !k.startsWith("_"), Named.as("main"))
            .to(OUTPUT_TOPIC, Produced.as("eowc-sink"));

  }

  protected static long now() {
    return System.currentTimeMillis();
  }
}
