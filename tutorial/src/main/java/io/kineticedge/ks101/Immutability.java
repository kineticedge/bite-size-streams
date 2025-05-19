package io.kineticedge.ks101;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.domain.OSProcess;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class Immutability extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(Immutability.class);

  private static final String OUTPUT_TOPIC_A = "split-A";
  private static final String OUTPUT_TOPIC_B = "split-B";

  @Override
  public String applicationId() {
    return "immutability";
  }

  @Override
  protected void build(StreamsBuilder builder) {
    var top = builder.<String, OSProcess>stream(Constants.PROCESSES, Consumed.as("processes-source"))
            .mapValues((k, v) -> {
              Map<String, Object> map = new HashMap<>();
              map.put("processId", v.processId());
              map.put("parentProcessId", v.parentProcessId());
              map.put("name", v.name());
              return map;
            }, Named.as("mapValues"));

    top
            .mapValues((k, v) -> {
              v.put("SIDE", "A");
              v.put("SIDE_A", true);
              return v;
            }, Named.as("mapValues-a")).to(OUTPUT_TOPIC_A, Produced.as("output-sink-A"));

    top
            .mapValues((k, v) -> {
              v.put("SIDE", "B");
              v.put("SIDE_B", true);
              return v;
            }, Named.as("mapValues-b")).to(OUTPUT_TOPIC_B, Produced.as("output-sink-B"));
  }

  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC_A, OUTPUT_TOPIC_B);
  }
}
