
package io.kineticedge.kstutorial.producer.collector;

import io.kineticedge.kstutorial.domain.OSSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = MapperHelper.class)
public interface SessionMapper {

  SessionMapper INSTANCE = Mappers.getMapper(SessionMapper.class);

  @Mapping(target = "hostname", expression = "java(MapperHelper.hostname())")
  OSSession convert(oshi.software.os.OSSession session);

}