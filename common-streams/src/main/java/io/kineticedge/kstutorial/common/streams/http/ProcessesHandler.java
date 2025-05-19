package io.kineticedge.kstutorial.common.streams.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.kineticedge.kstutorial.common.util.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Returns the List of Processes Rooted at the parent and sorted by parent/child -- depth first
 */
public class ProcessesHandler implements HttpHandler {

  private final ObjectMapper objectMapper = JsonUtil.objectMapper();
  private final OperatingSystem operatingSystem = new SystemInfo().getOperatingSystem();

  public ProcessesHandler() {
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    // Set response content type to JSON
    exchange.getResponseHeaders().set("Content-Type", "application/json");

    try (OutputStream os = exchange.getResponseBody()) {
      // Initialize response JSON array
      StringBuilder responseJson = new StringBuilder("[");
      List<OSProcess> processes = operatingSystem.getProcesses(null, OperatingSystem.ProcessSorting.PID_ASC, 0);
      //List<oshi.software.os.OSProcess> processes = operatingSystem.getProcesses(List.of(1));

      // Step 2: Use TreeMap to organize parent-child relationships
      TreeMap<Integer, List<OSProcess>> processTree = new TreeMap<>();

      // Populate the TreeMap (key: parent PID, value: list of children processes)
      for (OSProcess process : processes) {
        int parentId = process.getParentProcessID();
        processTree.computeIfAbsent(parentId, k -> new ArrayList<>()).add(process);
      }


      List<JsonNode> list = new ArrayList<>();

      ObjectNode node = JsonUtil.objectMapper().createObjectNode();
      node.put("id", String.valueOf(1));
      node.put("label", "1:ROOT");
      list.add(node);

      // Step 3: Perform a recursive traversal starting from the root process (PID 1)
      System.out.println("Process Tree (Using TreeMap):");

      int rootPid = 1; // Typically, PID 1 is `launchd` (macOS) or `systemd` (Linux)
      traverseProcessTree(processTree, rootPid, 1, list);

      ArrayNode arrayNode = objectMapper.valueToTree(list);

      byte[] responseBytes = objectMapper.writeValueAsBytes(arrayNode);
      exchange.sendResponseHeaders(200, responseBytes.length);
      os.write(responseBytes);
    } catch (IOException e) {
      // Handle IO exceptions
      exchange.sendResponseHeaders(500, -1);
    } finally {
      exchange.close();
    }

  }


  private static void traverseProcessTree(Map<Integer, List<OSProcess>> processTree, int currentPid, int depth, List<JsonNode> list) {
    // Get the list of child processes for the current PID
    List<OSProcess> children = processTree.getOrDefault(currentPid, Collections.emptyList());

    for (OSProcess child : children) {
      // Print the details of the current child process
      //System.out.println(" ".repeat(depth * 2) + "PID: " + child.getProcessID() + ", Name: " + child.getName());

      ObjectNode node = JsonUtil.objectMapper().createObjectNode();
      node.put("id", String.valueOf(child.getProcessID()));
      node.put("label", "\u00A0\u00A0".repeat(depth * 2) + child.getProcessID() + ":" +  StringUtils.truncate(child.getName(), 25));
      list.add(node);

      // Recursively traverse this child's subtree
      traverseProcessTree(processTree, child.getProcessID(), depth + 1, list);
    }
  }

}
