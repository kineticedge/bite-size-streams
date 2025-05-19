package io.kineticedge.kstutorial.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "_type")
public record OSService(
        String hostname,
        String name,
        int processId,
        String state) implements Id {
  @JsonIgnore
  public String id() {
    //return hostname + ":" + processId;
    return "" + processId;
  }
}

