package io.kineticedge.kstutorial.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "_type")
public class OSWindowMap implements Id {

  public TreeMap<Long, OSWindow> windows = new TreeMap<>();

  @JsonIgnore
  public void addWindow(OSWindow window) {
    windows.put(window.windowId(), window);
  }

  @JsonIgnore
  public String id() {
    return "n/a";
  }

  @JsonIgnore
  public List<Long> windowIds() {
    return windows.values().stream().map(OSWindow::windowId).toList();
  }
}
