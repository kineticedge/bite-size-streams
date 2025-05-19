package io.kineticedge.ks201;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.domain.OSProcess;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Repartitioned;
import org.apache.kafka.streams.processor.api.ContextualFixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;

@SuppressWarnings("unused")
public class SlowPunctuate extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(SlowPunctuate.class);

  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  private static final String OUTPUT_TOPIC = "bad-punctuate-output";

  private static final Duration PUNCTUATE_INTERVAL = Duration.ofSeconds(5L);
  private static final long MAX_PUNCTUATE_TIME = 500L;
  private static final long MAX_TIME_TO_LIVE = 10_000L;

  @Override
  public String applicationId() {
    return "slow-punctuate";
  }

  @Override
  protected void build(StreamsBuilder builder) {

    final String KTABLE_STORE = "process-table-statestore";

    final Materialized<String, OSProcess, KeyValueStore<Bytes, byte[]>> store = Materialized.as(KTABLE_STORE);

    if (isCachingDisabled()) {
      store.withCachingDisabled();
    }

    builder.<String, OSProcess>stream(Constants.PROCESSES, Consumed.as("processes-source"))
            .peek(BaseTopologyBuilder::print, Named.as("peek"))
            .processValues(() -> new ContextualFixedKeyProcessor<String, OSProcess, OSProcess>() {
                  @Override
                  public void init(final FixedKeyProcessorContext<String, OSProcess> context) {
                    super.init(context);
                    this.context().schedule(PUNCTUATE_INTERVAL, punctuationType(), timestamp -> {
                      log.info("schedule: timestamp={}, streamTime={}", format(timestamp), format(this.context().currentStreamTimeMs()));
                      long start = System.currentTimeMillis();
                      if (this.context().taskId().partition() == 0) {
                        sleep(1000L);
                      }
                      log.info("schedule: done, took {}ms", System.currentTimeMillis() - start);
                    });
                  }
                  @Override
                  public void process(FixedKeyRecord<String, OSProcess> record) {
                      //sleep(random.nextLong(5L));
                      context().forward(record.withValue(record.value()));
                  }
            }, Named.as("process-values-with-punctuate"))
            .repartition(Repartitioned.as("repartitioned"))
            .peek(BaseTopologyBuilder::print, Named.as("peek-repartitioned"))
            .to(OUTPUT_TOPIC, Produced.as("output-sink"));
  }


  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }

}
