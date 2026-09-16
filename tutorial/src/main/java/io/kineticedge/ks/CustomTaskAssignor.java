package io.kineticedge.ks;

import org.apache.kafka.clients.consumer.ConsumerPartitionAssignor;
import org.apache.kafka.streams.processor.TaskId;
import org.apache.kafka.streams.processor.assignment.ApplicationState;
import org.apache.kafka.streams.processor.assignment.KafkaStreamsAssignment;
import org.apache.kafka.streams.processor.assignment.KafkaStreamsState;
import org.apache.kafka.streams.processor.assignment.ProcessId;
import org.apache.kafka.streams.processor.assignment.TaskAssignor;
import org.apache.kafka.streams.processor.assignment.assignors.StickyTaskAssignor;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public class CustomTaskAssignor implements TaskAssignor {

  private final StickyTaskAssignor delegate = new StickyTaskAssignor();

  @Override
  public void onAssignmentComputed(ConsumerPartitionAssignor.GroupAssignment assignment, ConsumerPartitionAssignor.GroupSubscription subscription, AssignmentError error) {
    delegate.onAssignmentComputed(assignment, subscription, error);
  }

  @Override
  public void configure(Map<String, ?> configs) {
    delegate.configure(configs);
  }

  @Override
  public TaskAssignment assign(ApplicationState applicationState) {

    TaskAssignment assignment = delegate.assign(applicationState);


    return assignment;
//    final Map<ProcessId, KafkaStreamsState> clients = applicationState.kafkaStreamsStates(false);
//    final Map<TaskId, ProcessId> previousActiveAssignment = mapPreviousActiveTasks(clients);
//    final Map<TaskId, Set<ProcessId>> previousStandbyAssignment = mapPreviousStandbyTasks(clients);
//    final StickyTaskAssignor.AssignmentState assignmentState = new StickyTaskAssignor.AssignmentState(applicationState, clients,
//            previousActiveAssignment, previousStandbyAssignment);
//
//    assignActive(applicationState, clients.values(), assignmentState, this.mustPreserveActiveTaskAssignment);
//    optimizeActive(applicationState, assignmentState);
//    assignStandby(applicationState, assignmentState);
//    optimizeStandby(applicationState, assignmentState);
//
//    final Map<ProcessId, KafkaStreamsAssignment> finalAssignments = assignmentState.newAssignments;
//    if (mustPreserveActiveTaskAssignment && !finalAssignments.isEmpty()) {
//      // We set the followup deadline for only one of the clients.
//      final ProcessId clientId = finalAssignments.entrySet().iterator().next().getKey();
//      final KafkaStreamsAssignment previousAssignment = finalAssignments.get(clientId);
//      finalAssignments.put(clientId, previousAssignment.withFollowupRebalance(Instant.ofEpochMilli(0)));
//    }
//
//    return new TaskAssignment(finalAssignments.values());
  }


  //  @Override
//  public Map<UUID, ClientState> assign(
//          final Map<UUID, ClientState> clients,
//          final Set<TaskId> allTaskIds,
//          final Set<TaskId> statefulTaskIds,
//          final Map<TaskId, UUID> previousActiveTaskAssignments,
//          final Map<TaskId, Set<UUID>> previousStandbyTaskAssignments,
//          final Map<UUID, InternalTopologyBuilder.TopologyMetadata> topologyMetadata
//  ) throws TaskAssignmentException {
//
//    // Create a copy of client states that we can modify
//    final Map<UUID, ClientState> resultAssignment = new HashMap<>();
//    for (final Map.Entry<UUID, ClientState> entry : clients.entrySet()) {
//      resultAssignment.put(entry.getKey(), entry.getValue().copy());
//    }
//
//    // Find all client IDs
//    UUID[] clientIds = resultAssignment.keySet().toArray(new UUID[0]);
//
//    // If we don't have enough clients for our desired assignment, return empty
//    if (clientIds.length < 3) {
//      return resultAssignment;
//    }
//
//    // Create our custom assignment
//    for (TaskId taskId : allTaskIds) {
//      if (taskId.subtopology() == 0 && taskId.partition() == 0) {
//        // Task 0_0 goes to Thread 1
//        assignTaskToClient(taskId, clientIds[0], resultAssignment);
//      } else if (taskId.subtopology() == 0 && taskId.partition() == 1) {
//        // Task 0_1 goes to Thread 2
//        assignTaskToClient(taskId, clientIds[1], resultAssignment);
//      } else if (taskId.subtopology() == 1) {
//        // All tasks from subtopology 1 go to Thread 3
//        assignTaskToClient(taskId, clientIds[2], resultAssignment);
//      }
//    }
//
//    return resultAssignment;
//  }
//
//  private void assignTaskToClient(
//          final TaskId taskId,
//          final UUID clientId,
//          final Map<UUID, ClientState> resultAssignment
//  ) {
//    final ClientState clientState = resultAssignment.get(clientId);
//    clientState.assignActive(taskId);
//  }
}