package io.kineticedge.ks201;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.common.streams.util.DurationParser;
import io.kineticedge.kstutorial.domain.OSWindow;
import io.kineticedge.kstutorial.domain.OSWindowAggregate;
import io.kineticedge.kstutorial.domain.OSWindowPositions;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.SessionWindows;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.SessionStore;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class SessionWindow extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(SessionWindow.class);

  private static final String OUTPUT_TOPIC = "window-pos";

  @Override
  public String applicationId() {
    return "session-window";
  }    @Override
    public Map<String, String> metadata() {

        final Duration size = windowConfig().size().orElse(Duration.ofSeconds(10L));
        final Duration grace = windowConfig().grace().orElse(Duration.ofSeconds(1L));

        return map(coreMetadata(),
                Map.entry("caching", isCachingDisabled() ? "disabled" : "enabled"),
                Map.entry("window-size", DurationParser.toString(size)),
                Map.entry("window-grace", DurationParser.toString(grace))
        );
    }



    @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }


  @Override
  protected void build(StreamsBuilder builder) {

    var materialized = Materialized.<String, OSWindowPositions, SessionStore<Bytes, byte[]>>as("window-positions");

    if (isCachingDisabled()) {
      materialized.withCachingDisabled();
    }

    builder.<String, OSWindow>stream(Constants.WINDOWS, Consumed.as("windows-source"))
            .groupByKey(Grouped.as("group-by-key"))
            .windowedBy(SessionWindows.ofInactivityGapAndGrace(
                    windowConfig().advance().orElse(Duration.ofSeconds(10L)),
                    windowConfig().grace().orElse(Duration.ofSeconds(1L)))
            )
            .aggregate(
                    OSWindowPositions::create,
                    (key, window, aggregate) -> aggregate.next(window.pos()),
                    (key, left, right) -> left.merge(right),
                    Named.as("aggregate"),
                    materialized
            )
            .toStream(Named.as("to-stream"))
            .selectKey((k, v) -> k.key() + " [" + k.window().startTime() + "," + k.window().endTime() + ")", Named.as("select-key"))
            .to(OUTPUT_TOPIC, Produced.as(OUTPUT_TOPIC + "-sink"));
  }

}
