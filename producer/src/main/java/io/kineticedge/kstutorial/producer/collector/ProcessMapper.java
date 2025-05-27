package io.kineticedge.kstutorial.producer.collector;

import io.kineticedge.kstutorial.domain.OSProcess;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Collections;
import java.util.List;

@Mapper(uses = MapperHelper.class)
public interface ProcessMapper {

  ProcessMapper INSTANCE = Mappers.getMapper(ProcessMapper.class);

  @Mapping(source = "processID", target = "processId")
  @Mapping(source = "parentProcessID", target = "parentProcessId")
//  @Mapping(source = "state", target = "state", qualifiedByName = "stateToString")
  @Mapping(target = "iteration", expression = "java(MapperHelper.iteration())")
  @Mapping(target = "ts", expression = "java(MapperHelper.ts())")
  OSProcess convert(oshi.software.os.OSProcess osProcess);

}