package io.kineticedge.ks1ks001;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.domain.OSProcess;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@SuppressWarnings("unused")
public class HelloWorld extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(HelloWorld.class);

  private static final String OUTPUT_TOPIC = "filtered-processes";

  @Override
  public String applicationId() {
    return "hello-world";
  }


  /// filter the `processes` topic for processes of the name `java` and map the OSProcess POJO to a new Map of only
  /// a subset of the properties.
  @Override
  protected void build(StreamsBuilder builder) {

    builder.<String, OSProcess>stream(Constants.PROCESSES)
            // SOLUTION
            .filter((k, v) -> v.name().equals("java"))
            .processValues(() -> new FixedKeyProcessor<String, OSProcess, OSProcess>() {
              private FixedKeyProcessorContext<String, OSProcess> context;

              @Override
              public void init(FixedKeyProcessorContext<String, OSProcess> context) {
                this.context = context;
              }

              @Override
              public void process(FixedKeyRecord<String, OSProcess> fixedKeyRecord) {
                context.forward(fixedKeyRecord.withValue(fixedKeyRecord.value()));
              }
            })
            .mapValues((k, v) -> {
              return Map.ofEntries(
                      Map.entry("processId", v.processId()),
//                      Map.entry("path", v.path()),
                      Map.entry("arguments", v.arguments()),
                      Map.entry("threadCount", v.threadCount())
              );
            })
            //.peek((k, v) -> log.info("filtered: key={}, value={}", k, v))
            // SOLUTION
            .to(OUTPUT_TOPIC);
  }

}
