package scheduler.cloudsim;

import org.cloudsimplus.cloudlets.CloudletSimple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import scheduler.experiment.MetricsCalculator;
import scheduler.experiment.TaskExecutionRecord;
import scheduler.experiment.WorkloadGenerator;
import scheduler.model.CloudVmSpec;
import scheduler.model.Task;
import scheduler.proposed.ProposedPriorityScheduler;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CloudSim Plus 8.0.0 End-To-End Execution Pipeline Test")
public class CloudSimEndToEndTest {

    private static final double REF_MIPS = 1000.0;

    private List<CloudVmSpec> createTestVms() {
        return List.of(
                CloudVmSpec.of(0, 1000.0, 2048, 1000, 10000),
                CloudVmSpec.of(1, 2000.0, 4096, 1000, 10000)
        );
    }

    private List<Task> createTestTasks() {
        return List.of(
                new Task(1L, 8, 0.0, 2.0, 5.0),
                new Task(2L, 4, 0.5, 3.0, 8.0),
                new Task(3L, 9, 1.0, 1.5, 4.0)
        );
    }

    @Test
    @DisplayName("Verify complete TaskCloudletAdapter translation and CloudSim execution pipeline")
    void testCompleteExecutionPipeline() {
        List<CloudVmSpec> vms = createTestVms();
        List<Task> tasks = createTestTasks();

        // 1. Verify Task to Cloudlet adaptation
        List<CloudletSimple> cloudlets = TaskCloudletAdapter.toCloudletList(tasks, REF_MIPS);
        assertEquals(3, cloudlets.size(), "All 3 tasks must be translated into Cloudlets");
        assertEquals(2000, cloudlets.get(0).getLength(), "2.0s * 1000 MIPS = 2000 MI");
        assertEquals(3000, cloudlets.get(1).getLength(), "3.0s * 1000 MIPS = 3000 MI");
        assertEquals(1500, cloudlets.get(2).getLength(), "1.5s * 1000 MIPS = 1500 MI");
        assertEquals(0.0, cloudlets.get(0).getSubmissionDelay(), 1e-4);
        assertEquals(0.5, cloudlets.get(1).getSubmissionDelay(), 1e-4);
        assertEquals(1.0, cloudlets.get(2).getSubmissionDelay(), 1e-4);

        // 2. Derive scheduling plan using Proposed Scheduler
        List<Task> proposedTasks = WorkloadGenerator.cloneWorkload(tasks);
        ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(vms, REF_MIPS);
        scheduler.schedule(proposedTasks);

        assertFalse(scheduler.getAssignments().isEmpty(), "Scheduler must generate VM assignments");
        assertEquals(3, scheduler.getAssignments().size());

        // 3. Execute in CloudSim Plus environment
        CloudSimEnvironment env = new CloudSimEnvironment(vms);
        CloudSimEnvironment.CloudSimExecutionResult result = env.execute(proposedTasks, scheduler.getAssignments(), REF_MIPS);

        assertNotNull(result, "CloudSim execution result must not be null");
        assertTrue(result.simulationClock() > 0.0, "Simulation clock must advance");
        assertEquals(3, result.records().size(), "Must produce 3 task execution records");

        // 4. Verify actual CloudSim Plus execution timestamps
        for (TaskExecutionRecord rec : result.records()) {
            assertTrue(rec.startTime() >= 0.0, "Start time must be non-negative");
            assertTrue(rec.completionTime() > rec.startTime(), "Completion time must be after start time");
            assertTrue(rec.waitingTime() >= 0.0, "Waiting time must be non-negative");
            assertTrue(rec.turnaroundTime() > 0.0, "Turnaround time must be positive");
            assertTrue(rec.assignedVmId() == 0 || rec.assignedVmId() == 1, "Assigned VM must be valid provisioned VM");
        }

        // 5. Verify domain task state transition
        for (Task t : proposedTasks) {
            assertTrue(t.getStatus().isTerminal(), "Executed tasks must reach terminal status");
            assertTrue(t.getStartTime() >= 0.0);
            assertTrue(t.getCompletionTime() > t.getStartTime());
            assertTrue(t.getAllocatedVmId() >= 0);
        }

        // 6. Verify Metrics derived from actual CloudSim results
        MetricsCalculator.MetricsSummary summary = MetricsCalculator.calculate(result.records(), vms.size());
        assertTrue(summary.makespan() > 0.0, "Makespan must be positive");
        assertTrue(summary.averageWaitingTime() >= 0.0, "Average wait time must be non-negative");
        assertTrue(summary.averageTurnaroundTime() > 0.0, "Average turnaround time must be positive");
        assertTrue(summary.throughput() > 0.0, "Throughput must be positive");
        assertTrue(summary.resourceUtilization() > 0.0 && summary.resourceUtilization() <= 100.0, "Utilization must be in (0, 100]");
        assertEquals(3, summary.completedTaskCount());
    }
}
