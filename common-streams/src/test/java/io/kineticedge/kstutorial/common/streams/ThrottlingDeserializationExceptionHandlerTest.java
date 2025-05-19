package io.kineticedge.kstutorial.common.streams;

import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.metrics.Sensor;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.StreamsMetrics;
import org.apache.kafka.streams.processor.Cancellable;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.Punctuator;
import org.apache.kafka.streams.processor.StateRestoreCallback;
import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.processor.TaskId;
import org.apache.kafka.streams.processor.To;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

class ThrottlingDeserializationExceptionHandlerTest {


//    @Test
//    public void testSchemaRegistryRestClientException() {
//        ThrottlingDeserializationExceptionHandler handler = new ThrottlingDeserializationExceptionHandler();
//
//        handler.configure(Map.of(ThrottlingDeserializationExceptionHandler.THROTTLING_DESERIALIZATION_EXCEPTION_THRESHOLD, 100.0));
//
//        //RestClientException e = new RestClientException("bad", 0, 0);
//
//        SerializationException ee = new SerializationException(e);
//        DeserializationExceptionHandler.DeserializationHandlerResponse response = handler.handle(processorContext, new ConsumerRecord<>("topic", 0, 0L, "key".getBytes(), "value".getBytes()), ee);
//
//        // ensure that RestClient exception results in a hard failure, regardless of threshold.
//        Assertions.assertEquals(DeserializationExceptionHandler.DeserializationHandlerResponse.FAIL, response);
//    }


    //

    private ProcessorContext processorContext = new ProcessorContext() {
        @Override
        public String applicationId() {
            return null;
        }

        @Override
        public TaskId taskId() {
            return new TaskId(1, 0);
        }

        @Override
        public Serde<?> keySerde() {
            return null;
        }

        @Override
        public Serde<?> valueSerde() {
            return null;
        }

        @Override
        public File stateDir() {
            return null;
        }

        @Override
        public StreamsMetrics metrics() {
            return new StreamsMetrics() {
                @Override
                public Map<MetricName, ? extends Metric> metrics() {
                    return new HashMap<>();
                }

                @Override
                public Sensor addLatencyRateTotalSensor(String scopeName, String entityName, String operationName, Sensor.RecordingLevel recordingLevel, String... tags) {
                    return null;
                }

                @Override
                public Sensor addRateTotalSensor(String scopeName, String entityName, String operationName, Sensor.RecordingLevel recordingLevel, String... tags) {
                    return null;
                }

                @Override
                public Sensor addSensor(String name, Sensor.RecordingLevel recordingLevel) {
                    return null;
                }

                @Override
                public Sensor addSensor(String name, Sensor.RecordingLevel recordingLevel, Sensor... parents) {
                    return null;
                }

                @Override
                public void removeSensor(Sensor sensor) {

                }
            };
        }

        @Override
        public void register(StateStore store, StateRestoreCallback stateRestoreCallback) {
        }

        @Override
        public <S extends StateStore> S getStateStore(String name) {
            return null;
        }

        @Override
        public Cancellable schedule(Duration interval, PunctuationType type, Punctuator callback) {
            return null;
        }

        @Override
        public <K, V> void forward(K key, V value) {

        }

        @Override
        public <K, V> void forward(K key, V value, To to) {

        }

        @Override
        public void commit() {

        }

        @Override
        public String topic() {
            return null;
        }

        @Override
        public int partition() {
            return 0;
        }

        @Override
        public long offset() {
            return 0;
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
        public Map<String, Object> appConfigs() {
            return null;
        }

        @Override
        public Map<String, Object> appConfigsWithPrefix(String prefix) {
            return null;
        }

        @Override
        public long currentSystemTimeMs() {
            return 0;
        }

        @Override
        public long currentStreamTimeMs() {
            return 0;
        }
    };
}