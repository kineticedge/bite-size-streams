package io.kineticedge.kstutorial.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "_type")
public record OSThread(
        String hostname,
        int threadId,
        String state,
        long kernelTime,
        long userTime,
        Instant startTime,
        long upTime,
        int priority,
        int owningProcessId) implements Id {

  @JsonIgnore
  public String id() {
    //return hostname + ":" + owningProcessId + ":" + threadId;
    return owningProcessId + ":" + threadId;
  }

}
