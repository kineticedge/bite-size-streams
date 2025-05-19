package io.kineticedge.kstutorial.common.streams.util;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;


public class ClasspathStaticFileHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      // Add CORS headers
      addCORSHeaders(exchange);

      // Get requested URI
      String requestURI = exchange.getRequestURI().getPath();

      if (requestURI.equals("/")) {
        requestURI = "/index.html"; // Default to index.html
      }

      // Look for the file in the classpath (inside resources)
      String filePath = requestURI; // Files should be placed under "resources/static"
      InputStream fileStream = getResource(filePath.substring(1));

      if (fileStream != null) {
        // File found, send 200 response with file content
        byte[] fileBytes = fileStream.readAllBytes();
        String contentType = getContentType(filePath);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, fileBytes.length);

        // Write file bytes to response
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(fileBytes);
        outputStream.close();
      } else {
        // File not found, return 404
        String notFoundMessage = "404 Not Found\n";
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(404, notFoundMessage.length());
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(notFoundMessage.getBytes());
        outputStream.close();
      }
    }

    // Helper method to load a resource file from the classpath
    private InputStream getResource(String path) {
      return getClass().getClassLoader().getResourceAsStream(path);
    }

    // Helper method to determine content type
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

    // CORS headers
    private void addCORSHeaders(HttpExchange exchange) {
      exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
      exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
      exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
  }
