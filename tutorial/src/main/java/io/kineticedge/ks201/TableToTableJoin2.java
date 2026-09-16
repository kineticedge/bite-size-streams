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
import java.util.Map;

@SuppressWarnings("unused")
public class TableToTableJoin2 extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(TableToTableJoin2.class);

  private static final String OUTPUT_TOPIC = "tot-joined";

  @Override
  public String applicationId() {
    return "t-to-t-join-2";
  }

    @Override
    public Map<String, String> metadata() {
        return map(coreMetadata(),
                Map.entry("caching", isCachingDisabled() ? "disabled" : "enabled")        );
    }

    @Override
  protected void build(StreamsBuilder builder) {


      Materialized<String, OSWindowMap, KeyValueStore<Bytes, byte[]>> wMaterialized = Materialized.as("wstore");
      if (isCachingDisabled()) {
          wMaterialized.withCachingDisabled();
      }

      Materialized<String, OSProcess, KeyValueStore<Bytes, byte[]>> pMaterialized = Materialized.as("pstore");
      if (isCachingDisabled()) {
          pMaterialized.withCachingDisabled();
      }

      Materialized<String, String, KeyValueStore<Bytes, byte[]>> joinMaterialized =
              Materialized.as("jstore");
      joinMaterialized.withValueSerde(Serdes.String());

      if (isCachingDisabled()) {
          joinMaterialized.withCachingDisabled();
      }

      KTable<String, OSWindowMap> threads = builder.<String, OSWindow>stream(Constants.WINDOWS, Consumed.as("windows-source"))
            .selectKey((k, v) -> "" + v.processId(), Named.as("windows-select-key"))
            .groupByKey()
            .aggregate(OSWindowMap::new,
                    (k, v, a) -> {
                      a.addWindow(v);
                      return a;
                    },
                    Named.as("aggregate"),
                    wMaterialized
            );

    builder.<String, OSProcess>stream(Constants.PROCESSES, Consumed.as("processes-source"))
            .toTable(Named.as("processes-toTable"), pMaterialized)
            .join(
                    threads,
                    (p, w) -> "processName=" + p.name() + " | windowIds=" + w.windowIds(),
                    Named.as("__join__"),
                    joinMaterialized
            )
            .toStream(Named.as("toStream"))
            .to(OUTPUT_TOPIC, Produced.<String, String>as("output-to").withValueSerde(Serdes.String()));
  }


  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }
}
