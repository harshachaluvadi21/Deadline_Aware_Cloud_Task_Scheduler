package scheduler.baseline;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import scheduler.model.CloudVmSpec;
import scheduler.model.Task;
import scheduler.model.TaskStatus;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for {@link BaselinePriorityScheduler} validating:
 * - Priority Assignment integration
 * - Fibonacci Heap insertion & extraction ordering
 * - Equal priority and equal waiting time tie-breaking
 * - Different arrival times and different execution times
 * - Single VM and multi-VM assignment
 * - SchedulingResult generation (waiting time, turnaround time, observed deadline miss)
 * - Deterministic repeatability
 * - End-to-end execution
 */
class BaselinePrioritySchedulerTest {

    private Task createTask(long id, int basePriority, double arrivalTime, double executionTime, double deadline) {
        return new Task(id, basePriority, arrivalTime, executionTime, deadline);
    }

    private List<CloudVmSpec> createHeterogeneousVms() {
        return List.of(
            CloudVmSpec.of(0, 500.0, 1024, 1000, 10000),   // slow VM
            CloudVmSpec.of(1, 1000.0, 2048, 1000, 10000),  // medium VM (reference)
            CloudVmSpec.of(2, 2000.0, 4096, 1000, 10000)   // fast VM
        );
    }

    @Nested
    @DisplayName("Fibonacci Heap Insertion & Priority Extraction Tests")
    class HeapAndPriorityTests {

        @Test
        @DisplayName("Verifies tasks are extracted and scheduled strictly by computed IEEE baseline priority")
        void testPriorityExtractionOrder() {
            // Tasks with distinct execution times produce distinct baseline priorities via PAT
            Task t1 = createTask(1, 5, 0.0, 5.0, 50.0);
            Task t2 = createTask(2, 5, 0.0, 10.0, 50.0);
            Task t3 = createTask(3, 5, 0.0, 20.0, 50.0);

            // Single VM ensures sequential execution in exact extraction order
            BaselinePriorityScheduler scheduler = new BaselinePriorityScheduler(1);
            List<SchedulingResult> results = scheduler.schedule(List.of(t1, t2, t3));

            assertEquals(3, results.size());

            // T3 faces greatest sequential wait -> receives highest PAT priority score -> extracted first
            assertEquals(3, results.get(0).taskId());
            assertEquals(2, results.get(1).taskId());
            assertEquals(1, results.get(2).taskId());

            // Check dynamic priorities are assigned and descending in extraction order
            assertTrue(results.get(0).dynamicPriority() >= results.get(1).dynamicPriority());
            assertTrue(results.get(1).dynamicPriority() >= results.get(2).dynamicPriority());
        }

        @Test
        @DisplayName("Verifies deterministic tie-breaking for equal priority using Task ID")
        void testEqualPriorityDeterministicTieBreaking() {
            // Identical tasks with identical pre-assigned dynamic priorities
            Task t1 = createTask(10, 5, 0.0, 10.0, 50.0);
            Task t2 = createTask(20, 5, 0.0, 10.0, 50.0);
            t1.setDynamicPriority(0.75);
            t2.setDynamicPriority(0.75);

            BaselinePriorityScheduler scheduler = new BaselinePriorityScheduler(1);
            List<SchedulingResult> results = scheduler.schedule(List.of(t1, t2));

            assertEquals(2, results.size());
            // Lower taskId (10) must be scheduled before higher taskId (20)
            assertEquals(10, results.get(0).taskId());
            assertEquals(20, results.get(1).taskId());
        }
    }

    @Nested
    @DisplayName("Timing, Arrival & Execution Duration Tests")
    class TimingAndArrivalTests {

        @Test
        @DisplayName("Verifies execution with different task arrival times")
        void testDifferentArrivalTimes() {
            Task t1 = createTask(1, 5, 0.0, 10.0, 100.0);
            Task t2 = createTask(2, 5, 15.0, 10.0, 100.0);

            BaselinePriorityScheduler scheduler = new BaselinePriorityScheduler(1);
            List<SchedulingResult> results = scheduler.schedule(List.of(t1, t2));

            assertEquals(2, results.size());

            SchedulingResult r1 = results.get(0);
            SchedulingResult r2 = results.get(1);

            assertEquals(0.0, r1.startTime(), 1e-9);
            assertEquals(10.0, r1.completionTime(), 1e-9);
            assertEquals(0.0, r1.waitingTime(), 1e-9);

            // T2 arrives at 15.0, after VM completed T1 at 10.0 -> VM is idle until 15.0
            assertEquals(15.0, r2.startTime(), 1e-9);
            assertEquals(25.0, r2.completionTime(), 1e-9);
            assertEquals(0.0, r2.waitingTime(), 1e-9);
        }

