package scheduler.proposed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import scheduler.baseline.BaselinePriorityScheduler;
import scheduler.baseline.SchedulingResult;
import scheduler.model.CloudVmSpec;
import scheduler.model.Task;
import scheduler.model.TaskStatus;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for {@link ProposedPriorityScheduler} validating:
 * - Dynamic priority recalculation & heap rebuilding
 * - Overdue task execution (not discarded)
 * - Trade-off ordering (base priority vs deadline urgency)
 * - Deterministic tie-breaking (earlier deadline, then task ID)
 * - Baseline independence (verifying baseline remains completely untouched)
 * - End-to-end multi-task execution on heterogeneous VM cluster
 */
class ProposedPrioritySchedulerTest {

    private Task createTask(long id, int priority, double arrivalTime, double executionTime, double deadline) {
        return new Task(id, priority, arrivalTime, executionTime, deadline);
    }

    private List<CloudVmSpec> createHeterogeneousVms() {
        return List.of(
            CloudVmSpec.of(0, 500.0, 1024, 1000, 10000),
            CloudVmSpec.of(1, 1000.0, 2048, 1000, 10000),
            CloudVmSpec.of(2, 2000.0, 4096, 1000, 10000)
        );
    }

    @Nested
    @DisplayName("Dynamic Rebuilding & Urgency Prioritization Tests")
    class DynamicSchedulingTests {

        @Test
        @DisplayName("Urgent task is scheduled before relaxed task even if base priority is lower")
        void testUrgentTaskPrioritizedOverRelaxedTask() {
            // Task 1: High priority (9), relaxed deadline (200.0), exec = 10.0
            // Task 2: Low priority (2), urgent deadline (12.0), exec = 10.0
            Task relaxed = createTask(1, 9, 0.0, 10.0, 200.0);
            Task urgent = createTask(2, 2, 0.0, 10.0, 12.0);

            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(1);
            List<ProposedSchedulingResult> results = scheduler.schedule(List.of(relaxed, urgent));

            assertEquals(2, results.size());
            // Urgent task must be scheduled first
            assertEquals(2, results.get(0).taskId(), "Urgent task 2 should be scheduled before relaxed task 1");
            assertEquals(1, results.get(1).taskId());

            // Task 2 starts at 0.0 and finishes at 10.0 <= 12.0 -> COMPLETED
            assertEquals(0.0, results.get(0).startTime(), 1e-9);
            assertEquals(10.0, results.get(0).completionTime(), 1e-9);
            assertFalse(results.get(0).deadlineMissed());

            // Task 1 starts at 10.0 and finishes at 20.0 <= 200.0 -> COMPLETED
            assertEquals(10.0, results.get(1).startTime(), 1e-9);
            assertEquals(20.0, results.get(1).completionTime(), 1e-9);
            assertFalse(results.get(1).deadlineMissed());
        }

        @Test
        @DisplayName("When urgency is equal/relaxed, higher base priority wins")
        void testBasePriorityWinsWhenDeadlinesRelaxed() {
            // Both tasks have very loose deadlines (200.0)
            Task highPriority = createTask(1, 10, 0.0, 10.0, 200.0);
            Task lowPriority = createTask(2, 2, 0.0, 10.0, 200.0);

            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(1);
            List<ProposedSchedulingResult> results = scheduler.schedule(List.of(highPriority, lowPriority));

            assertEquals(2, results.size());
            assertEquals(1, results.get(0).taskId(), "High base priority should win when both deadlines are relaxed");
            assertEquals(2, results.get(1).taskId());
        }

        @Test
        @DisplayName("Deterministic tie-breaking uses earlier deadline, then lowest Task ID")
        void testDeterministicTieBreaking() {
            // Identical base priority (5), same arrival (0), same exec (10)
            // Task 10 has earlier deadline (30.0), Task 20 has later deadline (40.0)
            Task t10 = createTask(10, 5, 0.0, 10.0, 30.0);
            Task t20 = createTask(20, 5, 0.0, 10.0, 40.0);

            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(1);
            List<ProposedSchedulingResult> results = scheduler.schedule(List.of(t20, t10));

            assertEquals(10, results.get(0).taskId(), "Earlier deadline (t10) must be scheduled first");
            assertEquals(20, results.get(1).taskId());
        }
    }

    @Nested
    @DisplayName("Overdue Task Handling Tests")
    class OverdueHandlingTests {

        @Test
        @DisplayName("Overdue task (currentTime >= deadline) is executed rather than discarded")
        void testOverdueTaskIsExecuted() {
            // Task 1 arrives at 0, exec = 20, deadline = 10 (already overdue before start)
            Task overdueTask = createTask(1, 5, 0.0, 20.0, 10.0);

            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(1);
            List<ProposedSchedulingResult> results = scheduler.schedule(List.of(overdueTask));

            assertEquals(1, results.size());
            ProposedSchedulingResult r = results.get(0);

            assertEquals(0.0, r.startTime(), 1e-9);
            assertEquals(20.0, r.completionTime(), 1e-9);
            assertTrue(r.deadlineMissed(), "Overdue task must be marked as deadline-missed post-execution");
            assertEquals(TaskStatus.MISSED_DEADLINE, overdueTask.getStatus());
        }
    }

