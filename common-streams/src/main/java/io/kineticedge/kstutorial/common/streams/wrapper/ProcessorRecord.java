package io.kineticedge.kstutorial.common.streams.wrapper;

public record ProcessorRecord(
        String thread,
        String topic,
        int partition,
        String node,
        String type,
        String key,
        String value,
        String output,
        long nano
) {}
