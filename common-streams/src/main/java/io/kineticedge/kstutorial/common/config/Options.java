package io.kineticedge.kstutorial.common.config;

import com.beust.jcommander.Parameter;
import io.kineticedge.kstutorial.common.main.BaseTopologyBuilder;
import io.kineticedge.kstutorial.common.main.TopologyBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.EmitStrategy;
import org.apache.kafka.streams.processor.PunctuationType;

import java.time.Duration;
import java.util.Optional;

public class Options extends BaseOptions {

  @Parameter(names = "--topology", required = true, converter = TopologyConverter.class)
  private TopologyBuilder topologyBuilder;

  @Parameter(names = "--num-threads")
  private Integer numThreads = 1;

  @Parameter(names = "--commit-interval")
  private Long commitInterval = 5_000L;

  @Parameter(names = "--linger")
  private Long lingerMs = 50L;

  @Parameter(names = "--optimization")
  private String optimization = StreamsConfig.NO_OPTIMIZATION;

  @Parameter(names = "--emit-strategy")
  private EmitStrategy.StrategyType emitStrategy;

  @Parameter(names = "--caching-disabled")
  private Boolean cachingDisabled = Boolean.FALSE;

  @Parameter(names = "--punctuation-type")
  private PunctuationType punctuationType = PunctuationType.WALL_CLOCK_TIME;

  @Parameter(names = "--window-size", converter = DurationConverter.class)
  private Duration windowSize;

  @Parameter(names = "--window-grace", converter = DurationConverter.class)
  private Duration windowGrace;

  @Parameter(names = "--window-advance", converter = DurationConverter.class)
  private Duration windowAdvance;

  @Parameter(names = "--retention", converter = DurationConverter.class)
  private Duration retention;

  @Parameter(names = "--disable-feature")
  private Boolean disableFeature = Boolean.FALSE;

  @Parameter(names = "--enable-eos")
  private Boolean eosEnabled = Boolean.FALSE;


  public TopologyBuilder topologyBuilder() {
    if (topologyBuilder instanceof BaseTopologyBuilder baseTopologyBuilder) {
      baseTopologyBuilder.setConfig(this.topologyConfig());
    }
    return topologyBuilder;
  }

  private TopologyConfig topologyConfig() {
    return new TopologyConfig(
            Optional.ofNullable(numThreads),
            Optional.ofNullable(commitInterval),
            Optional.ofNullable(optimization),
            Optional.ofNullable(cachingDisabled),
            Optional.ofNullable(disableFeature),
            Optional.ofNullable(eosEnabled),
            emitStrategy,
            punctuationType,
            new WindowConfig(
                    Optional.ofNullable(windowSize),
                    Optional.ofNullable(windowGrace),
                    Optional.ofNullable(windowAdvance),
                    Optional.ofNullable(retention)
            )
    );
  }


  public Long lingerMs() {
    return lingerMs;
  }
}
