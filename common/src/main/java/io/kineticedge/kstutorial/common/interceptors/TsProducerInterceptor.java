package io.kineticedge.kstutorial.common.interceptors;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TsProducerInterceptor<K, V> implements ProducerInterceptor<K, V> {

  private static final String HEADER = "ts";

  private static final LongSerializer serializer = new LongSerializer();

  @Override
  public void configure(Map<String, ?> configs) {
  }

  @Override
  public ProducerRecord<K, V> onSend(ProducerRecord<K, V> record) {
    record.headers().add(HEADER, serializer.serialize(record.topic(), System.currentTimeMillis()));
    return record;
  }

  @Override
  public void close() {
  }
}
