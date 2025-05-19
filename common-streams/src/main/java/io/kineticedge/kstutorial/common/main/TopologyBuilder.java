package io.kineticedge.kstutorial.common.main;

import io.kineticedge.kstutorial.common.config.TopologyConfig;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TopologyBuilder {

  String applicationId();

  Topology topology();

  Map<String, Object> properties();


  List<String> topics();

}
