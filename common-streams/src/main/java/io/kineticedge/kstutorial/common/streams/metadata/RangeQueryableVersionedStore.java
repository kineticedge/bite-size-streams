package io.kineticedge.kstutorial.common.streams.metadata;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.processor.StateStoreContext;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.query.Position;
import org.apache.kafka.streams.query.PositionBound;
import org.apache.kafka.streams.query.Query;
import org.apache.kafka.streams.query.QueryConfig;
import org.apache.kafka.streams.query.QueryResult;
import org.apache.kafka.streams.query.RangeQuery;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.VersionedKeyValueStore;
import org.apache.kafka.streams.state.VersionedRecord;
import org.apache.kafka.streams.state.internals.StoreQueryUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class RangeQueryableVersionedStore<K, V> implements VersionedKeyValueStore<K, V>, StoreQueryUtils.QueryHandler {

  private final VersionedKeyValueStore<K, V> inner;

  private final long retention;

  private final Map<Pair<K, Long>, V> cache = new TreeMap<>();

  public RangeQueryableVersionedStore(VersionedKeyValueStore<K, V> inner, long retention) {
    this.inner = inner;
    this.retention = retention;
  }

  @Override
  public <R> QueryResult<R> query(Query<R> query, PositionBound positionBound, QueryConfig config) {
    if (query instanceof RangeQuery) {
      RangeQuery<K, V> rangeQuery = (RangeQuery<K, V>) query;
      // Implement your own range logic here using reflection or known keys
      KeyValueIterator<K, VersionedRecord<V>> iterator = customRangeScan(rangeQuery);
      return QueryResult.forResult((R) iterator);
    }
    return QueryResult.forUnknownQueryType(query, this);
  }

  // Delegate all other VersionedKeyValueStore methods to `inner`
  @Override public VersionedRecord<V> get(K key) {
    System.out.println("** GET : " + key);
    return inner.get(key);
  }
  @Override public VersionedRecord<V> get(K key, long timestamp) {
    System.out.println("** GET w/timestamp : " + key  + " : " + timestamp);
    return inner.get(key, timestamp);
  }
  @Override public long put(K key, V value, long timestamp) {
    System.out.println("** PUT : " + key + " : " + value + " : " + timestamp);

    cache.put(Pair.of(key, timestamp), value);

    return inner.put(key, value, timestamp);
  }
  @Override public VersionedRecord<V> delete(K key, long timestamp) {
    System.out.println("** DELETE w/timestamp : " + key  + " : " + timestamp);

    cache.remove(Pair.of(key, timestamp));

    return inner.delete(key, timestamp);
  }
  @Override public String name() { return inner.name(); }
  //@Override public void init(ProcessorContext context, StateStore root) { inner.init(context, root); }
  @Override public void init(StateStoreContext stateStoreContext, StateStore root) {inner.init(stateStoreContext, root);}
  @Override public void flush() { inner.flush(); }
  @Override public void close() { inner.close(); }
  @Override public boolean isOpen() { return inner.isOpen(); }
  @Override public Position getPosition() {
    System.out.println("POSITION : ");
    return inner.getPosition();
  }
  @Override public boolean persistent() { return inner.persistent(); }

  @Override
  public QueryResult<?> apply(Query<?> query, PositionBound positionBound, QueryConfig config, StateStore store) {
    return query(query, positionBound, config);
  }

  private void clear() {
    long currentTime = System.currentTimeMillis();
    long cutoffTime = currentTime - retention;

// Group by key and find the latest timestamp for each key
    Map<K, Long> latestTimestampPerKey = cache.entrySet().stream()
            .collect(Collectors.toMap(
                    e -> e.getKey().getLeft(),
                    e -> e.getKey().getRight(),
                    Math::max
            ));

// Remove entries that are expired AND not the latest for their key
    cache.entrySet().removeIf(e ->
            (e.getValue() == null || e.getKey().getRight() < cutoffTime)
                    && e.getKey().getRight() < latestTimestampPerKey.get(e.getKey().getLeft())
    );
  }

  // Your custom range scan logic
  private KeyValueIterator<K, VersionedRecord<V>> customRangeScan(RangeQuery<K, V> query) {
//    // Use reflection or known keys to scan the store
//    throw new UnsupportedOperationException("*** Custom range scan not implemented yet");

    //remove old values
    //cache.entrySet().removeIf(e -> e.getValue() == null || e.getKey().getRight() + retention < System.currentTimeMillis());
    clear();

    return new KeyValueIterator<K, VersionedRecord<V>>() {

      Iterator<Map.Entry<Pair<K, Long>, V>> iterator = cache.entrySet().iterator();

      @Override
      public void close() {
      }

      @Override
      public K peekNextKey() {
        throw new UnsupportedOperationException("peekNextKey not implemented yet");
      }

      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public KeyValue<K, VersionedRecord<V>> next() {
        Map.Entry<Pair<K, Long>, V> next = iterator.next();
        return KeyValue.pair(next.getKey().getLeft(), new VersionedRecord<>(next.getValue(), next.getKey().getRight()));
      }
    };
  }
}