package io.kineticedge.kstutorial.common.serde;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;

import static io.kineticedge.kstutorial.common.util.JsonUtil.objectMapper;

public class JsonDeserializer<T> implements Deserializer<T> {

    public static final String OBJECT_TYPE_KEY = "_type";

    @SuppressWarnings("unused")
    public JsonDeserializer() {
        // needed by jackson
    }

    @Override
    public T deserialize(String topic, byte[] bytes) {

        if (bytes == null) {
            return null;
        }

        try {
            JsonNode node = objectMapper().readTree(bytes);

            if (node.get(OBJECT_TYPE_KEY) == null || !node.get(OBJECT_TYPE_KEY).isTextual()) {
                throw new SerializationException("missing 'type' field.");
            }

            return read(node.get(OBJECT_TYPE_KEY).asText(), node);
        } catch (final IOException e) {
            throw new SerializationException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private T read(final String className, JsonNode jsonNode) {
        try {
            return (T) objectMapper().convertValue(jsonNode, Class.forName(className));
        } catch (final ClassNotFoundException e) {
            throw new SerializationException(e);
        }
    }

}