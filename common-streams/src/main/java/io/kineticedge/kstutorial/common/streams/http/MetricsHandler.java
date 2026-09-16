package io.kineticedge.kstutorial.common.streams.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.kineticedge.kstutorial.common.streams.ProcessWrapperX;
import io.kineticedge.kstutorial.common.streams.PrometheusRegistryManager;
import io.kineticedge.kstutorial.common.util.JsonUtil;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.io.IOException;
import java.io.OutputStream;

public class MetricsHandler implements HttpHandler {


  public MetricsHandler() {
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {

    try {
      exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
      exchange.getResponseHeaders().set("Cache-Control", "no-cache");
      exchange.getResponseHeaders().set("Connection", "keep-alive");
      exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

      final String response;
      try {
        response = PrometheusRegistryManager.getRegistry().scrape();
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }

      exchange.sendResponseHeaders(200, response.getBytes().length);

      try (OutputStream os = exchange.getResponseBody()) {
        os.write(response.getBytes());
        os.flush();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

  }
}
