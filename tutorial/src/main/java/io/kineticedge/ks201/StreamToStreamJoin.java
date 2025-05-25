package io.kineticedge.ks201;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.domain.OSProcess;
import io.kineticedge.kstutorial.domain.OSWindow;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.StreamJoined;
import org.apache.kafka.streams.processor.api.ContextualFixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

@SuppressWarnings("unused")
public class StreamToStreamJoin extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(StreamToStreamJoin.class);

  private static final String OUTPUT_TOPIC = "sts-joined";

  @Override
  public String applicationId() {
    return "stream-to-stream-join";
  }


  @Override
  protected void build(StreamsBuilder builder) {

    KStream<String, OSProcess> processes = builder
            .<String, OSProcess>stream(Constants.PROCESSES, Consumed.as("processes-source"));

    KStream<String, OSWindow> windows = builder
            .<String, OSWindow>stream(Constants.WINDOWS, Consumed.as("windows-source"))
            .selectKey((k, v) -> "" + v.processId(), Named.as("windows-selectKey"));

    processes.join(
            windows,
            //(p, w) -> "processName=" + p.name() + " | windowId=" + w.windowId() + " | processIteration=" + p.iteration() + " | windowIteration=" + w.iteration(),
            (p, w) -> {

              String difference;
              if (p.iteration() > w.iteration()) {
                difference = "process +" + (p.iteration() - w.iteration()) + "(" + ((p.ts() - w.ts()) / 1000.0) + "s)";
              } else if (p.iteration() < w.iteration()) {
                difference = "window +" + (w.iteration() - p.iteration()) + "(" + ((w.ts() - p.ts()) / 1000.0) + "s)";
              } else {
                difference = "";
              }

              ObjectNode node = JsonNodeFactory.instance.objectNode();
              node.put("id", p.name() + "_" + p.id() + "_" + w.id());
              node.put("difference", difference);
              return node;
            },
            JoinWindows.ofTimeDifferenceAndGrace(
                    windowConfig().size().orElse(Duration.ofSeconds(5L)),
                    windowConfig().grace().orElse(Duration.ofSeconds(1L))
            ),
            StreamJoined.<String, OSProcess, OSWindow>as("join-stream")
    ).to(OUTPUT_TOPIC, Produced.<String, ObjectNode>as("output-sink"));
  }


  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }
}
