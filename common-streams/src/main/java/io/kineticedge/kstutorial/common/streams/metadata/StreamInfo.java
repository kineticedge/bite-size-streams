package io.kineticedge.kstutorial.common.streams.metadata;

import java.util.List;

public record StreamInfo(
        String applicationId,
        List<ThreadInfo> thread,
        List<SubtopologyInfo> subtopology
) {}
