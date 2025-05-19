package io.kineticedge.kstutorial.common.streams.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.kineticedge.kstutorial.common.streams.metadata.StreamInfo;
import io.kineticedge.kstutorial.common.streams.util.StreamInternalsUtil;
import io.kineticedge.kstutorial.common.util.JsonUtil;
import org.apache.kafka.streams.KafkaStreams;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.locks.LockSupport;

public class ThreadsHandler implements HttpHandler {

  private final KafkaStreams kafkaStreams;

  public ThreadsHandler(final KafkaStreams kafkaStreams) {
    this.kafkaStreams = kafkaStreams;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    {
      exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
      exchange.getResponseHeaders().set("Cache-Control", "no-cache");
      exchange.getResponseHeaders().set("Connection", "keep-alive");
      exchange.sendResponseHeaders(200, 0);

      //applicationConfigs.getString("application.id");
      try (OutputStream os = exchange.getResponseBody()) {
        while (!Thread.currentThread().isInterrupted()) {
          long startTime = System.nanoTime();

          // Generate and send SSE data
          StreamInfo streamInfo = new StreamInfo(
                  StreamInternalsUtil.printApplicationId(kafkaStreams),
                  StreamInternalsUtil.monitorLastCommit(kafkaStreams),
                  StreamInternalsUtil.printStreamTimesBySubtopology(kafkaStreams)
          );
          String data = "data: " + JsonUtil.objectMapper().writeValueAsString(streamInfo) + "\n\n";
          os.write(data.getBytes());
          os.flush();

          // Wait exactly 100ms
          long duration = System.nanoTime() - startTime;
          long remainingTimeNanos = 1000_000_000 - duration; // 100ms in nanoseconds
          if (remainingTimeNanos > 0) {
            LockSupport.parkNanos(remainingTimeNanos);
          }
//          try {
//            Thread.sleep(2000);
//          } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//          }
        }
      } catch (IOException e) {
        System.err.println("Client disconnected: " + e.getMessage());
      } finally {
        exchange.close();
      }
    }
  }
}