        @Test
        @DisplayName("Verifies non-preemptive queuing delay when task arrives while VM is busy")
        void testQueuingDelayWhenBusy() {
            Task t1 = createTask(1, 5, 0.0, 20.0, 100.0);
            Task t2 = createTask(2, 5, 5.0, 10.0, 100.0);

            BaselinePriorityScheduler scheduler = new BaselinePriorityScheduler(1);
            List<SchedulingResult> results = scheduler.schedule(List.of(t1, t2));

            SchedulingResult r1 = results.get(0);
            SchedulingResult r2 = results.get(1);

            assertEquals(0.0, r1.startTime(), 1e-9);
            assertEquals(20.0, r1.completionTime(), 1e-9);

            // T2 arrives at 5.0 but VM is busy until 20.0 -> starts at 20.0, waits 15.0
            assertEquals(20.0, r2.startTime(), 1e-9);
            assertEquals(30.0, r2.completionTime(), 1e-9);
            assertEquals(15.0, r2.waitingTime(), 1e-9);
            assertEquals(25.0, r2.turnaroundTime(), 1e-9);
        }

        @Test
        @DisplayName("Verifies handling of different execution times across heterogeneous VMs")
        void testHeterogeneousVmScaling() {
            List<CloudVmSpec> vms = List.of(
                CloudVmSpec.of(0, 500.0, 1024, 1000, 10000),  // 0.5x speed relative to 1000 MIPS
                CloudVmSpec.of(1, 2000.0, 1024, 1000, 10000)  // 2.0x speed relative to 1000 MIPS
            );

            // Reference MIPS = 1000.0
            BaselinePriorityScheduler scheduler = new BaselinePriorityScheduler(vms, 1000.0);

            Task t1 = createTask(1, 5, 0.0, 10.0, 100.0);
            Task t2 = createTask(2, 5, 0.0, 10.0, 100.0);

            List<SchedulingResult> results = scheduler.schedule(List.of(t1, t2));

            assertEquals(2, results.size());
            // Both free at time 0 -> First assigned task goes to faster VM (vm 1, 2000 MIPS)
            SchedulingResult r1 = results.get(0);
            assertEquals(1, r1.assignedVmId(), "Faster VM (id 1) should be chosen first among idle VMs");
            // 10.0 nominal seconds at 2000 MIPS with 1000 ref MIPS -> 5.0 seconds
            assertEquals(5.0, r1.completionTime(), 1e-9);

            // Second task goes to VM 0 (500 MIPS) -> 10.0 * (1000/500) = 20.0 seconds
            SchedulingResult r2 = results.get(1);
            assertEquals(0, r2.assignedVmId());
            assertEquals(20.0, r2.completionTime(), 1e-9);
        }
    }

    @Nested
    @DisplayName("SchedulingResult & Deadline Invariant Tests")
    class ResultAndDeadlineTests {

        @Test
        @DisplayName("Verifies deadline miss is recorded purely as a post-execution measurement")
        void testDeadlineMissObservation() {
            // Task 1 has generous deadline (50.0); Task 2 has impossible deadline (5.0 < execution 15.0)
            Task t1 = createTask(1, 5, 0.0, 10.0, 50.0);
            Task t2 = createTask(2, 5, 0.0, 15.0, 5.0);

            BaselinePriorityScheduler scheduler = new BaselinePriorityScheduler(1);
            List<SchedulingResult> results = scheduler.schedule(List.of(t1, t2));

            assertEquals(2, results.size());

            // One completed, one missed
            SchedulingResult resT1 = results.stream().filter(r -> r.taskId() == 1).findFirst().orElseThrow();
            SchedulingResult resT2 = results.stream().filter(r -> r.taskId() == 2).findFirst().orElseThrow();

            assertFalse(resT1.deadlineMissed(), "Task 1 completed within deadline");
            assertEquals(TaskStatus.COMPLETED, t1.getStatus());

            assertTrue(resT2.deadlineMissed(), "Task 2 completed after deadline");
            assertEquals(TaskStatus.MISSED_DEADLINE, t2.getStatus());
        }

        @Test
        @DisplayName("Verifies that deadline does NOT influence baseline priority calculation or ordering")
        void testDeadlineDoesNotInfluenceBaselineOrdering() {
            // tA has very tight deadline (10.0); tB has loose deadline (500.0)
            // But tB has longer sequential wait -> gets higher baseline priority via PAT
            Task tA = createTask(1, 5, 0.0, 5.0, 10.0);
            Task tB = createTask(2, 5, 0.0, 20.0, 500.0);

            BaselinePriorityScheduler scheduler = new BaselinePriorityScheduler(1);
            List<SchedulingResult> results = scheduler.schedule(List.of(tA, tB));

            // In IEEE baseline, tB has higher sequential wait, so tB gets higher PAT priority
            assertEquals(2, results.get(0).taskId(), "IEEE baseline must NOT prioritize earlier deadline");
            assertEquals(1, results.get(1).taskId());
        }

