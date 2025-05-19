
The domain leverages Jackson's JSON Type Info to embed the classname within the serialized JSON allowing the deserializer to to know what class to deserialize into.
This is what Avro and Protobuf Serdes do, making them the perferred way of building out Kafka Streams application, as the Serializer/Deserializer can be left out of the DSL as much as possible.
With JSON, this is typically not the case.
However, this approach gives enough information to the derserializer to properly marshal the data into the correct POJO leveraging reflection.
This makes a tutorial much easier to understand without the need of using a non human readable serializer and the need to spin up and maintain a schema registry.

```java
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "_type")
```

