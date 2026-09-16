package io.kineticedge.kstutorial.common.streams.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class StaticFileHandler implements HttpHandler {
  @Override
  public void handle(HttpExchange exchange) throws IOException {
    // Add CORS headers
    addCORSHeaders(exchange);

    // Extract the requested URI
    String requestURI = exchange.getRequestURI().getPath();
    if (requestURI.equals("/")) {
      requestURI = "/index.html"; // Default to index.html
    }

    // Determine the file path
    String filePath = "static" + requestURI; // Serve files from the "static" folder
    File file = new File(filePath);

    // Check if file exists and is not a directory
    if (file.exists() && !file.isDirectory()) {
      // Set the content type based on the file extension
      String contentType = getContentType(filePath);

      // Send successful response with file content
      byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
      exchange.getResponseHeaders().set("Content-Type", contentType);
      exchange.sendResponseHeaders(200, fileBytes.length);
      OutputStream outputStream = exchange.getResponseBody();
      outputStream.write(fileBytes);
      outputStream.close();
    } else {
      // Send 404 response if file not found
      String notFoundMessage = "404 Not Found\n";
      exchange.getResponseHeaders().set("Content-Type", "text/plain");
      exchange.sendResponseHeaders(404, notFoundMessage.length());
      OutputStream outputStream = exchange.getResponseBody();
      outputStream.write(notFoundMessage.getBytes());
      outputStream.close();
    }
  }

  // Function to determine content type based on file extension
  private String getContentType(String filePath) {
    if (filePath.endsWith(".html")) {
      return "text/html";
    } else if (filePath.endsWith(".css")) {
      return "text/css";
    } else if (filePath.endsWith(".js")) {
      return "application/javascript";
    } else if (filePath.endsWith(".json")) {
      return "application/json";
    } else if (filePath.endsWith(".png")) {
      return "image/png";
    } else if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) {
      return "image/jpeg";
    } else {
      return "application/octet-stream";
    }
  }

  // Function to add CORS headers
  private void addCORSHeaders(HttpExchange exchange) {
    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
  }
}