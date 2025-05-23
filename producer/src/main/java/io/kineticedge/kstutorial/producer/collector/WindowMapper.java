

package io.kineticedge.kstutorial.producer.collector;

import io.kineticedge.kstutorial.domain.OSWindow;
import io.kineticedge.kstutorial.domain.Rectangle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = MapperHelper.class)
public interface WindowMapper {

  WindowMapper INSTANCE = Mappers.getMapper(WindowMapper.class);

//  @Mapping(target = "hostname", expression = "java(MapperHelper.hostname())")
  @Mapping(source = "owningProcessId", target = "processId")
  OSWindow convert(oshi.software.os.OSDesktopWindow osDesktopWindow);

  Rectangle toRectangle(java.awt.Rectangle rectangle);

}