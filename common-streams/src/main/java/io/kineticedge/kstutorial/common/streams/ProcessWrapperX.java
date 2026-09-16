package io.kineticedge.kstutorial.common.streams;

import io.kineticedge.kstutorial.common.streams.wrapper.ProcessorRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsMetrics;
import org.apache.kafka.streams.processor.Cancellable;
import org.apache.kafka.streams.processor.CommitCallback;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.Punctuator;
import org.apache.kafka.streams.processor.StateRestoreCallback;
import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.processor.TaskId;
import org.apache.kafka.streams.processor.To;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorSupplier;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.processor.api.ProcessorWrapper;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.processor.api.RecordMetadata;
import org.apache.kafka.streams.processor.api.WrappedFixedKeyProcessorSupplier;
import org.apache.kafka.streams.processor.api.WrappedProcessorSupplier;
import org.apache.kafka.streams.processor.internals.InternalProcessorContext;
import org.apache.kafka.streams.processor.internals.ProcessorMetadata;
import org.apache.kafka.streams.processor.internals.ProcessorNode;
import org.apache.kafka.streams.processor.internals.ProcessorRecordContext;
import org.apache.kafka.streams.processor.internals.RecordCollector;
import org.apache.kafka.streams.processor.internals.StreamTask;
import org.apache.kafka.streams.processor.internals.Task;
import org.apache.kafka.streams.processor.internals.metrics.StreamsMetricsImpl;
import org.apache.kafka.streams.query.Position;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.internals.ThreadCache;

