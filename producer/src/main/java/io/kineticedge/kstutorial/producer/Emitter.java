package io.kineticedge.kstutorial.producer;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.domain.Id;
import io.kineticedge.kstutorial.producer.collector.MapperHelper;
import io.kineticedge.kstutorial.producer.collector.ProcessMapper;
import io.kineticedge.kstutorial.producer.collector.WindowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Emitter {

  private static final Logger log = LoggerFactory.getLogger(Emitter.class);

  private final Set<String> ignoredWindows = Set.of("Window Server", "Control Center");

  private final Producer producer;

  private final OperatingSystem os = new SystemInfo().getOperatingSystem();

  private int parent = 1;

  // how many seconds "old" the message is made.
  private int age = 0;

  //private Boolean windowsOnly = false;
  private String types;

  public Emitter(Options options) {
    producer = new Producer(options);
  }

  public Emitter(final String bootstrapServers, final Map<String, String> producerMetadata) {
    producer = new Producer(bootstrapServers, producerMetadata);
  }

  public void poisonPill() {
    try {
      producer.publish(Constants.PROCESSES, new Id() {

        private String foo;

        public String getFoo() {
          return foo;
        }

        public void setFoo(String foo) {
          this.foo = foo;
        }

        @Override
        public String id() {
          return null;
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void emit(int parent, int age, String types) {
    this.parent = parent;
    this.age = age;
    //this.windowsOnly = windowsOnly;
    this.types = types;
    emit();
  }

  public void emit() {
    log.info("emitting processes for parent={}", parent);

    long start = System.currentTimeMillis();

    // track IDs of emission to keep track of what events get joined.
    MapperHelper.incrementIteration();

    final Set<io.kineticedge.kstutorial.domain.OSProcess> proccesses = Stream.concat(
                    (parent != 1 ? Optional.of(os.getProcess(parent)) : Optional.<OSProcess>empty()).stream(),
                    os.getDescendantProcesses(parent, null, null, 0).stream()
            )
            .map(ProcessMapper.INSTANCE::convert)
//            .peek(p -> {
//              if (includeProcesses()) {
//                producer.publish(Constants.PROCESSES, p, messageTimestamp(Constants.PROCESSES));
//              }
//            })
//            .map(p -> (long) p.processId())
            .collect(Collectors.toSet());

    final Set<Long> proccessIds = proccesses.stream().map(p -> (long) p.processId()).collect(Collectors.toSet());

    log.debug("process Ids: {}", proccessIds);

    if (includeProcesses()) {
      proccesses.forEach(p -> producer.publish(Constants.PROCESSES, p, messageTimestamp(Constants.PROCESSES)));
    }

    if (includeWindows()) {
      os.getDesktopWindows(true).stream()
              .map(WindowMapper.INSTANCE::convert)
              .filter(w -> !ignoredWindows.contains(w.title())) //TODO was command now title, need to verify ok/good
              .filter(w -> proccessIds.contains(w.processId()))
              .forEach(w -> producer.publish(Constants.WINDOWS, w, messageTimestamp(Constants.WINDOWS)));
    }

    if (includeProcessesDelayed()) {
      producer.flush();
//      try {
//        Thread.sleep(50L);
//      } catch (InterruptedException e) {
//        throw new RuntimeException(e);
//      }
      proccesses.forEach(p -> producer.publish(Constants.PROCESSES, p, messageTimestamp(Constants.PROCESSES)));
    }


//    if (!windowsOnly) {
//      os.getServices().stream()
//              .map(ServiceMapper.INSTANCE::convert)
//              .filter(s -> proccessIds.contains((long) s.processId()))
//              .forEach(p -> producer.publish(Constants.SERVICES, p, (System.currentTimeMillis() - (age * 1000L))));
//    }

//    if (parent != 1) {
//      final long threadStartTime = System.currentTimeMillis();
//      os.getProcesses().stream()
//              .filter(p -> proccessIds.contains((long) p.getProcessID()))
//              .peek(p -> log.info("threads for process: {}", p))
//              .flatMap(p -> p.getThreadDetails().stream())
//              .map(ThreadMapper.INSTANCE::convert)
//              .forEach(s -> producer.publish(Constants.THREADS, s, (System.currentTimeMillis() - (age * 1000L))));
//      log.info("thread events - duration {}ms", System.currentTimeMillis() - threadStartTime);
//    } else {
//      log.info("no threads when parent is 1");
//    }

    log.info("all events - duration {}ms", System.currentTimeMillis() - start);
  }

//  private boolean windowsOnly() {
//    return windowsOnly != null && windowsOnly;
//  }

  private long messageTimestamp(final String topic) {
//    if ("processes-delayed".equals(types) && Constants.PROCESSES.equals(topic)) {
//      // process gets current time, all other topics are aged accordingly.
//      return System.currentTimeMillis();
//    }
    return System.currentTimeMillis() - (age * 1000L);
  }

  private boolean includeProcesses() {
    return types == null || "all".equals(types) || "processes".equals(types);
  }

  private boolean includeWindows() {
    return types == null || "all".equals(types) || "windows".equals(types) || "processes-delayed".equals(types);
  }

  private boolean includeProcessesDelayed() {
    return "processes-delayed".equals(types);
  }

}