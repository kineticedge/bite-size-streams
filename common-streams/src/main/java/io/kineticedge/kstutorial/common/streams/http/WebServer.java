package io.kineticedge.kstutorial.common.streams.http;

import io.kineticedge.kstutorial.common.config.ProducerConfig;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.common.streams.util.ClasspathStaticFileHandler;
import io.kineticedge.kstutorial.producer.Emitter;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WebServer {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebServer.class);

  private final com.sun.net.httpserver.HttpServer httpServer;

  private final ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);

  public WebServer(int port, String applicationId, Topology topology, KafkaStreams kafkaStreams, Map<String, String> metadata, Map<String, String> producerMetadata) throws IOException {

    httpServer = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port), 100);

    ThreadPoolExecutor executor = new ThreadPoolExecutor(
            20, 50, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy());

    monitor.scheduleAtFixedRate(() -> {
      log.info(
              "Thread stats\nActive threads: {}\nPool size: {}\nTask count: {}\nCompleted tasks: {}\nQueue size: {}\n",
              executor.getActiveCount(),
              executor.getPoolSize(),
              executor.getTaskCount(),
              executor.getCompletedTaskCount(),
              executor.getQueue().size()
      );
    }, 0, 60, TimeUnit.SECONDS);


    httpServer.setExecutor(executor);

    final Emitter emitter = new Emitter("localhost:9092", producerMetadata);

    httpServer.createContext("/static", new ClasspathStaticFileHandler());
    httpServer.createContext("/topology", new TopologyHandler(applicationId, topology));
    httpServer.createContext("/emit", new EmitHandler(emitter));
    httpServer.createContext("/check", new CheckHandler(emitter));
    httpServer.createContext("/processes", new ProcessesHandler());
    httpServer.createContext("/threads", new ThreadsHandler(kafkaStreams, metadata, producerMetadata));
    httpServer.createContext("/stores", new StoresHandler(kafkaStreams));
    httpServer.createContext("/metrics", new MetricsHandler());
    httpServer.createContext("/metrics2", new MetricsHandler());
    //httpServer.createContext("/publish", new PublishHandler());
//    httpServer.createContext("/lineage", new LineageHandler());
  }

  public void start() {
    httpServer.start();
    log.info("Http Server started at: {}", httpServer.getAddress());

    openBrowser("http://localhost:" + httpServer.getAddress().getPort() + "/static/index.html");
  }

  public void stop() {
    monitor.shutdown();
    httpServer.stop(0);
  }


  private void openBrowser(String url) {
    try {
      String os = System.getProperty("os.name").toLowerCase();

      if (os.contains("mac")) {
        // macOS: use AppleScript to reuse a named window
        String script = String.format(
                "tell application \"Google Chrome\"\n" +
                        "  set windowFound to false\n" +
                        "  repeat with w in windows\n" +
                        "    repeat with t in tabs of w\n" +
                        "      if URL of t starts with \"http://localhost:%d\" then\n" +
                        "        set URL of t to \"%s\"\n" +
                        "        set active tab index of w to (index of t)\n" +
                        "        set index of w to 1\n" +
                        "        set windowFound to true\n" +
                        "        exit repeat\n" +
                        "      end if\n" +
                        "    end repeat\n" +
                        "    if windowFound then exit repeat\n" +
                        "  end repeat\n" +
                        "  if not windowFound then\n" +
                        "    tell window 1 to make new tab with properties {URL:\"%s\"}\n" +
                        "  end if\n" +
                        "  activate\n" +
                        "end tell", httpServer.getAddress().getPort(), url, url);
        new ProcessBuilder("osascript", "-e", script).start();
      }
      log.info("Browser opened/navigated to: {}", url);
    } catch (Exception e) {
      log.warn("Could not open browser automatically: {}", e.getMessage());
    }
  }


}