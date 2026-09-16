package io.kineticedge.kstutorial.common.serde;


import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

import static io.kineticedge.kstutorial.common.util.JsonUtil.objectMapper;

public class JsonSerializer<T> implements Serializer<T> {

    @SuppressWarnings("unused")
    public JsonSerializer() {
        // needed by jackson
    }

    @Override
    public void configure(Map<String, ?> props, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, T data) {

        if (data == null)
            return null;

        try {
            return objectMapper().writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException("Error serializing JSON message", e);
        }
    }

    @Override
    public void close() {
    }
}
