package io.kineticedge.kstutorial.producer;

import java.io.*;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.*;

public class MacLogStreamer {

  private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
          .appendPattern("yyyy-MM-dd HH:mm:ss.")
          .appendValue(ChronoField.MICRO_OF_SECOND, 6) // handles microseconds
          .appendPattern("Z") // timezone offset like -0500
          .toFormatter();


  public static void main(String[] args) throws IOException {
    ProcessBuilder pb = new ProcessBuilder("log", "stream", "--color", "none", "--style", "ndjson");
    pb.redirectErrorStream(true); // merge stderr into stdout
    Process process = pb.start();

    ExecutorService executor = Executors.newSingleThreadExecutor();
    ObjectMapper mapper = new ObjectMapper();

    executor.submit(() -> {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          try {
            JsonNode json = mapper.readTree(line);

            ZonedDateTime zdt = ZonedDateTime.parse(json.path("timestamp").asText(), formatter);
            Instant instant = zdt.toInstant();

            // Instant timestamp = Instant.ofEpochMilli(json.path("machTimestamp").asLong()/1000);

            System.out.println(">> " + instant.toString());
            System.out.println("📘 ProcessId: " + json.path("processID").asText());
            System.out.println("📘 Subsystem: " + json.path("subsystem").asText());
            System.out.println("🕒 Timestamp: " + json.path("timestamp").asText());
            System.out.println("📝 Message: " + json.path("eventMessage").asText());
            System.out.println("—".repeat(40));
          } catch (Exception e) {
            System.err.println("⚠️ Failed to parse: " + line);
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }
}