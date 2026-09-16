package io.kineticedge.kstutorial.common.serde;

import com.fasterxml.jackson.databind.JsonNode;
import io.kineticedge.kstutorial.common.util.JsonUtil;
import io.kineticedge.kstutorial.common.util.XmlUtil;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;

public class XmlDeserializer<T> implements Deserializer<T> {

    public static final String OBJECT_TYPE_KEY = "_type";

    @SuppressWarnings("unused")
    public XmlDeserializer() {
        // needed by jackson
    }

    @Override
    public T deserialize(String topic, byte[] bytes) {

        if (bytes == null) {
            return null;
        }

        try {
            JsonNode node = XmlUtil.objectMapper().readTree(bytes);

            if (node.get(OBJECT_TYPE_KEY) == null || !node.get(OBJECT_TYPE_KEY).isTextual()) {
                throw new SerializationException("missing 'type' field.");
            }

           // System.out.println(node);
            //System.out.println("*****");

            return read(node.get(OBJECT_TYPE_KEY).asText(), node);
        } catch (final IOException e) {
            throw new SerializationException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private T read(final String className, JsonNode jsonNode) {
        try {
            return (T) JsonUtil.objectMapper().convertValue(jsonNode, Class.forName(className));
        } catch (final ClassNotFoundException e) {
            throw new SerializationException(e);
        }
    }

}