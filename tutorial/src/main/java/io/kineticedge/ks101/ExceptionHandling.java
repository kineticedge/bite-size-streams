package io.kineticedge.ks101;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.domain.OSProcess;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ExceptionHandling extends BaseTopologyBuilder {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandling.class);

    private static final String OUTPUT_TOPIC = "exception-handling";

    @Override
    public String applicationId() {
        return "eh";
    }

    @Override
    public List<String> topics() {
        return List.of(OUTPUT_TOPIC);
    }

    @Override
    protected void build(StreamsBuilder builder) {
        builder
                .<String, OSProcess>stream(Constants.PROCESSES, Consumed.as("eh-source"))
                .mapValues((k, v) -> {

                    if (v.name().length() < 10) {
                        log.error("PRETEND ERROR");
                        throw new RuntimeException("Name too short");
                    }

                    return v.name() + "_" + v.upTime();
                })
                .to(OUTPUT_TOPIC, Produced.<String, String>as("eh-sink-to").withValueSerde(Serdes.String()));
    }

}
