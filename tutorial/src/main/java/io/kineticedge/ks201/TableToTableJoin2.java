package io.kineticedge.ks201;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.domain.OSProcess;
import io.kineticedge.kstutorial.domain.OSWindow;
import io.kineticedge.kstutorial.domain.OSWindowList;
import io.kineticedge.kstutorial.domain.OSWindowMap;
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
public class TableToTableJoin2 extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(TableToTableJoin2.class);

  private static final String OUTPUT_TOPIC = "tot-joined";

  @Override
  public String applicationId() {
    return "table-to-table-join-2";
  }

  @Override
  protected void build(StreamsBuilder builder) {


    KTable<String, OSWindowMap> threads = builder.<String, OSWindow>stream(Constants.WINDOWS, Consumed.as("windows-source"))
            .selectKey((k, v) -> "" + v.owningProcessId(), Named.as("windows-select-key"))
            .groupByKey()
            .aggregate(OSWindowMap::new,
                    (k, v, a) -> {
                      a.addWindow(v);
                      return a;
                    },
                    Materialized.as("wstore")
            );

    builder.<String, OSProcess>stream(Constants.PROCESSES, Consumed.as("processes-source"))
            .toTable(Named.as("processes-toTable"), Materialized.as("pstore"))
            .join(
                    threads,
                    (p, w) -> "processName=" + p.name() + " | windowIds=" + w.windowIds(),
                    Named.as("__join__"),
                    Materialized.<String, String, KeyValueStore<Bytes, byte[]>>as("jstore").withValueSerde(Serdes.String())
            )
            .toStream(Named.as("toStream"))
            .to(OUTPUT_TOPIC, Produced.<String, String>as("output-to").withValueSerde(Serdes.String()));
  }


  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }
}
