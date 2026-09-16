package io.kineticedge.kstutorial.common.streams.metadata;

import java.util.List;
import java.util.Map;

public record StreamInfo(
        String applicationId,
        List<ThreadInfo> thread,
        List<SubtopologyInfo> subtopology,
        Map<String, String> metadata,
        Map<String, String> producerMetadata
) {}
