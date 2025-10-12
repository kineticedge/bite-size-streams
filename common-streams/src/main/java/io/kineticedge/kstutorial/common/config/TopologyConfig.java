package io.kineticedge.kstutorial.common.config;

import org.apache.kafka.streams.kstream.EmitStrategy;
import org.apache.kafka.streams.processor.PunctuationType;

import java.util.Optional;

public record TopologyConfig(
        Optional<Integer> numThreads,
        Optional<Long> commitInterval,
        Optional<String> optimization,
        Optional<Long> taskMaxIdle,
        Optional<Boolean> cachingDisabled,
        Optional<Boolean> disableFeature,
        Optional<Boolean> eosEnabled,
        EmitStrategy.StrategyType emitStrategy,
        PunctuationType punctuationType,
        WindowConfig windowConfig
) {}
