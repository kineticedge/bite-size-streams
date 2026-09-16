package io.kineticedge.kstutorial.common.streams.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.kineticedge.kstutorial.common.streams.util.KafkaStreamsTopologyToDot;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.internals.InternalTopologyBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class TopologyHandler implements HttpHandler {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TopologyHandler.class);

  private final byte[] dot;

  private KafkaStreamsTopologyToDot kafkaStreamsTopologyToDot;

  public TopologyHandler(String applicationId, Topology topology) {

    kafkaStreamsTopologyToDot = new KafkaStreamsTopologyToDot();

    InternalTopologyBuilder x = internalTopologyBuilder(topology);
    Map<String, String> map = storeToChangelogTopic(x);

    this.dot = kafkaStreamsTopologyToDot.convertTopoToDot(applicationId, topology.describe().toString(), map).getBytes(StandardCharsets.UTF_8);

    log.info("topology: {}", new String(dot));
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    exchange.getResponseHeaders().set("Content-Type", "html/text");
    exchange.sendResponseHeaders(200, 0);
    try (exchange; OutputStream os = exchange.getResponseBody()) {
      os.write(dot);
    }
  }

  private static InternalTopologyBuilder internalTopologyBuilder(Topology topology) {
    try {
      Field field = Topology.class.getDeclaredField("internalTopologyBuilder");
      field.setAccessible(true);
      return (InternalTopologyBuilder) field.get(topology);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, String> storeToChangelogTopic(InternalTopologyBuilder topology) {
    try {
      Field field = InternalTopologyBuilder.class.getDeclaredField("storeToChangelogTopic");
      field.setAccessible(true);
      return (Map<String, String>) field.get(topology);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

}
