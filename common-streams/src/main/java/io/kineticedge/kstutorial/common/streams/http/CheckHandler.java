package io.kineticedge.kstutorial.common.streams.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.kineticedge.kstutorial.common.streams.metadata.StreamInfo;
import io.kineticedge.kstutorial.common.streams.util.StreamInternalsUtil;
import io.kineticedge.kstutorial.common.util.JsonUtil;
import io.kineticedge.kstutorial.producer.Emitter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class CheckHandler implements HttpHandler {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CheckHandler.class);

  private ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);

  private ScheduledFuture<?> scheduledFuture;

  private final Emitter emitter;

  public CheckHandler(final Emitter emitter) {
    this.emitter = emitter;
  }


  @Override
  public void handle(HttpExchange exchange) throws IOException {

    String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    Map<String, Object> formData = parseFormData(requestBody);

    System.out.println(formData);
    System.out.println(formData.containsKey("enabled") );
    System.out.println(formData.get("enabled").getClass());
    System.out.println(formData.containsKey("value") );
    System.out.println(formData.get("value").getClass());

    if (formData.containsKey("enabled") && Boolean.TRUE == formData.get("enabled")) {
      System.out.println("XX");

      if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
        System.out.println("Canceling scheduledFuture...");
        scheduledFuture.cancel(true);
        scheduledFuture = null;
      }

      scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(() -> {
        System.out.println("Checking streams...");
        emitter.emit();
      }, 0, Long.parseLong((String) formData.get("value")), TimeUnit.SECONDS);
    } else {
      if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
        System.out.println("Canceling scheduledFuture...");
        scheduledFuture.cancel(true);
        scheduledFuture = null;
      }
    }

    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, 0);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write("".getBytes());
      os.flush();
    } catch (IOException e) {
    }

    exchange.close();
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