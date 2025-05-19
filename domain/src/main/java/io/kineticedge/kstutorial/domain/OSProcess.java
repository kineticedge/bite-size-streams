package io.kineticedge.kstutorial.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.time.Instant;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "_type")
public record OSProcess(
        String hostname,
        int processId,
        String name,
        String path,
        String currentWorkingDirectory,
        String user,
        String userId,
        String group,
        String groupId,
        String state,
        int parentProcessId,
        int threadCount,
        int priority,
        long virtualSize,
        long residentSetSize,
        long kernelTime,
        long userTime,
        Instant startTime,
        long upTime,
        long bytesRead,
        long bytesWritten,
        long openFiles,
        int bitness,
        long minorFaults,
        long majorFaults,
        long contextSwitches,
        //Map<String, String> environmentVariables, do not map, may have sensitive data
        @JacksonXmlElementWrapper(useWrapping = false)
        //@JacksonXmlProperty(localName = "foo")
        List<String> arguments,
        double processCpuLoadCumulative) implements Id {

  @JsonIgnore
  public String id() {
    //return hostname + ":" + processId;
    return "" + processId;
  }

  public static OSProcess withUppercaseName(OSProcess osProcess) {
    return new OSProcess(
            osProcess.hostname(),
            osProcess.processId(),
            osProcess.name().toUpperCase(),
            osProcess.path(),
            osProcess.currentWorkingDirectory(),
            osProcess.user(),
            osProcess.userId(),
            osProcess.group(),
            osProcess.groupId(),
            osProcess.state(),
            osProcess.parentProcessId(),
            osProcess.threadCount(),
            osProcess.priority(),
            osProcess.virtualSize(),
            osProcess.residentSetSize(),
            osProcess.kernelTime(),
            osProcess.userTime(),
            osProcess.startTime(),
            osProcess.upTime(),
            osProcess.bytesRead(),
            osProcess.bytesWritten(),
            osProcess.openFiles(),
            osProcess.bitness(),
            osProcess.minorFaults(),
            osProcess.majorFaults(),
            osProcess.contextSwitches(),
            osProcess.arguments(),
            osProcess.processCpuLoadCumulative()
    );
  }
  public static OSProcess other(OSProcess osProcess) {
    return new OSProcess(
            osProcess.hostname(),
            osProcess.processId(),
            "OTHER_" + osProcess.name().toUpperCase(),
            osProcess.path(),
            osProcess.currentWorkingDirectory(),
            osProcess.user(),
            osProcess.userId(),
            osProcess.group(),
            osProcess.groupId(),
            osProcess.state(),
            osProcess.parentProcessId(),
            osProcess.threadCount(),
            osProcess.priority(),
            osProcess.virtualSize(),
            osProcess.residentSetSize(),
            osProcess.kernelTime(),
            osProcess.userTime(),
            osProcess.startTime(),
            osProcess.upTime(),
            osProcess.bytesRead(),
            osProcess.bytesWritten(),
            osProcess.openFiles(),
            osProcess.bitness(),
            osProcess.minorFaults(),
            osProcess.majorFaults(),
            osProcess.contextSwitches(),
            osProcess.arguments(),
            osProcess.processCpuLoadCumulative()
    );
  }

  public static OSProcess synthetic(int key) {
    return new OSProcess(
            "SYNTHETIC",
            key,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            0,
            0,
            0L,
            0L,
            0L,
            0L,
            null,
            0L,
            0L,
            0L,
            0L,
            0,
            0L,
            0L,
            0L,
            null,
            0.0
    );

  }

}