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

  public WebServer(int port, String applicationId, Topology topology, KafkaStreams kafkaStreams, Map<String, String> metadata, final long lingerMs) throws IOException {

    httpServer = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port), 100);

    ThreadPoolExecutor executor = new ThreadPoolExecutor(
            20, 50, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy());

    ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
    monitor.scheduleAtFixedRate(() -> {
      System.out.println("Active threads: " + executor.getActiveCount());
      System.out.println("Pool size: " + executor.getPoolSize());
      System.out.println("Task count: " + executor.getTaskCount());
      System.out.println("Completed tasks: " + executor.getCompletedTaskCount());
      System.out.println("Queue size: " + executor.getQueue().size());
    }, 0, 60, TimeUnit.SECONDS);


    httpServer.setExecutor(executor);

    final Emitter emitter = new Emitter("localhost:9092", lingerMs);

    httpServer.createContext("/static", new ClasspathStaticFileHandler());
    httpServer.createContext("/topology", new TopologyHandler(applicationId, topology));
    httpServer.createContext("/emit", new EmitHandler(emitter));
    httpServer.createContext("/check", new CheckHandler(emitter));
    httpServer.createContext("/processes", new ProcessesHandler());
    httpServer.createContext("/threads", new ThreadsHandler(kafkaStreams, metadata));
    httpServer.createContext("/stores", new StoresHandler(kafkaStreams));
    httpServer.createContext("/metrics", new MetricsHandler());
    httpServer.createContext("/metrics2", new MetricsHandler());
    //httpServer.createContext("/publish", new PublishHandler());
//    httpServer.createContext("/lineage", new LineageHandler());
  }

  public void start() {
    httpServer.start();
    log.info("Http Server started at: " + httpServer.getAddress());
  }

  public void stop() {
    httpServer.stop(0);
  }

}