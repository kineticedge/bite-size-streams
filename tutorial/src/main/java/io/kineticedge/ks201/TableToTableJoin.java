package io.kineticedge.ks201;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.domain.OSProcess;
import io.kineticedge.kstutorial.domain.OSWindow;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@SuppressWarnings("unused")
public class TableToTableJoin extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(TableToTableJoin.class);

  private static final String OUTPUT_TOPIC = "tot-joined";

  @Override
  public String applicationId() {
    return "table-to-table-join";
  }


  @Override
  protected void build(StreamsBuilder builder) {

    // "oops"
    KTable<String, OSWindow> windows = builder.<String, OSWindow>stream(Constants.WINDOWS, Consumed.as("windows-source"))
            .selectKey((k, v) -> "" + v.owningProcessId(), Named.as("threads-selectKey"))
            .toTable(Named.as("windows-toTable"), Materialized.as("windows-store"));

    builder.<String, OSProcess>stream(Constants.PROCESSES, Consumed.as("processes-source"))
            .toTable(Named.as("processes-toTable"), Materialized.as("processes-store"))
            .join(
                    windows,
                    (p, w) -> "processName=" + p.name() + " | windowId=" + w.windowId() + " | visible=" + w.visible(),
                    Named.as("__incorrect_join__"),
                    Materialized.<String, String, KeyValueStore<Bytes, byte[]>>as("join").withValueSerde(Serdes.String())
            )
            .toStream(Named.as("toStream"))
            .to(OUTPUT_TOPIC, Produced.<String, String>as("output-to").withValueSerde(Serdes.String()));
  }


  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }
}
