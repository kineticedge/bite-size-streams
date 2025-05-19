package io.kineticedge.kstutorial.common.streams.util;

import io.kineticedge.kstutorial.common.config.Options;
import io.kineticedge.kstutorial.common.util.PropertiesUtil;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class KafkaStreamsTopologyToDot {


  final AdminClient kafkaAdmin;

  public KafkaStreamsTopologyToDot() {
    this.kafkaAdmin = AdminClient.create(properties());
  }

  // Main function that converts topology to DOT format
  public  String convertTopoToDot(String applicationId,String topo, Map<String, String> map) {
    String[] lines = topo.split("\n");
    List<String> results = new ArrayList<>();
    Set<String> outside = new LinkedHashSet<>();
    Set<String> stores = new LinkedHashSet<>();
    Set<String> topics = new LinkedHashSet<>();
    Set<String> sames = new LinkedHashSet<>();
    String entityName = null;

    for (String line : lines) {
      // Parse Sub-topology sections
      Matcher subMatcher = Pattern.compile("Sub-topology: (\\d+)").matcher(line);
      if (subMatcher.find()) {
        if (!results.isEmpty()) results.add("}");
        results.add("subgraph cluster_" + subMatcher.group(1) + " {");
        results.add("\tlabel = \"Sub-topology: " + subMatcher.group(1) + "\";");
        results.add("\tstyle=filled;");
        results.add("\tcolor=lightgrey;");
        results.add("\tnode [style=filled,color=white];");
        continue;
      }

      // Parse Source, Processor, and Sink
      Matcher entityMatcher = Pattern.compile("(Source:|Processor:|Sink:)\\s+(\\S+)\\s+\\((topics|topic|stores):([^\\)]*)\\)").matcher(line);
      if (entityMatcher.find()) {
        entityName = processName(entityMatcher.group(2));
        String type = entityMatcher.group(3);

        String linkedNames = entityMatcher.group(4).replaceAll("[\\[\\]]", "");

        for (String linkedName : linkedNames.split(",")) {
          linkedName = processName(linkedName.trim());
          if (linkedName.isEmpty()) continue;

          final String ln = linkedName;

          if (type.equals("topics")) {
            outside.add("\"" + linkedName + "\" -> \"" + entityName + "\";");
            topics.add(linkedName);

            System.out.println(">> :" + ln + ":");
            int partitions = 0;
//            try {
//              DescribeTopicsResult r = kafkaAdmin.describeTopics(Collections.singleton(ln));
//              partitions = r.allTopicNames().get(10, TimeUnit.SECONDS).get(ln).partitions().size();
//            } catch (TimeoutException | ExecutionException | InterruptedException e) {
//              throw new RuntimeException(e);
//            }
            partitions = 2;

            final StringBuilder ll = new StringBuilder("{ rank=same; \"" + linkedName + "\";");
            IntStream.range(0, partitions).forEach(i -> {
              final String xx = ln + "/" + i;
              outside.add("\"" + ln + "\" -> \"" + xx + "\" [style=invis];");
              topics.add(xx);
//              System.out.println(" \"" + ln + "\";");
              ll.append(" \"" + xx + "\";");
            });
            ll.append("}");
            sames.add(ll.toString());
            //sames.add("{ rank=same; \"" + linkedName + "\"; \"" + linkedName + "/0\"; \"" + linkedName + "/1\"; }");

//            //NJB
//            outside.add("\"" + linkedName + "\" -> \"" + linkedName + "/0\" [style=invis];");
//            topics.add(linkedName +"/0");
//
//            outside.add("\"" + linkedName + "\" -> \"" + linkedName + "/1\" [style=invis];");
//            topics.add(linkedName +"/1");


          } else if (type.equals("topic")) {
            outside.add("\"" + entityName + "\" -> \"" + linkedName + "\";");
            topics.add(linkedName);

            //NJB
            outside.add("\"" + linkedName + "\" -> \"" + linkedName + "/0\" [style=invis];");
            topics.add(linkedName +"/0");

            outside.add("\"" + linkedName + "\" -> \"" + linkedName + "/1\" [style=invis];");
            topics.add(linkedName +"/1");

            sames.add("{ rank=same; \"" + linkedName + "\"; \"" + linkedName + "/0\"; \"" + linkedName + "/1\"; }");

          } else if (type.equals("stores")) {
            outside.add("\"" + entityName + "\" -> \"" + linkedName + "\";");
            stores.add(linkedName);

            //NJB
            outside.add("\"" + linkedName + "\" -> \"" + linkedName + "/0\" [style=invis];");
            stores.add(linkedName +"/0");

            outside.add("\"" + linkedName + "\" -> \"" + linkedName + "/1\" [style=invis];");
            stores.add(linkedName +"/1");

            sames.add("{ rank=same; \"" + linkedName + "\"; \"" + linkedName + "/0\"; \"" + linkedName + "/1\"; }");

          }
        }
        continue;
      }

      // Process entity relationships (---> arrows)
      Matcher arrowMatcher = Pattern.compile("-->\\s+(.*)").matcher(line);
      if (arrowMatcher.find() && entityName != null) {
        String targets = arrowMatcher.group(1);
        for (String target : targets.split(",")) {
          String linkedName = processName(target.trim());
          if (!linkedName.equals("none")) {
            results.add("\"" + entityName + "\" -> \"" + linkedName + "\";");
          }
        }
      }
    }

    if (!results.isEmpty()) results.add("}");

    // Add external relationships (outside)
    results.addAll(outside);
    results.addAll(sames);

    // Add stores and topics as special shapes
    for (String store : stores) {

      if (store.endsWith("/0") || store.endsWith("/1")) {
        String num = store.substring(store.lastIndexOf("/") + 1);
        results.add("\"" + store + "\" [shape=egg, label=\"" + num + "\", href=\"/stores/" + store + "\"];");
      } else {
        results.add("\"" + store + "\" [shape=cylinder; label=\"" + store + "\", href=\"/stores/" + store + "\"];");
      }

      String cl = map.get(store);

      //TODO only add this if it is actually created....
      if (cl != null) {
        results.add("\"" + cl + "\" [shape=box, label=\"" + cl + "\", href=\"/events/" + applicationId + "-" + store + "-changelog\"];");

        if (cl.contains("-changelog")) {
          results.add("\"" + store + "\" -> \"" + cl + "\"");
        }
        results.add("\"" + cl + "\" -> \"" + store + "\" [style=dashed,comment=\"restore\"];");
      }
    }

    for (String topic : topics) {

//      //TODO /-repartition/0,1....
//      String t = topic;
//      if (t.endsWith("-repartition")) {
//        t = applicationId + "-" + t;
//      }
      String t = topic;
      if (t.matches(".*-repartition(/\\d+)?$") || t.matches(".*-topic(/\\d+)?$")) {
        t = applicationId + "-" + t;
      }

      if (t.matches(".*(/\\d+)$")) {
        String num = t.substring(t.lastIndexOf("/") + 1);
        results.add("\"" + topic + "\" [shape=egg, label=\"" + num + "\", href=\"/events/" + t + "\"];");
      } else {
        results.add("\"" + topic + "\" [shape=rect, label=\"" + topic + "\", href=\"/events/" + t + "\"];");
      }
    }

    // Combine everything into DOT format
    return "digraph {\n" +
            //"graph [layout=dot, size=\"100%,100%\", ratio=fill];\n" +
            "\tlabel = \"Kafka Streams Topology\"\n\n" +
            String.join("\n", results) +
            "\n}";
  }

  // Helper function to process names (replaces dashes to split on multi-lines in DOT representation)
  private static String processName(String name) {
    return name;
    //return name.replaceAll("-", "-\n");
  }


  //


  private Map<String, Object> properties() {
    Map<String, Object> defaults = Map.ofEntries(
            Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"),
            Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
    );

    Map<String, Object> map = new HashMap<>(defaults);

   // map.putAll(PropertiesUtil.load("/mnt/secrets/connection.properties"));

    return map;
  }

  public static Properties toProperties(final Map<String, Object> map) {
    final Properties properties = new Properties();
    properties.putAll(map);
    return properties;
  }
}