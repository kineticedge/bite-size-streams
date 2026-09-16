package io.kineticedge.kstutorial.common.streams.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.kineticedge.kstutorial.common.streams.metadata.PortInfo;
import io.kineticedge.kstutorial.common.streams.util.KafkaStreamsTopologyToDot;
import io.kineticedge.kstutorial.common.util.JsonUtil;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.internals.InternalTopologyBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class PortHandler implements HttpHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PortHandler.class);

    private final int port;

    public PortHandler(String applicationId, int port) {
        this.port = port;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "html/text");
        exchange.sendResponseHeaders(200, 0);
        try (exchange; OutputStream os = exchange.getResponseBody()) {
            os.write(
                    JsonUtil.objectMapper().writeValueAsString(
                            new PortInfo(port)
                    ).getBytes(StandardCharsets.UTF_8)
            );
        }
    }


}
