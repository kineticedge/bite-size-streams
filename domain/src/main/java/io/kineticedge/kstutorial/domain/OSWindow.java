package io.kineticedge.kstutorial.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "_type")
public record OSWindow(
        //String hostname,
        long windowId,
        long processId,
        String title,
//        String command,
        Rectangle locAndSize
        //int order,
        //boolean visible
) implements Id {
  @JsonIgnore
  public String id() {
    //return hostname + "_" + windowId;
    return "" + windowId;
  }

}
