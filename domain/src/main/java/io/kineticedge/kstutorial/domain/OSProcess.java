package io.kineticedge.kstutorial.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "_type")
public record OSProcess(
        int processId,
        String name,
        //String user,
        int parentProcessId,
        int threadCount,
        Instant startTime,
        String upTime,
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
            //null,
            0,
            0,
            null,
            null,
            0,
            0
    );
  }
}