package io.kineticedge.kstutorial.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "_type")
public class OSWindowList implements Id {

  public String batchId;

  public List<OSWindow> windows = new ArrayList<>();

  public String getBatchId() {
    return batchId;
  }

  public void setBatchId(String batchId) {
    this.batchId = batchId;
  }

  public List<OSWindow> getWindows() {
    return windows;
  }

  public void setWindows(List<OSWindow> windows) {
    this.windows = windows;
  }

  @JsonIgnore
  public void addWindow(OSWindow window) {
    windows.add(window);
  }

  @JsonIgnore
  public String id() {
    //return hostname + "_" + windowId;
    return batchId;
  }

  @JsonIgnore
  public List<Long> windowIds() {
    return windows.stream().map(OSWindow::windowId).toList();
  }
}
