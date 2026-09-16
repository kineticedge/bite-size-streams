package io.kineticedge.kstutorial.common.serde;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class XmlSerde<T> implements Serde<T> {

    public XmlSerde() {
        // needed by jackson
    }

    @Override
    public Serializer<T> serializer() {
        return new XmlSerializer<>();
    }

    @Override
    public Deserializer<T> deserializer() {
        return new XmlDeserializer<>();
    }
}
