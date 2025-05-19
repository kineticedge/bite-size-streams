package io.kineticedge.kstutorial.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "_type")
public record OSWindowAggregate(
        int xDelta,
        int yDelta,
        int revision,
        OSWindow window
) {
}
