package io.kineticedge.kstutorial.common.streams;

import org.apache.commons.lang3.SerializationException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.metrics.Sensor;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.StreamsMetrics;
import org.apache.kafka.streams.errors.DeserializationExceptionHandler;
import org.apache.kafka.streams.errors.ErrorHandlerContext;
import org.apache.kafka.streams.processor.Cancellable;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.Punctuator;
import org.apache.kafka.streams.processor.StateRestoreCallback;
import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.processor.TaskId;
import org.apache.kafka.streams.processor.To;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

class ThrottlingDeserializationExceptionHandlerTest {


    @Test
    void testSchemaRegistryRestClientException() {
        ThrottlingDeserializationExceptionHandler handler = new ThrottlingDeserializationExceptionHandler();

        handler.configure(Map.of(ThrottlingDeserializationExceptionHandler.THROTTLING_DESERIALIZATION_EXCEPTION_THRESHOLD, 100.0));

        SerializationException ee = new SerializationException("cannot deserialize");
        DeserializationExceptionHandler.DeserializationHandlerResponse response = handler.handle(errorHandlingContext, new ConsumerRecord<>("topic", 0, 0L, "key".getBytes(), "value".getBytes()), ee);

        // ensure that RestClient exception results in a hard failure, regardless of threshold.
        Assertions.assertEquals(DeserializationExceptionHandler.DeserializationHandlerResponse.FAIL, response);
    }

    private ErrorHandlerContext errorHandlingContext = new ErrorHandlerContext() {
      @Override
      public String topic() {
        return "";
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
      public String processorNodeId() {
        return "";
      }

      @Override
      public TaskId taskId() {
        return null;
      }

      @Override
      public long timestamp() {
        return 0;
      }

      @Override
      public byte[] sourceRawKey() {
        return new byte[0];
      }

      @Override
      public byte[] sourceRawValue() {
        return new byte[0];
      }
    };

}