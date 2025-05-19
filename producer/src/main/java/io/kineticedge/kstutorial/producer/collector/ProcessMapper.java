package io.kineticedge.kstutorial.producer.collector;

import io.kineticedge.kstutorial.domain.OSProcess;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(uses = MapperHelper.class)
public interface ProcessMapper {

  ProcessMapper INSTANCE = Mappers.getMapper(ProcessMapper.class);

  @Mapping(target = "hostname", expression = "java(MapperHelper.hostname())")
  @Mapping(source = "processID", target = "processId")
  @Mapping(source = "parentProcessID", target = "parentProcessId")
  @Mapping(source = "userID", target = "userId")
  @Mapping(source = "groupID", target = "groupId")
  @Mapping(source = "state", target = "state", qualifiedByName = "stateToString")
  OSProcess convert(oshi.software.os.OSProcess osProcess);

  default <T> List<T> toImmutableList(List<T> input) {
    return List.copyOf(input);
  }

}