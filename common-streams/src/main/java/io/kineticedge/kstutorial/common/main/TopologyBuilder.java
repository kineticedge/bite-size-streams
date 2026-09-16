package io.kineticedge.kstutorial.common.main;

import io.kineticedge.kstutorial.common.config.TopologyConfig;
import io.kineticedge.kstutorial.common.config.WindowConfig;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.EmitStrategy;
import org.apache.kafka.streams.processor.PunctuationType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TopologyBuilder {

  String applicationId();

  Topology topology();

  Map<String, Object> properties();


  List<String> topics();

  Map<String, String> metadata();

}
