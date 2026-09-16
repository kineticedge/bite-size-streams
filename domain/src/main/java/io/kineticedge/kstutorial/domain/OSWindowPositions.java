package io.kineticedge.kstutorial.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "_type")
public record OSWindowPositions(
        List<int[]> positions
) {

  public OSWindowPositions next(int[] position) {

    final List<int[]> newPositions = new ArrayList<>(this.positions());

    final int[] lastPosition = !newPositions.isEmpty() ? newPositions.getLast() : null;
    if (lastPosition == null || (lastPosition[0] != position[0] || lastPosition[1] != position[1])) {
      newPositions.add(position);
    }

    return new OSWindowPositions(Collections.unmodifiableList(newPositions));
  }

  public OSWindowPositions merge(OSWindowPositions other) {
    ArrayList<int[]> newPositions = new ArrayList<>(this.positions());
    newPositions.addAll(other.positions());
    return new OSWindowPositions(Collections.unmodifiableList(newPositions));
  }

  public static OSWindowPositions create() {
    return new OSWindowPositions(List.of());
  }


}
