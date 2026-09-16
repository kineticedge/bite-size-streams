package io.kineticedge.ks201;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.common.streams.util.DurationParser;
import io.kineticedge.kstutorial.domain.OSWindow;
import io.kineticedge.kstutorial.domain.OSWindowAggregate;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class EmitOnChange extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(EmitOnChange.class);

  private static final String OUTPUT_TOPIC = "resized-window";

  @Override
  public String applicationId() {
    return "resized-window";
  }

    @Override
    public Map<String, String> metadata() {
        return map(coreMetadata(),
                Map.entry("caching", isCachingDisabled() ? "disabled" : "enabled")
        );
    }
  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }
  @Override
  protected void build(StreamsBuilder builder) {

    var materialized = Materialized.<String, OSWindowAggregate, KeyValueStore<Bytes, byte[]>>as("current-window");

    if (isCachingDisabled()) {
      materialized.withCachingDisabled();
    }

    builder.<String, OSWindow>stream(Constants.WINDOWS, Consumed.as("windows-source"))
            .groupByKey(Grouped.as("group-by-key"))
            .aggregate(
                    () -> null,
                    (key, window, aggregate) -> {
                      if (aggregate == null) {
                        return new OSWindowAggregate(0 /*change_in_x*/, 0 /*change_in_y*/, 1 /*revision*/, window);
                      } else {
                        return new OSWindowAggregate(
                                window.x() - aggregate.window().x(),
                                window.y() - aggregate.window().y(),
                                aggregate.revision() + 1,
                                window);
                      }
                    },
                    Named.as("aggregate"),
                    materialized
            )
            .toStream(Named.as("to-stream"))
            .filter((k, v) -> v.xDelta() != 0 || v.yDelta() != 0, Named.as("filter"))
            .peek((k, v) -> log.info("filtered - key={}, value={}", k, v), Named.as("peek-filtered"))
            .to(OUTPUT_TOPIC, Produced.as(OUTPUT_TOPIC + "-sink"));
  }

}
