package io.kineticedge.ks301;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.domain.OSWindow;
import io.kineticedge.kstutorial.domain.OSWindowMoves;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public class SlidingWindows extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(SlidingWindows.class);

  private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
  };

  private static final String OUTPUT_TOPIC = "sliding-windows-output";

  private static final Duration WINDOW_SIZE = Duration.ofSeconds(15L);

  @Override
  public String applicationId() {
    return "sliding-windows";
  }

  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }

  @Override
  protected void build(StreamsBuilder builder) {

    final Materialized<String, OSWindowMoves, WindowStore<Bytes, byte[]>> store = Materialized.<String, OSWindowMoves, WindowStore<Bytes, byte[]>>as("store");

    if (isCachingDisabled()) {
      store.withCachingDisabled();
    }

    builder.<String, OSWindow>stream(Constants.WINDOWS, Consumed.as("windows-source"))
            .peek((k, v) -> log.info("peek (before): key={}", k), Named.as("peek-incoming"))
            .groupByKey(Grouped.as("group-by-key"))
            .windowedBy(org.apache.kafka.streams.kstream.SlidingWindows.ofTimeDifferenceWithNoGrace(WINDOW_SIZE))
            .emitStrategy(emitStrategy())
            .aggregate(
                    OSWindowMoves::new,
                    (key, value, aggregate) -> {
                      aggregate.moved(value);
                      return aggregate;
                    },
                    Named.as("sliding-aggregate"),
                    store)
            .toStream(Named.as("to-stream"))
            .filter((k, v) -> v.getCount() > 0, Named.as("filter"))
            .peek((k, v) -> log.info("peek (after): key={}, value={}", k, v), Named.as("peek-after-filter"))
//            .selectKey((k, v) -> asString(k), Named.as("select-key"))
            .process(() -> new ContextualProcessor<Windowed<String>, OSWindowMoves, String, OSWindowMoves>() {
              public void process(Record<Windowed<String>, OSWindowMoves> record) {
                context().forward(new Record<>(asString(record, context().currentStreamTimeMs()), record.value(), record.timestamp(), record.headers()));
              }
            }, Named.as("select-key"))
            .to(OUTPUT_TOPIC, Produced.as("sliding-windows-sink"));

  }

  private static String asString(Windowed<String> k) {
    return k.key() + " [" + k.window().startTime() + "," + k.window().endTime() + ")";
  }

  protected static String asString(Record<Windowed<String>, OSWindowMoves> record, long currentStreamTimeMs) {
    final Windowed<String> k = record.key();
    double percent = (double) (record.timestamp() - k.window().start()) / (double) (k.window().end() - k.window().start()) * 100.0d;
    return String.format("%s [%s,%s){%d} %.2f", k.key(), k.window().startTime().toString(), k.window().endTime().toString(), ((k.window().end() - k.window().start()) / 1000), percent);
  }
}
