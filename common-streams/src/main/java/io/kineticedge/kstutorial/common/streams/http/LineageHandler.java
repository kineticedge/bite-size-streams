package io.kineticedge.kstutorial.common.streams.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.kineticedge.kstutorial.common.streams.ProcessWrapperX;
import io.kineticedge.kstutorial.common.util.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;

public class LineageHandler implements HttpHandler {

  public LineageHandler() {
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    //String path = exchange.getRequestURI().getPath();
    //String store = getStoreFromPath(path);

    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.getResponseHeaders().set("Cache-Control", "no-cache");
    exchange.getResponseHeaders().set("Connection", "keep-alive");
    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
    exchange.sendResponseHeaders(200, 0);

    try (OutputStream os = exchange.getResponseBody()) {
      //os.write("[".getBytes());
      os.write(JsonUtil.objectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(ProcessWrapperX.getAl()));
      //os.write("]".getBytes());
    }

  }
}
