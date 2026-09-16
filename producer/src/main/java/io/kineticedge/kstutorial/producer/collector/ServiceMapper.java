
package io.kineticedge.kstutorial.producer.collector;

import io.kineticedge.kstutorial.domain.OSService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ServiceMapper {

  ServiceMapper INSTANCE = Mappers.getMapper(ServiceMapper.class);

  @Mapping(target = "hostname", expression = "java(MapperHelper.hostname())")
  @Mapping(source = "processID", target = "processId")
  OSService convert(oshi.software.os.OSService osThread);

}