import java.io.File;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ProcessWrapperX implements ProcessorWrapper {

  private static AtomicLong counter = new AtomicLong();
  private static final byte[] id() {
    return ("_" + counter.incrementAndGet()).getBytes();
  }

  private static Map<String, List<ProcessorRecord>> records = new ConcurrentHashMap<>();

  public static Map<String, List<ProcessorRecord>> getRecords() {
      return records;
  }

  public static Map<String, List<ProcessorRecord>> getAl() {
    return records;
  }

  @Override
  public void configure(Map<String, ?> configs) {
    ProcessorWrapper.super.configure(configs);
  }

  @Override
  public <KIn, VIn, KOut, VOut> WrappedProcessorSupplier<KIn, VIn, KOut, VOut> wrapProcessorSupplier(String s, ProcessorSupplier<KIn, VIn, KOut, VOut> processorSupplier) {
    System.out.println("Process Supplier > " + s);
    return new Foo2<>(s, processorSupplier);
//    return ProcessorWrapper.asWrapped(processorSupplier);
  }

  @Override
  public <KIn, VIn, VOut> WrappedFixedKeyProcessorSupplier<KIn, VIn, VOut> wrapFixedKeyProcessorSupplier(String s, FixedKeyProcessorSupplier<KIn, VIn, VOut> fixedKeyProcessorSupplier) {
    System.out.println("Fixed Process Supplier > " + s);
    return new Foo<>(s, fixedKeyProcessorSupplier);
  }

  public  class ProcessorContextWrapper<KForward, VForward> implements ProcessorContext<KForward, VForward> {

    private final String name;
    private final ProcessorContext<KForward, VForward> context;

    public ProcessorContextWrapper(String name, ProcessorContext<KForward, VForward> context) {
      this.name = name;
      this.context = context;

    }


    @Override
    public <K extends KForward, V extends VForward> void forward(Record<K, V> fixedKeyRecord) {


      Header lineage = fixedKeyRecord.headers().lastHeader("lineage");
      if (lineage == null) {
        lineage = new RecordHeader("lineage", id());
        fixedKeyRecord.headers().add(lineage);
      }

      List<ProcessorRecord> list = records.computeIfAbsent(new String(lineage.value()), key -> new LinkedList<>());
      list.add(new ProcessorRecord(
              Thread.currentThread().getName(),
              context.recordMetadata().orElse(EMPTY).topic(),
              context.recordMetadata().orElse(EMPTY).partition(),
              name,
              "out",
              fixedKeyRecord.key() != null ? fixedKeyRecord.key().toString() : null,
              null, //fixedKeyRecord.value() != null ? fixedKeyRecord.value().toString() : null,
              "*",
              System.nanoTime()));

      //fixedKeyRecord.headers()
      System.out.println("***************************************** Fixed Process Record > " + fixedKeyRecord);
      System.out.println(list);

      this.context.forward(fixedKeyRecord);
    }

    @Override
    public <K extends KForward, V extends VForward> void forward(Record<K, V> fixedKeyRecord, String s) {

      Header lineage = fixedKeyRecord.headers().lastHeader("lineage");
      if (lineage == null) {
        lineage = new RecordHeader("lineage", id());
        fixedKeyRecord.headers().add(lineage);
      }

      List<ProcessorRecord> list = records.computeIfAbsent(new String(lineage.value()), key -> new LinkedList<>());
      list.add(new ProcessorRecord(
              Thread.currentThread().getName(),
              context.recordMetadata().orElse(EMPTY).topic(),
              context.recordMetadata().orElse(EMPTY).partition(),
              name,
              "out",
              fixedKeyRecord.key() != null ? fixedKeyRecord.key().toString() : null,
              null, //fixedKeyRecord.value() != null ? fixedKeyRecord.value().toString() : null,
              s,
              System.nanoTime()));


      //System.out.println("***************************************** Fixed Process Record > " + fixedKeyRecord + " with target topic " + s);
      this.context.forward(fixedKeyRecord, s);
    }


    public String applicationId() {
      return this.context.applicationId();
    }

    public TaskId taskId() {
      return this.context.taskId();
    }

    public Optional<RecordMetadata> recordMetadata() {
      return this.context.recordMetadata();
    }

    public Serde<?> keySerde() {
      return this.context.keySerde();
    }

    public Serde<?> valueSerde() {
      return this.context.valueSerde();
    }

    public File stateDir() {
      return this.context.stateDir();
    }

    public StreamsMetrics metrics() {
      return this.context.metrics();
    }

    public <S extends StateStore> S getStateStore(String var1) {
      return this.context.getStateStore(var1);
    }

    public Cancellable schedule(Duration var1, PunctuationType var2, Punctuator var3) {
      return this.context.schedule(var1, var2, var3);
    }

    public void commit() {
      this.context.commit();
    }

    public Map<String, Object> appConfigs() {
      return this.context.appConfigs();
    }

    public Map<String, Object> appConfigsWithPrefix(String var1) {
      return this.context.appConfigsWithPrefix(var1);
    }

    public long currentSystemTimeMs() {
      return this.context.currentSystemTimeMs();
    }

    public long currentStreamTimeMs() {
      return this.context.currentStreamTimeMs();
    }

  }

  public  class FixedKeyProcessorContextWrapper<KForward, VForward> implements FixedKeyProcessorContext<KForward, VForward> {

    private final String name;
    private final FixedKeyProcessorContext<KForward, VForward> context;

    public FixedKeyProcessorContextWrapper(String name, FixedKeyProcessorContext<KForward, VForward> context) {
      this.name = name;
      this.context = context;

    }


    @Override
    public <K extends KForward, V extends VForward> void forward(FixedKeyRecord<K, V> fixedKeyRecord) {


      Header lineage = fixedKeyRecord.headers().lastHeader("lineage");
      if (lineage == null) {
        lineage = new RecordHeader("lineage", id());
        fixedKeyRecord.headers().add(lineage);
      }

      List<ProcessorRecord> list = records.computeIfAbsent(new String(lineage.value()), key -> new LinkedList<>());
      list.add(new ProcessorRecord(
              Thread.currentThread().getName(),
              context.recordMetadata().orElse(EMPTY).topic(),
              context.recordMetadata().orElse(EMPTY).partition(),
              name,
              "out",
              fixedKeyRecord.key() != null ? fixedKeyRecord.key().toString() : null,
              null, //fixedKeyRecord.value() != null ? fixedKeyRecord.value().toString() : null,
              "*",
              System.nanoTime()));


      //fixedKeyRecord.headers()
      System.out.println("***************************************** Fixed Process Record > " + fixedKeyRecord);
      System.out.println(list);

      this.context.forward(fixedKeyRecord);
    }

    @Override
    public <K extends KForward, V extends VForward> void forward(FixedKeyRecord<K, V> fixedKeyRecord, String s) {

      Header lineage = fixedKeyRecord.headers().lastHeader("lineage");
      if (lineage == null) {
        lineage = new RecordHeader("lineage", id());
        fixedKeyRecord.headers().add(lineage);
      }

      List<ProcessorRecord> list = records.computeIfAbsent(new String(lineage.value()), key -> new LinkedList<>());
      list.add(new ProcessorRecord(
              Thread.currentThread().getName(),
              context.recordMetadata().orElse(EMPTY).topic(),
              context.recordMetadata().orElse(EMPTY).partition(),
              name,
              "out",
              fixedKeyRecord.key() != null ? fixedKeyRecord.key().toString() : null,
              null, //fixedKeyRecord.value() != null ? fixedKeyRecord.value().toString() : null,
              s,
              System.nanoTime()));


      //System.out.println("***************************************** Fixed Process Record > " + fixedKeyRecord + " with target topic " + s);
      this.context.forward(fixedKeyRecord, s);
    }


    public String applicationId() {
      return this.context.applicationId();
    }

    public TaskId taskId() {
      return this.context.taskId();
    }

    public Optional<RecordMetadata> recordMetadata() {
      return this.context.recordMetadata();
    }

    public Serde<?> keySerde() {
      return this.context.keySerde();
    }

    public Serde<?> valueSerde() {
      return this.context.valueSerde();
    }

    public File stateDir() {
      return this.context.stateDir();
    }

    public StreamsMetrics metrics() {
      return this.context.metrics();
    }

    public <S extends StateStore> S getStateStore(String var1) {
      return this.context.getStateStore(var1);
    }

    public Cancellable schedule(Duration var1, PunctuationType var2, Punctuator var3) {
      return this.context.schedule(var1, var2, var3);
    }

    public void commit() {
      this.context.commit();
    }

    public Map<String, Object> appConfigs() {
      return this.context.appConfigs();
    }

    public Map<String, Object> appConfigsWithPrefix(String var1) {
      return this.context.appConfigsWithPrefix(var1);
    }

    public long currentSystemTimeMs() {
      return this.context.currentSystemTimeMs();
    }

    public long currentStreamTimeMs() {
      return this.context.currentStreamTimeMs();
    }

  }


  public class Bar<KIn, VIn, VOut> implements FixedKeyProcessor<KIn, VIn, VOut> {

    private final String name;
    private final FixedKeyProcessor<KIn, VIn, VOut> delegate;
    private FixedKeyProcessorContext<KIn, VOut> context;

    public Bar(final String name, FixedKeyProcessor<KIn, VIn, VOut> delegate) {
      this.name = name;
      this.delegate = delegate;
    }

    @Override
    public void process(FixedKeyRecord<KIn, VIn> fixedKeyRecord) {

      Header lineage = fixedKeyRecord.headers().lastHeader("lineage");
      if (lineage == null) {
        lineage = new RecordHeader("lineage", id());
        fixedKeyRecord.headers().add(lineage);
      }
      List<ProcessorRecord> list = records.computeIfAbsent(new String(lineage.value()), key -> new LinkedList<>());
      list.add(new ProcessorRecord(
              Thread.currentThread().getName(),
              context.recordMetadata().orElse(EMPTY).topic(),
              context.recordMetadata().orElse(EMPTY).partition(),
              name,
              "in",
              fixedKeyRecord.key() != null ? fixedKeyRecord.key().toString() : null,
              null, //fixedKeyRecord.value() != null ? fixedKeyRecord.value().toString() : null,
              null,
              System.nanoTime()));


      //System.out.println("PROCESS : " + this.name);
      //System.out.println("!!!!!Fixed Process Record > " + fixedKeyRecord);
      this.delegate.process(fixedKeyRecord);

    }

    @Override
    public void init(FixedKeyProcessorContext<KIn, VOut> context) {
      System.out.println("INIT : " + this.name);
      this.delegate.init(new FixedKeyProcessorContextWrapper<>(name, context));
      this.context = context;
    }

    @Override
    public void close() {
      System.out.println("CLOSE : " + this.name);
      this.delegate.close();
    }
  }

  public class Foo<KIn, VIn, VOut> implements WrappedFixedKeyProcessorSupplier<KIn, VIn, VOut> {

    private final String name;

    private final FixedKeyProcessorSupplier<KIn, VIn, VOut> delegate;

    public Foo(final String name, FixedKeyProcessorSupplier<KIn, VIn, VOut> delegate) {
      this.name = name;
      this.delegate = delegate;
    }

    public Set<StoreBuilder<?>> stores() {
      return this.delegate.stores();
    }

    public FixedKeyProcessor<KIn, VIn, VOut> get() {
      return new Bar<>(this.name, this.delegate.get());
    }
  }



  //


  public class Bar2<KIn, VIn, KOut, VOut> implements Processor<KIn, VIn, KOut, VOut> {

    private final String name;
    private final Processor<KIn, VIn, KOut, VOut> delegate;
    private ProcessorContext<KOut, VOut> context;

    public Bar2(final String name, Processor<KIn, VIn, KOut, VOut> delegate) {
      this.name = name;
      this.delegate = delegate;
    }

    @Override
    public void process(Record<KIn, VIn> fixedKeyRecord) {

      Header lineage = fixedKeyRecord.headers().lastHeader("lineage");
      if (lineage == null) {
        lineage = new RecordHeader("lineage", id());
        fixedKeyRecord.headers().add(lineage);
      }
      List<ProcessorRecord> list = records.computeIfAbsent(new String(lineage.value()), key -> new LinkedList<>());
      list.add(new ProcessorRecord(
              Thread.currentThread().getName(),
              context.recordMetadata().orElse(EMPTY).topic(),
              context.recordMetadata().orElse(EMPTY).partition(),
              name,
              "in",
              fixedKeyRecord.key() != null ? fixedKeyRecord.key().toString() : null,
              null, //fixedKeyRecord.value() != null ? fixedKeyRecord.value().toString() : null,
              null,
              System.nanoTime()));


      //System.out.println("PROCESS : " + this.name);
      //System.out.println("!!!!!Fixed Process Record > " + fixedKeyRecord);
      this.delegate.process(fixedKeyRecord);

    }

    @Override
    public void init(ProcessorContext<KOut, VOut> context) {
      System.out.println("INIT : " + this.name);
      if (context instanceof InternalProcessorContext) {
        this.delegate.init(new ProcessorContextWrapper2<>(name, (InternalProcessorContext) context));
      } else {
        this.delegate.init(new ProcessorContextWrapper<>(name, context));
      }
      this.context = context;
    }

    @Override
    public void close() {
      System.out.println("CLOSE : " + this.name);
      this.delegate.close();
    }
  }

  public class Foo2<KIn, VIn, KOut, VOut> implements WrappedProcessorSupplier<KIn, VIn, KOut, VOut> {

    private final String name;

    private final ProcessorSupplier<KIn, VIn, KOut, VOut> delegate;

    public Foo2(final String name, ProcessorSupplier<KIn, VIn, KOut, VOut> delegate) {
      this.name = name;
      this.delegate = delegate;
    }

    public Set<StoreBuilder<?>> stores() {
      return this.delegate.stores();
    }

    public Processor<KIn, VIn, KOut, VOut> get() {
      return new Bar2<>(this.name, this.delegate.get());
    }
  }











  //

  private final static RecordMetadata EMPTY = new RecordMetadata() {
    @Override
    public String topic() {
      return "<na>";
    }

    @Override
    public int partition() {
      return -1;
    }

    @Override
    public long offset() {
      return -1L;
    }
  };


  //


  public  class ProcessorContextWrapper2<KForward, VForward> implements InternalProcessorContext<KForward, VForward> {

    private final String name;
    private final InternalProcessorContext<KForward, VForward> context;

    public ProcessorContextWrapper2(String name, InternalProcessorContext<KForward, VForward> context) {
      this.name = name;
      this.context = context;

    }


    @Override
    public <K extends KForward, V extends VForward> void forward(FixedKeyRecord<K, V> fixedKeyRecord) {

      Header lineage = fixedKeyRecord.headers().lastHeader("lineage");
      if (lineage == null) {
        lineage = new RecordHeader("lineage", id());
        fixedKeyRecord.headers().add(lineage);
      }

      List<ProcessorRecord> list = records.computeIfAbsent(new String(lineage.value()), key -> new LinkedList<>());
      list.add(new ProcessorRecord(
              Thread.currentThread().getName(),
              context.recordMetadata().orElse(EMPTY).topic(),
              context.recordMetadata().orElse(EMPTY).partition(),
              name,
              "out",
              fixedKeyRecord.key() != null ? fixedKeyRecord.key().toString() : null,
              null, //fixedKeyRecord.value() != null ? fixedKeyRecord.value().toString() : null,
              "*",
              System.nanoTime()));

      this.context.forward(fixedKeyRecord);
    }


    @Override
    public <K extends KForward, V extends VForward> void forward(FixedKeyRecord<K, V> fixedKeyRecord, String s) {

      Header lineage = fixedKeyRecord.headers().lastHeader("lineage");
      if (lineage == null) {
        lineage = new RecordHeader("lineage", id());
        fixedKeyRecord.headers().add(lineage);
      }

      List<ProcessorRecord> list = records.computeIfAbsent(new String(lineage.value()), key -> new LinkedList<>());
      list.add(new ProcessorRecord(
              Thread.currentThread().getName(),
              context.recordMetadata().orElse(EMPTY).topic(),
              context.recordMetadata().orElse(EMPTY).partition(),
              name,
              "out",
              fixedKeyRecord.key() != null ? fixedKeyRecord.key().toString() : null,
              null, //fixedKeyRecord.value() != null ? fixedKeyRecord.value().toString() : null,
              s,
              System.nanoTime()));


      this.context.forward(fixedKeyRecord, s);
    }

    @Override
    public <K extends KForward, V extends VForward> void forward(Record<K, V> fixedKeyRecord) {


      Header lineage = fixedKeyRecord.headers().lastHeader("lineage");
      if (lineage == null) {
        lineage = new RecordHeader("lineage", id());
        fixedKeyRecord.headers().add(lineage);
      }

      List<ProcessorRecord> list = records.computeIfAbsent(new String(lineage.value()), key -> new LinkedList<>());
      list.add(new ProcessorRecord(
              Thread.currentThread().getName(),
              context.recordMetadata().orElse(EMPTY).topic(),
              context.recordMetadata().orElse(EMPTY).partition(),
              name,
              "out",
              fixedKeyRecord.key() != null ? fixedKeyRecord.key().toString() : null,
              null, //fixedKeyRecord.value() != null ? fixedKeyRecord.value().toString() : null,
              "*",
              System.nanoTime()));


      //fixedKeyRecord.headers()
      System.out.println("***************************************** Fixed Process Record > " + fixedKeyRecord);

      System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
      System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
      System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
      System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
      System.out.println(list);
      System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
      System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
      System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
      System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

      this.context.forward(fixedKeyRecord);
    }

    @Override
    public <K extends KForward, V extends VForward> void forward(Record<K, V> fixedKeyRecord, String s) {

      Header lineage = fixedKeyRecord.headers().lastHeader("lineage");
      if (lineage == null) {
        lineage = new RecordHeader("lineage", id());
        fixedKeyRecord.headers().add(lineage);
      }

      List<ProcessorRecord> list = records.computeIfAbsent(new String(lineage.value()), key -> new LinkedList<>());
      list.add(new ProcessorRecord(
              Thread.currentThread().getName(),
              context.recordMetadata().orElse(EMPTY).topic(),
              context.recordMetadata().orElse(EMPTY).partition(),
              name,
              "out",
              fixedKeyRecord.key() != null ? fixedKeyRecord.key().toString() : null,
              null, //fixedKeyRecord.value() != null ? fixedKeyRecord.value().toString() : null,
              s,
              System.nanoTime()));


      //System.out.println("***************************************** Fixed Process Record > " + fixedKeyRecord + " with target topic " + s);
      this.context.forward(fixedKeyRecord, s);
    }


    public String applicationId() {
      return this.context.applicationId();
    }

    public TaskId taskId() {
      return this.context.taskId();
    }

    public Optional<RecordMetadata> recordMetadata() {
      return this.context.recordMetadata();
    }

    public Serde<?> keySerde() {
      return this.context.keySerde();
    }

    public Serde<?> valueSerde() {
      return this.context.valueSerde();
    }

    public File stateDir() {
      return this.context.stateDir();
    }

    public <S extends StateStore> S getStateStore(String var1) {
      return this.context.getStateStore(var1);
    }

    public Cancellable schedule(Duration var1, PunctuationType var2, Punctuator var3) {
      return this.context.schedule(var1, var2, var3);
    }

    public void commit() {
      this.context.commit();
    }

    public Map<String, Object> appConfigs() {
      return this.context.appConfigs();
    }

    public Map<String, Object> appConfigsWithPrefix(String var1) {
      return this.context.appConfigsWithPrefix(var1);
    }

    public long currentSystemTimeMs() {
      return this.context.currentSystemTimeMs();
    }

    public long currentStreamTimeMs() {
      return this.context.currentStreamTimeMs();
    }


    @Override
    public StreamsMetricsImpl metrics() {
      return this.context.metrics();
    }

    @Override
    public void setSystemTimeMs(long timeMs) {
      this.context.setSystemTimeMs(timeMs);
    }

    @Override
    public ProcessorRecordContext recordContext() {
      return this.context.recordContext();
    }

    @Override
    public void setRecordContext(ProcessorRecordContext recordContext) {
      this.context.setRecordContext(recordContext);
    }

    @Override
    public void setCurrentNode(ProcessorNode<?, ?, ?, ?> currentNode) {
      this.context.setCurrentNode(currentNode);

    }

    @Override
    public ProcessorNode<?, ?, ?, ?> currentNode() {
      return this.context.currentNode();
    }

    @Override
    public ThreadCache cache() {
      return this.context.cache();
    }

    @Override
    public void initialize() {
      this.context.initialize();
    }

    @Override
    public void uninitialize() {
      this.context.uninitialize();
    }

    @Override
    public Task.TaskType taskType() {
      return this.context.taskType();
    }

    @Override
    public void transitionToActive(StreamTask streamTask, RecordCollector recordCollector, ThreadCache newCache) {
      this.context.transitionToActive(streamTask, recordCollector, newCache);
    }

    @Override
    public void transitionToStandby(ThreadCache newCache) {
      this.context.transitionToStandby(newCache);
    }

    @Override
    public void registerCacheFlushListener(String namespace, ThreadCache.DirtyEntryFlushListener listener) {
      this.context.registerCacheFlushListener(namespace, listener);
    }

    @Override
    public <T extends StateStore> T getStateStore(StoreBuilder<T> builder) {
      return InternalProcessorContext.super.getStateStore(builder);
    }

    @Override
    public void logChange(String storeName, Bytes key, byte[] value, long timestamp, Position position) {
      this.context.logChange(storeName, key, value, timestamp, position);
    }

    @Override
    public String changelogFor(String storeName) {
      return this.context.changelogFor(storeName);
    }

    @Override
    public void addProcessorMetadataKeyValue(String key, long value) {
      this.context.addProcessorMetadataKeyValue(key, value);
    }

    @Override
    public Long processorMetadataForKey(String key) {
      return this.context.processorMetadataForKey(key);
    }

    @Override
    public void setProcessorMetadata(ProcessorMetadata metadata) {
      this.context.setProcessorMetadata(metadata);
    }

    @Override
    public ProcessorMetadata processorMetadata() {
      return this.context.processorMetadata();
    }

    @Override
    public void register(StateStore store, StateRestoreCallback stateRestoreCallback) {
      this.context.register(store, stateRestoreCallback);
    }

    @Override
    public <K, V> void forward(K key, V value) {
      this.context.forward(key, value);
    }

    @Override
    public <K, V> void forward(K key, V value, To to) {
        this.context.forward(key, value, to);
    }

    @Override
    public String topic() {
      return this.context.topic();
    }

    @Override
    public int partition() {
      return this.context.partition();
    }

    @Override
    public long offset() {
      return this.context.offset();
    }

    @Override
    public Headers headers() {
      return null;
    }

    @Override
    public long timestamp() {
      return 0;
    }

    @Override
    public void register(StateStore store, StateRestoreCallback stateRestoreCallback, CommitCallback commitCallback) {
      this.context.register(store, stateRestoreCallback, commitCallback);
    }

  }

}
