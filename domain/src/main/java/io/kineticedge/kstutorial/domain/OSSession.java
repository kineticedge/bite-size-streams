package io.kineticedge.kstutorial.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "_type")
public record OSSession(
        String hostname,
        String userName,
        String terminalDevice,
        Instant loginTime,
        String host) implements Id {
  @JsonIgnore
  public String id() {
    //return hostname + "-" + userName + "-" + terminalDevice;
    return userName + "-" + terminalDevice;
  }
}
