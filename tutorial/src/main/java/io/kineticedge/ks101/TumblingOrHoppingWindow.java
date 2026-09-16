package io.kineticedge.ks101;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.common.streams.util.DurationParser;
import io.kineticedge.kstutorial.domain.OSWindow;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class TumblingOrHoppingWindow extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(TumblingOrHoppingWindow.class);

  private static final String OUTPUT_TOPIC = "tumbling-or-hopping-output";

  @Override
  public String applicationId() {
    return "t-or-h-win";
  }

    @Override
    public Map<String, String> metadata() {

        final Duration size = windowConfig().size().orElse(Duration.ofSeconds(30L));
        final Duration grace = windowConfig().grace().orElse(Duration.ofSeconds(10L));
        final Duration advance = windowConfig().advance().orElse(size);
        final Duration retention = windowConfig().retention().orElse(Duration.ofMinutes(2L));

        return map(coreMetadata(),
                Map.entry("caching", isCachingDisabled() ? "disabled" : "enabled"),
//                Map.entry("feature", isFeatureDisabled() ? "disabled" : "enabled"),
                Map.entry("window-size", DurationParser.toString(size)),
                Map.entry("window-grace", DurationParser.toString(grace)),
                Map.entry("advance", DurationParser.toString(advance)),
                Map.entry("retention", DurationParser.toString(retention))
        );
    }


    @Override
  protected void build(StreamsBuilder builder) {

    final Duration size = windowConfig().size().orElse(Duration.ofSeconds(30L));
    final Duration grace = windowConfig().grace().orElse(Duration.ofSeconds(10L));
    final Duration advance = windowConfig().advance().orElse(size);
    final Duration retention = windowConfig().retention().orElse(Duration.ofMinutes(2L));

    var materialized = Materialized.<String, Long, WindowStore<Bytes, byte[]>>as("count-store")
            .withRetention(retention)
            .withValueSerde(Serdes.Long());

    if (isCachingDisabled()) {
      materialized.withCachingDisabled();
    }

    builder
            .<String, OSWindow>stream(Constants.WINDOWS, Consumed.as("windows-source"))
            .peek(TumblingOrHoppingWindow::print, Named.as("peek"))
            .groupBy((k, v) -> "" + v.processId(), Grouped.as("group-by-owning-process-id"))
            .windowedBy(TimeWindows
                    .ofSizeAndGrace(size, grace)
                    .advanceBy(advance)
            ).count(
                    Named.as("count"),
                    materialized
            ).toStream(Named.as("to-stream"))
            .selectKey((k, v) -> format(k), Named.as("select-key"))
            .to(OUTPUT_TOPIC,
                    Produced
                            .<String, Long>as("output-sink")
                            .withValueSerde(Serdes.Long())
            );
  }

  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }
}
