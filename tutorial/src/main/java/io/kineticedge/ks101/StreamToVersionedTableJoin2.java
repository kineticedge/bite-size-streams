package io.kineticedge.ks101;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.domain.OSProcess;
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
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

@SuppressWarnings("unused")
public class StreamToVersionedTableJoin2 extends BaseTopologyBuilder {

    private static final Logger log = LoggerFactory.getLogger(StreamToVersionedTableJoin2.class);

    private static final String OUTPUT_TOPIC = "stream-to-table-join-output";

    @Override
    public String applicationId() {
        return "s-to-vt-join";
    }


    @Override
    protected void build(StreamsBuilder builder) {

        Materialized<String, OSProcess, KeyValueStore<Bytes, byte[]>> processStore = Materialized.as(
                Stores.persistentVersionedKeyValueStore(
                        "processes-store",
                        Duration.ofMinutes(30)
                )
        );

        if (isCachingDisabled()) {
            processStore.withCachingDisabled();
        }

        KTable<String, OSProcess> processes = builder
                .<String, OSProcess>stream(Constants.PROCESSES, Consumed.as("processes-source"))
                .toTable(Named.as("processes-toTable"), processStore);

        builder.<String, OSWindow>stream(Constants.WINDOWS, Consumed.as("windows-source"))
                .selectKey((k, v) -> "" + v.processId(), Named.as("windows-selectKey"))
                .join(processes, StreamToVersionedTableJoin2::asString, Joined.<String, OSWindow, OSProcess>as("window-to-process-joiner").withGracePeriod(Duration.ofSeconds(10)))
                .to(OUTPUT_TOPIC, Produced.<String, String>as("output-sink").withValueSerde(Serdes.String()));
    }

    private static String asString(OSWindow w, OSProcess p) {
        return String.format("pId=%d(%d), wId=%d(%d) %s",
                p.processId(), p.iteration(),
                w.windowId(), w.iteration(),
                rectangleToString(w)
        );
    }

    @Override
    public List<String> topics() {
        return List.of(OUTPUT_TOPIC);
    }
}
