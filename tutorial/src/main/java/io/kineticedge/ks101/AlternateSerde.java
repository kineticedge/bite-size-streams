package io.kineticedge.ks101;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.common.serde.XmlSerde;
import io.kineticedge.kstutorial.domain.OSProcess;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@SuppressWarnings("unused")
public class AlternateSerde extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(AlternateSerde.class);

  private static final String OUTPUT_TOPIC = "alternate-serde-output";

  @Override
  public String applicationId() {
    return "alternate-serde";
  }


  /// filter the `processes` topic for processes of the name `java` and map the OSProcess POJO to a new Map of only
  /// a subset of the properties.
  @Override
  protected void build(StreamsBuilder builder) {

    Materialized<String, Long, KeyValueStore<Bytes, byte[]>> materialized = Materialized.as("count-store");

    if (isCachingDisabled()) {
      materialized.withCachingDisabled();
    }

    builder.<String, OSProcess>stream(Constants.PROCESSES, Consumed.as("processes-source"))
            .selectKey((k, v) -> "" + v.parentProcessId(), Named.as("select-key-parent-process-id"))
            .groupByKey(Grouped
                    .<String, OSProcess>valueSerde(new XmlSerde<>())
                    .withName("group-by-key")
            ).count(
                    Named.as("count"),
                    materialized
            ).toStream(Named.as("to-stream"))
            .to(OUTPUT_TOPIC, Produced
                    .<String, Long>as("output-sink")
                    .withValueSerde(Serdes.Long())
            );
  }

  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }
}
