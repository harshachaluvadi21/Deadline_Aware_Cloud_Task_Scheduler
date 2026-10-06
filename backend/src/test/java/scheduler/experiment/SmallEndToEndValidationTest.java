package scheduler.experiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import scheduler.baseline.BaselinePriorityScheduler;
import scheduler.baseline.VmAssignment;
import scheduler.cloudsim.CloudSimEnvironment;
import scheduler.model.CloudVmSpec;
import scheduler.model.Task;
import scheduler.proposed.ProposedPriorityScheduler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Section 14: Small End-To-End Validation Experiment")
public class SmallEndToEndValidationTest {

    @Test
    @DisplayName("Run 4 tasks on 2 VMs and verify Scheduler VM == CloudSim VM and timing consistency")
    void testSmallValidationExperiment() {
        List<CloudVmSpec> vms = List.of(
                CloudVmSpec.of(0, 1000.0, 2048, 1000, 10000),
                CloudVmSpec.of(1, 1000.0, 2048, 1000, 10000)
        );

        List<Task> workload = List.of(
                new Task(1L, 8, 0.0, 3.0, 8.0),
                new Task(2L, 4, 1.0, 4.0, 12.0),
                new Task(3L, 9, 2.0, 2.0, 6.0),
                new Task(4L, 6, 2.5, 5.0, 10.0)
        );

        double refMips = 1000.0;

        System.out.println("\n=== SMALL VALIDATION EXPERIMENT: BASELINE (IEEE PAT) ===");
        System.out.println("Task | Scheduler VM | CloudSim VM | Start | Finish | Deadline | Miss?");
        System.out.println("------------------------------------------------------------------");

        // Run Baseline
        List<Task> baseTasks = WorkloadGenerator.cloneWorkload(workload);
        BaselinePriorityScheduler baseSched = new BaselinePriorityScheduler(vms, refMips);
        baseSched.schedule(baseTasks);
        Map<Long, Long> baseSchedVmMap = baseSched.getAssignments().stream()
                .collect(Collectors.toMap(VmAssignment::taskId, VmAssignment::vmId));

        CloudSimEnvironment baseEnv = new CloudSimEnvironment(vms);
        CloudSimEnvironment.CloudSimExecutionResult baseExec =
                baseEnv.execute(baseTasks, baseSched.getAssignments(), refMips);

        for (TaskExecutionRecord rec : baseExec.records()) {
            long schedVm = baseSchedVmMap.get(rec.taskId());
            long cloudSimVm = rec.assignedVmId();
            assertEquals(schedVm, cloudSimVm, "Scheduler VM must equal CloudSim VM for task " + rec.taskId());
            assertTrue(rec.startTime() >= rec.arrivalTime());
            assertTrue(rec.completionTime() > rec.startTime());

            System.out.printf("T%-3d | VM %-10d | VM %-8d | %-5.2f | %-6.2f | %-8.2f | %-5b%n",
                    rec.taskId(), schedVm, cloudSimVm, rec.startTime(), rec.completionTime(), rec.deadline(), rec.deadlineMissed());
        }

        System.out.println("\n=== SMALL VALIDATION EXPERIMENT: PROPOSED (DEADLINE-AWARE) ===");
        System.out.println("Task | Scheduler VM | CloudSim VM | Start | Finish | Deadline | Miss?");
        System.out.println("------------------------------------------------------------------");

        // Run Proposed
        List<Task> propTasks = WorkloadGenerator.cloneWorkload(workload);
        ProposedPriorityScheduler propSched = new ProposedPriorityScheduler(vms, refMips);
        propSched.schedule(propTasks);
        Map<Long, Long> propSchedVmMap = propSched.getAssignments().stream()
                .collect(Collectors.toMap(VmAssignment::taskId, VmAssignment::vmId));

        CloudSimEnvironment propEnv = new CloudSimEnvironment(vms);
        CloudSimEnvironment.CloudSimExecutionResult propExec =
                propEnv.execute(propTasks, propSched.getAssignments(), refMips);

        for (TaskExecutionRecord rec : propExec.records()) {
            long schedVm = propSchedVmMap.get(rec.taskId());
            long cloudSimVm = rec.assignedVmId();
            assertEquals(schedVm, cloudSimVm, "Scheduler VM must equal CloudSim VM for task " + rec.taskId());
            assertTrue(rec.startTime() >= rec.arrivalTime());
            assertTrue(rec.completionTime() > rec.startTime());

            System.out.printf("T%-3d | VM %-10d | VM %-8d | %-5.2f | %-6.2f | %-8.2f | %-5b%n",
                    rec.taskId(), schedVm, cloudSimVm, rec.startTime(), rec.completionTime(), rec.deadline(), rec.deadlineMissed());
        }
    }
}
