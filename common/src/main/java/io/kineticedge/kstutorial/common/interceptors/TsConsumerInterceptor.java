package io.kineticedge.kstutorial.common.interceptors;


import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * The LineageConsumerInterceptor is an implementation of the ConsumerInterceptor interface
 * that provides custom logic for intercepting and processing consumed records from Kafka.
 *
 * @param <K> The key type of the consumed records.
 * @param <V> The value type of the consumed records.
 */
public class TsConsumerInterceptor<K, V> implements ConsumerInterceptor<K, V> {

  private static final Logger logger = LoggerFactory.getLogger(TsConsumerInterceptor.class);

  private static final String HEADER = "ts";

  private static final LongDeserializer deserializer = new LongDeserializer();

  @Override
  public void configure(Map<String, ?> configs) {
  }

  @Override
  public ConsumerRecords<K, V> onConsume(ConsumerRecords<K, V> records) {
    return records;
  }

  @Override
  public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
  }

  @Override
  public void close() {
  }

}
