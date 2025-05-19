package io.kineticedge.kstutorial.producer;

import io.kineticedge.kstutorial.common.Constants;
import io.kineticedge.kstutorial.common.config.OptionsUtil;
import io.kineticedge.kstutorial.domain.OSProcess;
import io.kineticedge.kstutorial.producer.collector.ProcessMapper;
import io.kineticedge.kstutorial.producer.collector.ServiceMapper;
import io.kineticedge.kstutorial.producer.collector.SessionMapper;
import io.kineticedge.kstutorial.producer.collector.WindowMapper;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.software.os.OperatingSystem;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);

  private static final SecureRandom random = new SecureRandom();

  private static final Thread.UncaughtExceptionHandler exceptionHandler = (t, e) -> log.error("Uncaught exception in thread '{}': {}", t.getName(), e.getMessage());

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(6, r -> {
    final Thread t = Executors.defaultThreadFactory().newThread(r);
    t.setUncaughtExceptionHandler(exceptionHandler);
    return t;
  });

  private final OperatingSystem os = new SystemInfo().getOperatingSystem();

  public static void main(String[] args) {

    Main main = new Main();

    main.exec(args);
  }

  private void exec(String[] args) {

    final Options options = OptionsUtil.parse(Options.class, args);

    if (options == null) {
      return;
    }

    try (AdminClient client = AdminClient.create(Map.ofEntries(
            Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, options.bootstrapServers()),
            Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
    ))) {
      final CreateTopicsResult result = client.createTopics(
              List.of(new NewTopic("processes", 4, (short) -1),
                      new NewTopic("windows", 4, (short) -1),
                      new NewTopic("threads", 4, (short) -1),
                      new NewTopic("services", 4, (short) -1),
                      new NewTopic("sessions", 4, (short) -1)
              )
      );
      try {
        result.all().get();
      } catch (InterruptedException | ExecutionException e) {
        Thread.currentThread().interrupt();
        // ignore, assuming they already exists
      }
    }


    final Producer producer = new Producer(options);

    // windows that seem to cause a lot of noise...
    final Set<String> ignoredWindows = Set.of("Window Server", "Control Center");


    scheduler.scheduleAtFixedRate(() -> {

      //AtomicLong ts = new AtomicLong(System.currentTimeMillis());

      os.getProcesses().stream()
              .map(ProcessMapper.INSTANCE::convert)
              // ONLY THESE TWO for now
//              .filter(p -> Set.of(13615).contains(p.processId()))
              .flatMap(p -> {
                return Stream.of(p, OSProcess.withUppercaseName(p), OSProcess.other(p));
              })
              .forEach(p -> {

//                long ts = System.currentTimeMillis();
//                if (p.name().equals("tmux")) {
//                  ts = ts - 3000L;
//                } else if (p.name().equals("TMUX")) {
//                  ts = ts + 3000L;
//                }

                producer.publish(Constants.PROCESSES, p);
              });
    }, random.nextLong(options.getWindowFrequency()), options.getProcessesFrequency(), java.util.concurrent.TimeUnit.MILLISECONDS);

    scheduler.scheduleAtFixedRate(() -> {
      os.getDesktopWindows(false).stream()
              .map(WindowMapper.INSTANCE::convert)
              .filter(w -> !ignoredWindows.contains(w.command()))
              .forEach(p -> producer.publish(Constants.WINDOWS, p));
    }, random.nextLong(options.getWindowFrequency()), options.getWindowFrequency(), java.util.concurrent.TimeUnit.MILLISECONDS);

    scheduler.scheduleAtFixedRate(() -> {
      os.getServices().stream()
              .map(ServiceMapper.INSTANCE::convert)
              .forEach(p -> producer.publish(Constants.SERVICES, p));
    }, random.nextLong(options.getServicesFrequency()), options.getServicesFrequency(), java.util.concurrent.TimeUnit.MILLISECONDS);

    scheduler.scheduleAtFixedRate(() -> {
      os.getSessions().stream()
              .map(SessionMapper.INSTANCE::convert)
              .forEach(s -> producer.publish(Constants.SESSIONS, s));
    }, random.nextLong(options.getSessionsFrequency()), options.getSessionsFrequency(), java.util.concurrent.TimeUnit.MILLISECONDS);

    // slow, so deciding if we use it.
//    scheduler.scheduleAtFixedRate(() -> {
//      os.getProcesses().stream()
//              .flatMap(p -> p.getThreadDetails().stream())
//              .map(ThreadMapper.INSTANCE::convert)
//              .forEach(s -> producer.publish(options.getThreadsTopic(), s));
//    }, random.nextLong(options.getSessionsFrequency()), options.getThreadsFrequency(), java.util.concurrent.TimeUnit.MILLISECONDS);


    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutting down...");
      scheduler.shutdown();
      producer.flush();
      try {
        boolean terminated = scheduler.awaitTermination(10_000L, TimeUnit.MILLISECONDS);
        if (!terminated) {
          log.warn("Timed out waiting for shutdown");
        }
      } catch (InterruptedException e) {
        log.warn("Interrupted while waiting for shutdown", e);
        Thread.currentThread().interrupt();
      }
      producer.close();
    }));
  }

}

