package io.kineticedge.kstutorial.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "_type")
public record OSWindow(
        long windowId,
        long processId,
        String title,
        int[] pos,
        int[] size,
        int iteration,
        long ts
) implements Id {
  @JsonIgnore
  public String id() {
    return "" + windowId;
  }

  public int x() {
    return pos[0];
  }

  public int y() {
    return pos[1];
  }

  public int width() {
    return size[0];
  }


  public int height() {
    return size[1];
  }

}
