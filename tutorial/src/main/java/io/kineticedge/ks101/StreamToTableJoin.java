package io.kineticedge.ks101;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.domain.OSProcess;
import io.kineticedge.kstutorial.domain.OSThread;
import io.kineticedge.kstutorial.domain.OSWindow;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.ValueJoiner;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Key;
import java.util.List;

@SuppressWarnings("unused")
public class StreamToTableJoin extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(StreamToTableJoin.class);

  private static final String OUTPUT_TOPIC = "stream-to-table-join-output";

  @Override
  public String applicationId() {
    return "stream-to-table-join";
  }


  @Override
  protected void build(StreamsBuilder builder) {

    final Materialized<String, OSProcess, KeyValueStore<Bytes, byte[]>> processStore = Materialized.as("processes-store");

    if (isCachingDisabled()) {
      processStore.withCachingDisabled();
    }

    KTable<String, OSProcess> processes = builder
            .<String, OSProcess>stream(
                    Constants.PROCESSES, Consumed.as("processes-source")
            ).toTable(
                    Named.as("processes-toTable"), processStore
            );

    builder.<String, OSWindow>stream(
                    Constants.WINDOWS,
                    Consumed.as("windows-source")
            ).selectKey((k, v) -> "" + v.processId(), Named.as("windows-selectKey"))
            .join(
                    processes,
                    (window, process) -> "pId=" + process.processId() + ", wI d=" + window.windowId() + " : " + rectangleToString(window.locAndSize()),
                    Joined.as("window-to-process-joiner")
            )
            .to(OUTPUT_TOPIC,
                    Produced.<String, String>as("output-sink")
                            .withValueSerde(Serdes.String())
            );
  }


  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }
}
