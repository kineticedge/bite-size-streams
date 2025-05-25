package io.kineticedge.kstutorial.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "_type")
public record OSProcess(
        int processId,
        String name,
        String user,
        String state,
        int parentProcessId,
        int threadCount,
        long startTime,
        long upTime,
        int iteration,
        long ts
) implements Id {

  @JsonIgnore
  public String id() {
    return "" + processId;
  }

  public static OSProcess synthetic(int key) {
    return new OSProcess(
            key,
            null,
            null,
            null,
            0,
            0,
            0L,
            0L,
            0,
            0
    );
  }
}