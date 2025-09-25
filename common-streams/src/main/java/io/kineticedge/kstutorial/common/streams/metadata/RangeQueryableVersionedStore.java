package io.kineticedge.kstutorial.common.streams.metadata;

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

public class RangeQueryableVersionedStore<K, V> implements VersionedKeyValueStore<K, V>, StoreQueryUtils.QueryHandler {

  private final VersionedKeyValueStore<K, V> inner;

  public RangeQueryableVersionedStore(VersionedKeyValueStore<K, V> inner) {
    this.inner = inner;
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
    return inner.put(key, value, timestamp);
  }
  @Override public VersionedRecord<V> delete(K key, long timestamp) {
    System.out.println("** DELETE w/timestamp : " + key  + " : " + timestamp);
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

  // Your custom range scan logic
  private KeyValueIterator<K, VersionedRecord<V>> customRangeScan(RangeQuery<K, V> query) {
    // Use reflection or known keys to scan the store
    throw new UnsupportedOperationException("*** Custom range scan not implemented yet");
  }
}