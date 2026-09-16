package io.kineticedge.kstutorial.common.streams.metadata;

import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.VersionedKeyValueStore;
import org.apache.kafka.streams.state.internals.VersionedKeyValueStoreBuilder;

import java.util.Map;

public class RangeQueryableVersionedStoreBuilder<K, V> implements StoreBuilder<VersionedKeyValueStore<K, V>> {

  private final StoreBuilder<VersionedKeyValueStore<K, V>> innerBuilder;


  public RangeQueryableVersionedStoreBuilder(StoreBuilder<VersionedKeyValueStore<K, V>> innerBuilder) {
    this.innerBuilder = innerBuilder;
  }


  @Override
  public StoreBuilder<VersionedKeyValueStore<K, V>> withLoggingEnabled(Map<String, String> config) {
    innerBuilder.withLoggingEnabled(config);
    return this;
  }

  @Override
  public StoreBuilder<VersionedKeyValueStore<K, V>> withLoggingDisabled() {
    innerBuilder.withLoggingDisabled();
    return this;
  }

  @Override
  public StoreBuilder<VersionedKeyValueStore<K, V>> withCachingEnabled() {
    innerBuilder.withCachingEnabled();
    return this;
  }

  @Override
  public StoreBuilder<VersionedKeyValueStore<K, V>> withCachingDisabled() {
    innerBuilder.withCachingDisabled();
    return this;
  }

  @Override
  public VersionedKeyValueStore<K, V> build() {
    VersionedKeyValueStore<K, V> original = innerBuilder.build();

    long retention = -1;
    if (innerBuilder instanceof VersionedKeyValueStoreBuilder<K,V> vkvsb) {
      retention = vkvsb.historyRetention();
    }

    return new RangeQueryableVersionedStore<>(original, retention); // your IQv2-enabled wrapper
  }

  @Override
  public Map<String, String> logConfig() {
    return innerBuilder.logConfig();
  }

  @Override
  public boolean loggingEnabled() {
    return innerBuilder.loggingEnabled();
  }

  @Override
  public String name() {
    return innerBuilder.name();
  }
}