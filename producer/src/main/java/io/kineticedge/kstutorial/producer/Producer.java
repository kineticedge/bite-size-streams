package io.kineticedge.kstutorial.producer;

import io.kineticedge.kstutorial.common.serde.JsonSerializer;
import io.kineticedge.kstutorial.domain.Id;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class Producer {

    private static final Logger log = LoggerFactory.getLogger(Producer.class);

    final KafkaProducer<String, Id> kafkaProducer;


    public Producer(final Options options) {
        kafkaProducer = new KafkaProducer<>(properties(options));
    }

    public Producer(final String bootstrapServers) {
        kafkaProducer = new KafkaProducer<>(properties(bootstrapServers));
    }

    public void flush() {
        kafkaProducer.flush();
    }

    public void close() {
        kafkaProducer.close();
    }

    public Future<RecordMetadata> publish(final String topic, final Id object, final long ts) {
        //log.info("Sending key={}, value={}", object.id(), object);
        return kafkaProducer.send(new ProducerRecord<>(topic, null, ts, object.id(), object), (metadata, exception) -> {
            if (exception != null) {
                log.error("error producing to kafka", exception);
            } else {
                log.debug("topic={}, partition={}, offset={}", metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }

    public Future<RecordMetadata> publish(final String topic, final Id object) {
        //log.info("Sending key={}, value={}", object.id(), object);
        return kafkaProducer.send(new ProducerRecord<>(topic, null, null, object.id(), object), (metadata, exception) -> {
            if (exception != null) {
                log.error("error producing to kafka", exception);
            } else {
                log.debug("topic={}, partition={}, offset={}", metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }


    private Map<String, Object> properties(final Options options) {
        return properties(options.bootstrapServers());
    }

    private Map<String, Object> properties(final String bootstrapServers) {
        Map<String, Object> defaults = Map.ofEntries(
                Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers),
                Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT"),
                Map.entry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()),
                Map.entry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName()),
                Map.entry(ProducerConfig.LINGER_MS_CONFIG, 50L),
                Map.entry(ProducerConfig.BATCH_SIZE_CONFIG, 100_000),
                Map.entry(ProducerConfig.ACKS_CONFIG, "all")
        );

        Map<String, Object> map = new HashMap<>(defaults);

        return map;
    }
}
