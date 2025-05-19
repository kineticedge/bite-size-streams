package io.kineticedge.ks101;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.domain.Id;
import io.kineticedge.kstutorial.domain.OSProcess;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Repartitioned;
import org.apache.kafka.streams.processor.api.ContextualFixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class TimestampPropagation extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(TimestampPropagation.class);

  private static final String OUTPUT_TOPIC = "timestamp-propagation-output";

  @Override
  public String applicationId() {
    return "timestamp-propagation";
  }

  @Override
  protected void build(StreamsBuilder builder) {
    builder.<String, OSProcess>stream(Constants.PROCESSES, Consumed.as("processes-source"))
            .selectKey((k, v) -> v.path(), Named.as("select-key-path"))
            .repartition()//Repartitioned.as("step-1"))
            .peek(TimestampPropagation::print, Named.as("peek-1"))
            .selectKey((k, v) -> Integer.toString(v.processId()), Named.as("select-key-process-id"))
            .repartition(Repartitioned.as("step-2"))
            .peek(TimestampPropagation::print, Named.as("peek-2"))
            .processValues(() -> new ContextualFixedKeyProcessor<String, OSProcess, OSProcess>() {
              public void process(FixedKeyRecord<String, OSProcess> record) {
                if (isFeatureDisabled()) {
                  context().forward(record);
                } else {
                  context().forward(record.withTimestamp(System.currentTimeMillis()));
                }
              }
            }, Named.as("set-ts-to-current-ts"))
            .to(OUTPUT_TOPIC, Produced.as("output-sink"));
  }

  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }

}
