package io.kineticedge.ks301;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.common.streams.util.DurationParser;
import io.kineticedge.kstutorial.common.streams.util.FixedKeyRecordFactory;
import io.kineticedge.kstutorial.domain.OSProcess;
import io.kineticedge.kstutorial.domain.OSProcessCount;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.EmitStrategy;
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

@SuppressWarnings("unused")
public class EmitOnWindowClose extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(EmitOnWindowClose.class);

  private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
  };

  private static final String OUTPUT_TOPIC = "emit-on-window-close-output";

  private Duration size = Duration.ofSeconds(5L);
  private Duration grace = Duration.ofSeconds(1L);

  @Override
  public Map<String, String> metadata() {
    return map(coreMetadata(),
            Map.entry("caching", isCachingDisabled() ? "disabled" : "enabled"),
            Map.entry("emit", emitStrategyType().name()),
            Map.entry("window-size", DurationParser.toString(size)),
            Map.entry("window-grace", DurationParser.toString(grace))
    );
  }


  @Override
  public String applicationId() {
    return "eowc";
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

    builder.<String, OSProcess>stream(Constants.PROCESSES, Consumed.as("processes-source"))
            .peek(EmitOnWindowClose::print, Named.as("peek"))
            .groupByKey(Grouped.as("group-by-key"))
            .windowedBy(SessionWindows.ofInactivityGapAndGrace(size, grace))
            .emitStrategy(emitStrategy())
            .aggregate(
                    () -> null,
                    (k, v, a) -> {
                      if (v == null) return null;
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
            .selectKey((k, v) -> k.key(), Named.as("selectKey"))
            .to(OUTPUT_TOPIC, Produced.as("eowc-sink"));

  }

}
