package io.kineticedge.kstutorial.common.streams.ws;

import org.apache.commons.lang3.StringUtils;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.WebSocket;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SimpleWebSocketServer extends WebSocketServer {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SimpleWebSocketServer.class);

  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  private final Map<WebSocket, EventsHandler> activeSessions = new ConcurrentHashMap<>();

  public SimpleWebSocketServer(int port) {
    super(new InetSocketAddress(port));
  }

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {
    activeSessions.put(conn, new EventsHandler(conn));
    System.out.println(Thread.currentThread().getName() + " New connection: " + conn.getRemoteSocketAddress());
  }

  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    log.debug("Closed connection: {} with exit code {} additional info: {}", conn.getRemoteSocketAddress(), code, reason);
    EventsHandler handler = activeSessions.get(conn);
    if (handler != null) {
      handler.close();
    }
    activeSessions.remove(conn);
  }

  private static String topic(final String message) {
    final int lastSlash = message.lastIndexOf('/');
    return lastSlash < 0 ? message : message.substring(0, message.lastIndexOf('/'));
  }

  private static Integer partition(final String message) {
    final int lastSlash = message.lastIndexOf('/');
    return lastSlash < 0 ? null : Integer.parseInt(message.substring(message.lastIndexOf('/') + 1));
  }

  @Override
  public void onMessage(WebSocket conn, String message) {
    log.info("Received message: {}", message);

    EventsHandler handler = activeSessions.get(conn);

    if ("_rewind".equals(message)) {
      handler.rewind();
    } else {
      if (handler != null) {
        executor.submit(() -> {
          try {
            handler.handle(topic(message), partition(message));
          } catch (Exception e) {
            log.error("Error handling message", e);
          }
        });
      }
    }
  }

  @Override
  public void onError(WebSocket conn, Exception ex) {
    log.error("WebSocket error", ex);
  }

  @Override
  public void onStart() {
    log.info("WebSocket server started on port {}", getPort());
  }

  // Method to send a message to a specific client
  public void sendToAll(String message) {
    broadcast(message);
  }

}