package io.kineticedge.ks101;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.common.serde.JsonSerde;
import io.kineticedge.kstutorial.common.serde.XmlSerde;
import io.kineticedge.kstutorial.domain.OSProcess;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Repartitioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@SuppressWarnings("unused")
public class FlatMapAndBranch extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(FlatMapAndBranch.class);

  private static final String OUTPUT_TOPIC_SWITCHES = "FLATMAP-AND-BRANCH-ARGS-SWITCHES";
  private static final String OUTPUT_TOPIC_OTHER = "FLATMAP-AND-BRANCH-ARGS-OTHER";

  @Override
  public String applicationId() {
    return "flatmap-and-branch";
  }

  @Override
  protected void build(StreamsBuilder builder) {
    builder.<String, OSProcess>stream(Constants.PROCESSES, Consumed.as("processes-source"))
            .flatMapValues((k, v) -> List.of(v.name().split(" ")), Named.as("flatMapValues"))
            .peek(FlatMapAndBranch::print, Named.as("peek"))
            .split(Named.as("split"))
            .branch((k, v) -> v.contains("."), Branched.withConsumer(c -> {
              c.to(OUTPUT_TOPIC_SWITCHES,
                      Produced.<String, String>as(OUTPUT_TOPIC_SWITCHES + "-sink")
                              .withValueSerde(Serdes.String()));
            }))
            .branch((k, v) -> true, Branched.withConsumer(c -> {
              c.to(OUTPUT_TOPIC_OTHER,
                      Produced.<String, String>as(OUTPUT_TOPIC_OTHER + "-sink")
                              .withValueSerde(Serdes.String()));
            }));
  }


  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC_SWITCHES, OUTPUT_TOPIC_OTHER);
  }
}
