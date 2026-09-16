package io.kineticedge.ks1ks001;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.domain.OSWindow;
import io.kineticedge.kstutorial.domain.OSWindowList;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.List;

@SuppressWarnings("unused")
public class MicroBatchOLD extends BaseTopologyBuilder {

  private static final Logger log = LoggerFactory.getLogger(MicroBatchOLD.class);

  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  private static final String OUTPUT_TOPIC = "microbatch";

  @Override
  public String applicationId() {
    return "microbatch";
  }

//  @Override
//  protected void build(StreamsBuilder builder) {
//    // Create a serde for the list of OSWindow objects
//    Serde<List<OSWindow>> listSerde = new JsonSerde<>(OSWindow.class);
//
//    // Define your materialized store for the aggregation
//    var materialized = Materialized
//            .<String, List<OSWindow>, KeyValueStore<Bytes, byte[]>>as("batching-store")
//            .withKeySerde(Serdes.String())
//            .withValueSerde(listSerde);
//
//    // Build the stream processing topology
//    builder
//            .<String, OSWindow>stream(Constants.WINDOWS)
//            .peek((k, v) -> log.debug("Processing record: key={}, value={}", k, v))
//            // Group by key to prepare for aggregation
//            .groupByKey()
//            // Aggregate into lists
//            .aggregate(
//                    ArrayList::new,  // Initializer - create a new empty ArrayList
//                    (key, value, aggregate) -> {
//                      aggregate.add(value);
//                      log.debug("Added item to batch for key={}, current size={}", key, aggregate.size());
//                      return aggregate;
//                    },
//                    materialized
//            )
//            // Suppress output until we have 20 records or 30 seconds have passed
//            .suppress(
//                    Suppressed.untilTimeLimit(
//                            Duration.ofSeconds(30),  // Time limit before emitting regardless of count
//                            Suppressed.BufferConfig.unbounded()
//                                    .withMaxRecords(BATCH_SIZE)  // Release after 20 records
//                                    .emitEarlyWhenFull()  // Emit when we reach 20 even if time hasn't passed
//                    )
//            )
//            // Convert back to a stream
//            .toStream()
//            // Log the batches being emitted
//            .peek((k, batch) -> log.info(
//                    "Emitting batch: key={}, batch_size={}, items={}",
//                    k, batch.size(), batch
//            ))
//            // Send to output topic
//            .to(OUTPUT_TOPIC);
//
//  }

  @Override
  protected void build(StreamsBuilder builder) {

    final String KTABLE_STORE = "process-table-statestore";

    //Serde<List<OSWindow>> listSerde = new Serdes.ListSerde<>(ArrayList.class, new JsonSerde<OSWindow>());

  //  Stores.inMemoryWindowStore(KTABLE_STORE, Duration.ofHours(1), Duration.ofMinutes(60), true);
//    Materialized<String, List<OSWindow>, KeyValueStore<Bytes, byte[]>> materialized =
//            Materialized.<String, List<OSWindow>, KeyValueStore<Bytes, byte[]>>as("process-table-statestore")
//                    .withValueSerde(listSerde);

    Materialized<String, OSWindowList, KeyValueStore<Bytes, byte[]>> materialized = Materialized.<String, OSWindowList, KeyValueStore<Bytes, byte[]>>as("batching-store")
            .withCachingDisabled();

    final int BATCH_SIZE = 3;

    builder.<String, OSWindow>stream(Constants.WINDOWS)
            .process(new ProcessorSupplier<String, OSWindow, String, OSWindow>() {
              // can not do this with a select key or anything to kick off a repartition...
              @Override
              public Processor<String, OSWindow, String, OSWindow> get() {
                return new Processor<>() {

                  private int count;
                  private int id;

                  private ProcessorContext<String, OSWindow> context;
                  @Override
                  public void init(ProcessorContext<String, OSWindow> context) {
                    this.context = context;
                  }
                  @Override
                  public void process(Record<String, OSWindow> record) {
                    count++;
                    if (count % BATCH_SIZE == 0) {
                      id++;
                    }
                    System.out.println(count + " : " + id);
                    this.context.forward(new Record<>("__" + id, record.value(), record.timestamp(), record.headers()));
                  }
                };
              }
            })
            .peek((k, v) -> log.info("BEFORE key={}, value={}", k, v))
            .groupByKey()
          //  .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(10)))
            .aggregate(
                    OSWindowList::new,
                    (k, v, a) -> {
                      a.setBatchId(k);
                      a.addWindow(v);
                      return a;
                    },
                    materialized
            )
            .filter((k, v) -> v.getWindows().size() >= BATCH_SIZE)
//            .suppress(Suppressed.untilTimeLimit(Duration.ofSeconds(10),
//                    Suppressed.BufferConfig.unbounded().withMaxRecords(20).emitEarlyWhenFull()
//              )
//            )
            .toStream()
            .peek((k, v) -> log.error("key={}, value={}", k, v))
            .to(OUTPUT_TOPIC);

//            .groupByKey()
//            .aggregate(
//              ArrayList::new,
//                    )
////            // Use groupByKey which doesn't trigger repartitioning
//            // (as opposed to groupBy which does repartition)
//            .groupByKey()
//            // Apply tumbling windows - sized to act as batches
//            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(60)))
//            // Aggregate messages into lists
//            .aggregate(
//                    ArrayList::new,
//                    (key, value, aggregate) -> {
//                      aggregate.add(value);
//                      return aggregate;
//                    },
//                    Materialized.<String, List<Object>, WindowStore<Bytes, byte[]>>as(
//                                    Stores.inMemoryWindowStore("batching-store", Duration.ofHours(1), Duration.ofMinutes(60), false)
//                            )
//                            .withKeySerde(Serdes.String())
//                            .withValueSerde(listSerde)
//            )
//            // Apply suppression to control when batches are emitted
//            .suppress(Suppressed.untilWindowCloses(
//                    // Configure to emit after 20 records or window closes
//                    Suppressed.BufferConfig.unbounded().withMaxRecords(20).emitEarlyWhenFull()
//            ))
//            // Convert back to KStream and extract the batch
//            .toStream()
//            .map((windowedKey, value) -> KeyValue.pair(windowedKey.key(), value))
//            // Send to output topic
//            .to("output-topic", Produced.with(Serdes.String(), listSerde));

  }


  @Override
  public List<String> topics() {
    return List.of(OUTPUT_TOPIC);
  }

}
