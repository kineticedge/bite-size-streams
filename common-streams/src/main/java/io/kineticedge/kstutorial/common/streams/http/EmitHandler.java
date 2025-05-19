package io.kineticedge.kstutorial.common.streams.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.kineticedge.kstutorial.common.util.JsonUtil;
import io.kineticedge.kstutorial.producer.Emitter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class EmitHandler implements HttpHandler {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmitHandler.class);

  private Emitter emitter;

  private String process = "1";
  private Integer age = 0;
  private Boolean windowsOnly = Boolean.FALSE;

  public EmitHandler(Emitter emitter) {
    this.emitter = emitter;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {

    try {
      String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      Map<String, Object> formData = parseFormData(requestBody);

      final String mode = (String) formData.get("mode");

      if ("poison-pill".equalsIgnoreCase(mode)) {
        log.info("EmitHandler.handle() -- poison-pill");
        emitter.poisonPill();
        log.info("EmitHandler.handle() -- poison-pill -- done");
      } else {
        log.info("EmitHandler.handle() -- formData={}, mode={}", formData, mode);
        process = (String) formData.get("process");
        age = Integer.parseInt((String) formData.get("age"));
        windowsOnly = "on".equals(formData.get("windowsOnly"));
        log.info("Emitting process: {}, age={}, windowsOnly={}", process, age, windowsOnly);
        emitter.emit(Integer.parseInt(process), age, windowsOnly);
      }

      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, 0);
    } finally {
      log.info("EmitHandler.handle() -- close");
      exchange.close();
    }
  }

  private static Map<String, Object> parseFormData(String requestBody) {
    try {
      return JsonUtil.objectMapper().readValue(requestBody, new TypeReference<Map<String, Object>>() {
      });
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

}