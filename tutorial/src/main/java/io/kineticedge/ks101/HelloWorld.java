package io.kineticedge.ks101;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;

import java.util.List;

public class HelloWorld extends BaseTopologyBuilder {

    private static final String OUTPUT_TOPIC = "hello-world";

    @Override
    public String applicationId() {
        return "hw";
    }

    @Override
    public List<String> topics() {
        return List.of(OUTPUT_TOPIC);
    }

    @Override
    protected void build(StreamsBuilder builder) {
        builder
                .stream(Constants.PROCESSES, Consumed.as("hw-source"))
                .to(OUTPUT_TOPIC, Produced.as("hw-sink"));
    }

}
