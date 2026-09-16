package io.kineticedge.kstutorial.common.streams.metadata;

import java.util.List;

public record ThreadInfo(String name, Long commitTimestamp, String tasks) {}