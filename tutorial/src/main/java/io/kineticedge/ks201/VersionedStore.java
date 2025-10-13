package io.kineticedge.ks201;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.common.streams.metadata.RangeQueryableVersionedStoreBuilder;
import io.kineticedge.kstutorial.domain.OSProcess;
import io.kineticedge.kstutorial.domain.OSWindow;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Repartitioned;
import org.apache.kafka.streams.processor.api.ContextualFixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorSupplier;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.VersionedKeyValueStore;
import org.apache.kafka.streams.state.VersionedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class VersionedStore extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(VersionedStore.class);

  private static final String STORE_NAME = "versioned-store";

  private static final String OUTPUT_TOPIC = "versioned-store-output";

  @Override
  public String applicationId() {
    return "versioned-store";
  }

  @Override
  protected void build(StreamsBuilder builder) {

    KStream<String, OSProcess> processes = builder
            .<String, OSProcess>stream(Constants.PROCESSES, Consumed.as("processes-source"))
            .processValues(new FixedKeyProcessorSupplier<>() {
                             public ContextualFixedKeyProcessor<String, OSProcess, OSProcess> get() {
                               return new ContextualFixedKeyProcessor<>() {
                                 public void process(FixedKeyRecord<String, OSProcess> record) {
                                   VersionedKeyValueStore<String, OSProcess> store = context().getStateStore(STORE_NAME);
                                   store.put(record.key(), record.value(), record.timestamp());
                                   context().forward(record);
                                 }
                               };
                             }

                             public Set<StoreBuilder<?>> stores() {
                               return Set.of(

                                       new RangeQueryableVersionedStoreBuilder<>(
                                               Stores.versionedKeyValueStoreBuilder(
                                                       Stores.persistentVersionedKeyValueStore(STORE_NAME, windowConfig().retention().orElse(Duration.ofMinutes(5L))),
                                                       null, null)
                                       )
                               );
                             }
                           }
                    , Named.as("processValues-put")
            );

    builder.<String, OSWindow>stream(Constants.WINDOWS, Consumed.as("windows-source"))
            .selectKey((k, v) -> "" + v.processId(), Named.as("select-key-owning-process-id"))
            .repartition(Repartitioned.as("windows-repartition"))
            .processValues((FixedKeyProcessorSupplier<String, OSWindow, String>) () -> new ContextualFixedKeyProcessor<>() {
              public void process(FixedKeyRecord<String, OSWindow> r) {
                VersionedKeyValueStore<String, OSProcess> store = context().getStateStore(STORE_NAME);
                VersionedRecord<OSProcess> vr = store.get(r.key(), r.timestamp());
                context().forward(r.withValue(asString(r, vr)));
              }
            }, Named.as("processValues-get"), STORE_NAME)
            .to(OUTPUT_TOPIC, Produced.<String, String>as("output-sink").withValueSerde(Serdes.String()));
  }

  private static String asString(OSWindow window, OSProcess process) {
    return "pId=" + process.processId() + "(" + process.iteration() + "), wId=" + window.windowId() + "(" + window.iteration() + ") " + rectangleToString(window);
  }

  private static String asString(FixedKeyRecord<String, OSWindow> r, VersionedRecord<OSProcess> vr) {

    //r.timestamp();
    //vr.timestamp();

    return "pId=" + vr.value().processId() + "(" + vr.value().iteration() + "), wId=" + r.value().windowId() + "(" + r.value().iteration() + ") " + rectangleToString(r.value());

//    return String.format("windowId=%s, processName=%s, uptime=%s",
//            r.value().windowId(), vr.value().name(), vr.value().upTime());
  }

  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }

  private static String formatUptime(long uptimeMs) {
    Duration duration = Duration.ofMillis(uptimeMs);
    long days = duration.toDays();
    long hours = duration.toHoursPart();
    long minutes = duration.toMinutesPart();
    long seconds = duration.toSecondsPart();
    long millis = duration.toMillisPart();
    return String.format("%d_%02d:%02d:%02d.%03d", days, hours, minutes, seconds, millis);
  }

}
