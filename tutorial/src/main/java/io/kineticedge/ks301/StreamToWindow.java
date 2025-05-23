package io.kineticedge.ks301;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.domain.OSProcess;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Repartitioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@SuppressWarnings("unused")
public class StreamToWindow extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(StreamToWindow.class);

  private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
  };

  private static final String OUTPUT_TOPIC = "process-with-parent";

  @Override
  public String applicationId() {
    return "resized-window";
  }

  @Override
  protected void build(StreamsBuilder builder) {

    final var stream = builder.<String, OSProcess>stream(Constants.PROCESSES);

    // TODO add in TTL for latest process, so any process that is > 10 minutes, is removed.

    final var latestProcess = stream
            .repartition(Repartitioned.as("foo"))
            .groupByKey(Grouped.as("groupByKey"))
            .reduce((v1, v2) -> v2, Named.as("reduce"), Materialized.as("reduce-store"));

    stream
            .peek((k, v) -> log.info("key={}, value={}", k, v), Named.as("peek"))
            .selectKey((k, v) -> Integer.toString(v.parentProcessId()), Named.as("selectKey"))
            .join(
                    latestProcess,
                    (left, right) -> Map.ofEntries(
                            Map.entry("child", left.name()),
                            Map.entry("parent", right.name())
                    )
            )
            .to(OUTPUT_TOPIC, Produced.as("output"));
  }

}
