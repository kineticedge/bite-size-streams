package io.kineticedge.kstutorial.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "_type")
public class OSWindowMoves {

  public static AtomicInteger counter = new AtomicInteger(0);

  record Position(int x, int y) {
  }

  private String id;
  private int lastX = Integer.MIN_VALUE;
  private int lastY = Integer.MIN_VALUE;
  private int count = 0;
  private List<String> positions = new ArrayList<>();

  public OSWindowMoves() {
    this.id = "id:" + counter.incrementAndGet();
  }


  public String getId() {
    return id;
  }

  public int getLastX() {
    return lastX;
  }

  public void setLastX(int lastX) {
    this.lastX = lastX;
  }

  public int getLastY() {
    return lastY;
  }

  public void setLastY(int lastY) {
    this.lastY = lastY;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public List<String> getPositions() {
    return positions;
  }

  public void setPositions(List<String> positions) {
    this.positions = positions;
  }

  @JsonIgnore
  public void moved(OSWindow window) {


    if (lastX == Integer.MIN_VALUE || lastY == Integer.MIN_VALUE) {
      lastX = window.pos()[0];
      lastY = window.pos()[1];
      positions.add("(" + lastX + "," + lastY + ")");
      count = 0;
      return;
    }

    if (lastX != window.pos()[0] || lastY != window.pos()[1]) {
      lastX = window.pos()[0];
      lastY = window.pos()[1];
      positions.add("(" + lastX + "," + lastY + ")");
      count++;
    }


  }
}


