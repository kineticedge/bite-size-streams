package io.kineticedge.ks301;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.domain.OSProcess;
import io.kineticedge.kstutorial.domain.OSWindow;
import io.kineticedge.kstutorial.domain.OSWindowList;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TableJoined;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;

@SuppressWarnings("unused")
public class ForeignKeyJoin extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(ForeignKeyJoin.class);

  private static final String OUTPUT_TOPIC = "foreign-key-output";

  @Override
  public String applicationId() {
    return "foreign-key-join";
  }


  /// filter the `processes` topic for processes of the name `java` and map the OSProcess POJO to a new Map of only
  /// a subset of the properties.
  @Override
  protected void build(StreamsBuilder builder) {

    KTable<String, OSProcess> processes = builder.<String, OSProcess>stream(Constants.PROCESSES, Consumed.as("processes-source"))
            .toTable(Named.as("processes-toTable"), Materialized.as("processes-store"));

    KTable<String, OSWindow> windows = builder.<String, OSWindow>stream(Constants.WINDOWS, Consumed.as("windows-source"))
            .toTable(Named.as("windows-toTable"), Materialized.as("windows-store"));

    windows.leftJoin(processes,
                    osWindow -> "" + osWindow.owningProcessId(),
                    (window, process) -> {
                      OSWindowList windowList = new OSWindowList();
                      windowList.setBatchId(process.parentProcessId() + " : " + process.name());
                      windowList.addWindow(window);
                      return windowList;
                    },
                    TableJoined.as("left-join")
            )
            .toStream(Named.as("to-stream"))
            .to(OUTPUT_TOPIC, Produced.as("foreign-key-output-sink"));
  }


  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }
}
