package io.kineticedge.ks201;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.domain.OSProcess;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Repartitioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@SuppressWarnings("unused")
public class PoisonPill extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(PoisonPill.class);

  private static final String OUTPUT_TOPIC = "poison-pill-output";

  @Override
  public String applicationId() {
    return "poison-pill";
  }

  @Override
  protected void build(StreamsBuilder builder) {
    builder.<String, OSProcess>stream(Constants.PROCESSES, Consumed.as("processes-source"))
            .peek(PoisonPill::print, Named.as("peek"))
            .to(OUTPUT_TOPIC, Produced.as("output-sink"));
  }

  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }
}
