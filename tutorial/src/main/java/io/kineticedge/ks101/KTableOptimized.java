package io.kineticedge.ks101;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.domain.OSProcess;
import io.kineticedge.kstutorial.domain.OSWindow;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
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
public class KTableOptimized extends BaseTopologyBuilder {

    private static final Logger log = LoggerFactory.getLogger(KTableOptimized.class);

    private static final String OUTPUT_TOPIC = "ktable-optimized-output";

    @Override
    public String applicationId() {
        return "ktable-opt";
    }

    @Override
    public Map<String, String> metadata() {
        return map(coreMetadata(),
                Map.entry("caching", isCachingDisabled() ? "disabled" : "enabled"),
                Map.entry("optimization", optimation())
        );
    }

    @Override
    protected void build(StreamsBuilder builder) {

        final Materialized<String, OSProcess, KeyValueStore<Bytes, byte[]>> materialized =
                Materialized.as("processes-store");

        if (isCachingDisabled()) {
            materialized.withCachingDisabled();
        }

        KTable<String, OSProcess> processesTable = builder
                .table(Constants.PROCESSES,
                        Consumed.as("processes-source"),
                        materialized
                );

        processesTable
                .toStream(Named.as("to-stream"))
                .to(OUTPUT_TOPIC, Produced.as("processes-to-output-sink"));

    }

    @Override
    public List<String> topics() {
        return List.of(OUTPUT_TOPIC);
    }
}
