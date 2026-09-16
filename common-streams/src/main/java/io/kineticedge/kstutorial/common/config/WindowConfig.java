package io.kineticedge.kstutorial.common.config;

import java.time.Duration;
import java.util.Optional;

public record WindowConfig(
        Optional<Duration> size,
        Optional<Duration> grace,
        Optional<Duration> advance,
        Optional<Duration> retention
) {}
