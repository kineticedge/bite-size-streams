
package io.kineticedge.kstutorial.producer.collector;

import io.kineticedge.kstutorial.domain.OSThread;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = MapperHelper.class)
public interface ThreadMapper {

  ThreadMapper INSTANCE = Mappers.getMapper(ThreadMapper.class);

  @Mapping(target = "hostname", expression = "java(MapperHelper.hostname())")
  @Mapping(source = "state", target = "state", qualifiedByName = "stateToString")
  OSThread convert(oshi.software.os.OSThread osThread);

}