        @Test
        @DisplayName("Verifies VmAssignment record captures correct metrics")
        void testVmAssignmentRecords() {
            Task t1 = createTask(1, 5, 0.0, 8.0, 50.0);
            BaselinePriorityScheduler scheduler = new BaselinePriorityScheduler(1);
            scheduler.schedule(List.of(t1));

            List<VmAssignment> assignments = scheduler.getAssignments();
            assertEquals(1, assignments.size());
            VmAssignment va = assignments.get(0);

            assertEquals(1, va.taskId());
            assertEquals(0, va.vmId());
            assertEquals(0.0, va.startTime(), 1e-9);
            assertEquals(8.0, va.completionTime(), 1e-9);
            assertEquals(8.0, va.executionDuration(), 1e-9);
        }
    }

    @Nested
    @DisplayName("End-to-End & Deterministic Repeatability Tests")
    class EndToEndTests {

        @Test
        @DisplayName("End-to-End multi-task baseline scheduling on heterogeneous VM cluster")
        void testEndToEndHeterogeneousCluster() {
            List<CloudVmSpec> vms = createHeterogeneousVms();
            BaselinePriorityScheduler scheduler = new BaselinePriorityScheduler(vms, 1000.0);

            List<Task> tasks = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                tasks.add(createTask(i, 5, i * 0.5, 5.0 + i * 2.0, 200.0));
            }

            List<SchedulingResult> results = scheduler.schedule(tasks);

            assertEquals(10, results.size());

            // All tasks must have valid completed/missed states
            for (SchedulingResult r : results) {
                assertTrue(r.completionTime() > r.startTime());
                assertTrue(r.turnaroundTime() >= r.waitingTime());
                assertTrue(r.assignedVmId() >= 0 && r.assignedVmId() <= 2);
                assertTrue(r.dynamicPriority() > 0.0 && r.dynamicPriority() < 1.0);
            }

            // Verify task states are properly transitioned
            for (Task t : tasks) {
                assertTrue(t.getStatus() == TaskStatus.COMPLETED || t.getStatus() == TaskStatus.MISSED_DEADLINE);
                assertTrue(t.getStartTime() >= 0.0);
                assertTrue(t.getCompletionTime() >= t.getStartTime());
                assertTrue(t.getAllocatedVmId() >= 0);
            }
        }

        @Test
        @DisplayName("Verifies 100% deterministic reproducibility across multiple runs")
        void testDeterministicExecutionAcrossRuns() {
            List<CloudVmSpec> vms = createHeterogeneousVms();

            List<Task> run1Tasks = List.of(
                createTask(1, 5, 0.0, 10.0, 100.0),
                createTask(2, 5, 2.0, 15.0, 100.0),
                createTask(3, 5, 4.0, 8.0, 100.0),
                createTask(4, 5, 6.0, 20.0, 100.0)
            );

            List<Task> run2Tasks = List.of(
                createTask(1, 5, 0.0, 10.0, 100.0),
                createTask(2, 5, 2.0, 15.0, 100.0),
                createTask(3, 5, 4.0, 8.0, 100.0),
                createTask(4, 5, 6.0, 20.0, 100.0)
            );

            BaselinePriorityScheduler s1 = new BaselinePriorityScheduler(vms, 1000.0);
            BaselinePriorityScheduler s2 = new BaselinePriorityScheduler(vms, 1000.0);

            List<SchedulingResult> r1 = s1.schedule(run1Tasks);
            List<SchedulingResult> r2 = s2.schedule(run2Tasks);

            assertEquals(r1.size(), r2.size());
            for (int i = 0; i < r1.size(); i++) {
                assertEquals(r1.get(i).taskId(), r2.get(i).taskId());
                assertEquals(r1.get(i).assignedVmId(), r2.get(i).assignedVmId());
                assertEquals(r1.get(i).startTime(), r2.get(i).startTime(), 1e-12);
                assertEquals(r1.get(i).completionTime(), r2.get(i).completionTime(), 1e-12);
                assertEquals(r1.get(i).waitingTime(), r2.get(i).waitingTime(), 1e-12);
                assertEquals(r1.get(i).dynamicPriority(), r2.get(i).dynamicPriority(), 1e-12);
            }
        }

        @Test
        @DisplayName("Verifies alias BaselineScheduler works identically")
        void testAliasBaselineScheduler() {
            BaselineScheduler scheduler = new BaselineScheduler(2);
            List<Task> tasks = List.of(
                createTask(1, 5, 0.0, 10.0, 100.0),
                createTask(2, 5, 0.0, 5.0, 100.0)
            );

            List<SchedulingResult> results = scheduler.schedule(tasks);
            assertEquals(2, results.size());
        }
    }
}
