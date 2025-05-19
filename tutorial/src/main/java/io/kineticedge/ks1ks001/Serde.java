package io.kineticedge.ks1ks001;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.common.serde.JsonSerde;
import io.kineticedge.kstutorial.common.serde.XmlSerde;
import io.kineticedge.kstutorial.domain.OSProcess;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Repartitioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class Serde extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(Serde.class);

  private static final String OUTPUT_TOPIC = "serde-output";

  @Override
  public String applicationId() {
    return "serde";
  }


  ///
  /// filter the `processes` topic for processes of the name `java` and map the OSProcess POJO to a new Map of only
  /// a subset of the properties.
  ///
  @Override
  protected void build(StreamsBuilder builder) {
    builder.<String, OSProcess>stream(Constants.PROCESSES)
            // SOLUTION
            .filter((k, v) -> v.name().equals("java"))
            .repartition(Repartitioned.<String, OSProcess>as("foo").withValueSerde(new XmlSerde<>()))
            // SOLUTION
            .peek((k, v) -> log.info("peek: key={}, value={}", k, v))
            .to(OUTPUT_TOPIC, Produced.<String, OSProcess>as("output").withValueSerde(new JsonSerde<>()));
  }

}
