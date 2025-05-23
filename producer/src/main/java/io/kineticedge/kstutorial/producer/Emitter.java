package io.kineticedge.kstutorial.producer;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.domain.Id;
import io.kineticedge.kstutorial.producer.collector.ProcessMapper;
import io.kineticedge.kstutorial.producer.collector.ServiceMapper;
import io.kineticedge.kstutorial.producer.collector.ThreadMapper;
import io.kineticedge.kstutorial.producer.collector.WindowMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

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

  private Boolean windowsOnly = false;

  public Emitter(Options options) {
    producer = new Producer(options);
  }

  public Emitter(final String bootstrapServers) {
    producer = new Producer(bootstrapServers);
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

  public void emit(int parent, int age, Boolean windowsOnly) {
    this.parent = parent;
    this.age = age;
    this.windowsOnly = windowsOnly;
    emit();
  }

  public void emit() {
    log.info("emitting processes for parent={}", parent);

    long start = System.currentTimeMillis();


    final Set<Long> proccessIds = Stream.concat(
                    (parent != 1 ? Optional.of(os.getProcess(parent)) : Optional.<OSProcess>empty()).stream(),
                    os.getDescendantProcesses(parent, null, null, 0).stream()
            )
            .map(ProcessMapper.INSTANCE::convert)
            .peek(p -> {
              if (!windowsOnly()) {
                producer.publish(Constants.PROCESSES, p, (System.currentTimeMillis() - (age * 1000L)));
              }
            })
            .map(p -> (long) p.processId())
            .collect(Collectors.toSet());

    log.debug("process Ids: {}", proccessIds);

    os.getDesktopWindows(true).stream()
            .map(WindowMapper.INSTANCE::convert)
            .filter(w -> !ignoredWindows.contains(w.title())) //TODO was command now title, need to verify ok/good
            .filter(w -> proccessIds.contains(w.processId()))
            .forEach(w -> producer.publish(Constants.WINDOWS, w, (System.currentTimeMillis() - (age * 1000L))));

    if (!windowsOnly) {
      os.getServices().stream()
              .map(ServiceMapper.INSTANCE::convert)
              .filter(s -> proccessIds.contains((long) s.processId()))
              .forEach(p -> producer.publish(Constants.SERVICES, p, (System.currentTimeMillis() - (age * 1000L))));
    }
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

  private boolean windowsOnly() {
    return windowsOnly != null && windowsOnly;
  }

}