    @Nested
    @DisplayName("Baseline Independence Verification Tests")
    class BaselineIndependenceTests {

        @Test
        @DisplayName("Verifies Phase 6 baseline behavior is completely unaffected by deadline changes")
        void testBaselineIgnoresDeadlines() {
            // Create two tasks:
            // Case A: Task 1 has loose deadline, Task 2 has tight deadline
            Task t1A = createTask(1, 5, 0.0, 5.0, 500.0);
            Task t2A = createTask(2, 5, 0.0, 20.0, 25.0);

            // Case B: Invert deadlines: Task 1 tight, Task 2 loose
            Task t1B = createTask(1, 5, 0.0, 5.0, 10.0);
            Task t2B = createTask(2, 5, 0.0, 20.0, 500.0);

            BaselinePriorityScheduler baseline = new BaselinePriorityScheduler(1);

            List<SchedulingResult> resA = baseline.schedule(List.of(t1A, t2A));
            List<SchedulingResult> resB = baseline.schedule(List.of(t1B, t2B));

            // Baseline ordering MUST BE IDENTICAL in both cases (governed strictly by PAT)
            assertEquals(resA.get(0).taskId(), resB.get(0).taskId(),
                "Baseline scheduler must produce identical task ordering regardless of deadline changes");
            assertEquals(resA.get(1).taskId(), resB.get(1).taskId());
        }
    }

    @Nested
    @DisplayName("End-to-End & Heterogeneous Cluster Tests")
    class EndToEndTests {

        @Test
        @DisplayName("End-to-End execution of 10 tasks on 3 heterogeneous VMs")
        void testEndToEndHeterogeneousCluster() {
            List<CloudVmSpec> vms = createHeterogeneousVms();
            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(vms, 1000.0);

            List<Task> tasks = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                // Vary priorities and deadlines
                int priority = (i % 10) + 1;
                double deadline = 10.0 + (i * 15.0);
                tasks.add(createTask(i, priority, i * 0.5, 5.0 + (i * 2.0), deadline));
            }

            List<ProposedSchedulingResult> results = scheduler.schedule(tasks);

            assertEquals(10, results.size());

            // All tasks must have valid execution parameters
            for (ProposedSchedulingResult r : results) {
                assertTrue(r.completionTime() > r.startTime());
                assertTrue(r.turnaroundTime() >= r.waitingTime());
                assertTrue(r.assignedVmId() >= 0 && r.assignedVmId() <= 2);
                assertTrue(r.finalScore() >= 0.0 && r.finalScore() <= 1.0);
            }

            for (Task t : tasks) {
                assertTrue(t.getStatus() == TaskStatus.COMPLETED || t.getStatus() == TaskStatus.MISSED_DEADLINE);
                assertTrue(t.getStartTime() >= 0.0);
                assertTrue(t.getCompletionTime() >= t.getStartTime());
            }
        }

        @Test
        @DisplayName("Verifies 100% deterministic reproducibility across multiple runs")
        void testDeterministicExecutionAcrossRuns() {
            List<CloudVmSpec> vms = createHeterogeneousVms();

            List<Task> run1Tasks = List.of(
                createTask(1, 8, 0.0, 10.0, 30.0),
                createTask(2, 3, 2.0, 15.0, 25.0),
                createTask(3, 10, 4.0, 8.0, 50.0),
                createTask(4, 1, 6.0, 20.0, 40.0)
            );

            List<Task> run2Tasks = List.of(
                createTask(1, 8, 0.0, 10.0, 30.0),
                createTask(2, 3, 2.0, 15.0, 25.0),
                createTask(3, 10, 4.0, 8.0, 50.0),
                createTask(4, 1, 6.0, 20.0, 40.0)
            );

            ProposedPriorityScheduler s1 = new ProposedPriorityScheduler(vms, 1000.0);
            ProposedPriorityScheduler s2 = new ProposedPriorityScheduler(vms, 1000.0);

            List<ProposedSchedulingResult> r1 = s1.schedule(run1Tasks);
            List<ProposedSchedulingResult> r2 = s2.schedule(run2Tasks);

            assertEquals(r1.size(), r2.size());
            for (int i = 0; i < r1.size(); i++) {
                assertEquals(r1.get(i).taskId(), r2.get(i).taskId());
                assertEquals(r1.get(i).assignedVmId(), r2.get(i).assignedVmId());
                assertEquals(r1.get(i).startTime(), r2.get(i).startTime(), 1e-12);
                assertEquals(r1.get(i).completionTime(), r2.get(i).completionTime(), 1e-12);
                assertEquals(r1.get(i).finalScore(), r2.get(i).finalScore(), 1e-12);
            }
        }
    }
}
