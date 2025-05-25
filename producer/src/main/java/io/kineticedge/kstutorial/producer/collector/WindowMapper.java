

package io.kineticedge.kstutorial.producer.collector;

import io.kineticedge.kstutorial.domain.OSWindow;
import io.kineticedge.kstutorial.domain.Rectangle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(uses = MapperHelper.class)
public interface WindowMapper {

  WindowMapper INSTANCE = Mappers.getMapper(WindowMapper.class);

  @Mapping(source = "owningProcessId", target = "processId")
  @Mapping(target = "iteration", expression = "java(MapperHelper.iteration())")
  @Mapping(source = "locAndSize", target = "pos", qualifiedByName = "rectangleToPos")
  @Mapping(source = "locAndSize", target = "size", qualifiedByName = "rectangleToSize")
  @Mapping(target = "ts", expression = "java(MapperHelper.ts())")
  OSWindow convert(oshi.software.os.OSDesktopWindow osDesktopWindow);

  Rectangle toRectangle(java.awt.Rectangle rectangle);

  @Named("rectangleToPos")
  default int[] rectangleToPos(Rectangle rectangle) {
    if (rectangle == null) {
      return new int[] { 0, 0};
    }
    return new int[] {rectangle.x(), rectangle.y()};
  }

  @Named("rectangleToSize")
  default int[] rectangleToSize(Rectangle rectangle) {
    if (rectangle == null) {
      return new int[] { 0, 0};
    }
    return new int[] {rectangle.width(), rectangle.height()};
  }

}