package io.kineticedge.kstutorial.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.time.Instant;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "_type")
public record OSProcessCount(
        int processId,
        int count
) implements Id {

  @JsonIgnore
  public String id() {
    return "" + processId;
  }


  public OSProcessCount increment() {
    return new OSProcessCount(processId, count + 1);
  }

  public OSProcessCount increment(OSProcessCount other) {
    return new OSProcessCount(processId, count + other.count);
  }

  public static OSProcessCount create(int processId) {
    return new OSProcessCount(processId, 1);
  }

}