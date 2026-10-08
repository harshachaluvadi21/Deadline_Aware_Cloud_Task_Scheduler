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
 * - Dynamic priority recalculation & persistent Fibonacci Heap updates via decreaseKey()
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
            // Task 1: High base priority (9), relaxed deadline (200.0), exec = 10.0
            // Task 2: Low base priority (2), critical deadline (10.0), exec = 10.0
            Task relaxed = createTask(1, 9, 0.0, 10.0, 200.0);
            Task urgent = createTask(2, 2, 0.0, 10.0, 10.0);

            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(1);
            List<ProposedSchedulingResult> results = scheduler.schedule(List.of(relaxed, urgent));

            assertEquals(2, results.size());
            // Urgent task must be scheduled first
            assertEquals(2, results.get(0).taskId(), "Urgent task 2 should be scheduled before relaxed task 1");
            assertEquals(1, results.get(1).taskId());

            // Task 2 starts at 0.0 and finishes at 10.0 <= 10.0 -> COMPLETED
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

        @Test
        @DisplayName("Dynamic priority escalation enables waiting task to overtake higher base priority task")
        void testDynamicPriorityEscalationAndOvertaking() {
            // Task 1: High priority (8), burst 5.0, deadline 60.0, arrives 0.0
            // Task 2: Low priority (3), burst 8.0, deadline 18.0, arrives 0.0
            // Task 3: Medium priority (5), burst 2.0, deadline 25.0, arrives 0.0
            Task t1 = createTask(1, 8, 0.0, 5.0, 60.0);
            Task t2 = createTask(2, 3, 0.0, 8.0, 18.0);
            Task t3 = createTask(3, 5, 0.0, 2.0, 25.0);

            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(1);
            List<ProposedSchedulingResult> results = scheduler.schedule(List.of(t1, t2, t3));

            assertEquals(3, results.size());
            // At t=0: T1 executes first (highest base priority 8, P~819)
            assertEquals(1, results.get(0).taskId());

            // At t=5.0: T2 has waited 5s, slack reduced to 5.0, burst 8.0 -> P(5) ~ 611.8 > T3 (P ~ 607.6)
            // T2 overtakes T3 via decreaseKey in Fibonacci Heap
            assertEquals(2, results.get(1).taskId(), "Task 2 should overtake Task 3 at t=5 due to slack and burst urgency");
            assertEquals(3, results.get(2).taskId());
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
                int priority = (i % 10) + 1;
                double deadline = 10.0 + (i * 15.0);
                tasks.add(createTask(i, priority, i * 0.5, 5.0 + (i * 2.0), deadline));
            }

            List<ProposedSchedulingResult> results = scheduler.schedule(tasks);

            assertEquals(10, results.size());

            for (ProposedSchedulingResult r : results) {
                assertTrue(r.completionTime() > r.startTime());
                assertTrue(r.turnaroundTime() >= r.waitingTime());
                assertTrue(r.assignedVmId() >= 0 && r.assignedVmId() <= 2);
                assertTrue(r.finalScore() > 0.0);
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

    @Nested
    @DisplayName("Preemptive and Dual-Mode Scheduling Tests (Part 10 & Part 11)")
    class PreemptiveAndDualModeTests {

        @Test
        @DisplayName("Part 11 - Exact edge case: Task A (Arr 0, B 10, D 30, P 3) and Task B (Arr 3, B 4, D 8, P 5)")
        void testPart11ExactEdgeCase() {
            // Task A: Arr 0, Burst 10, Dline 30, Priority 3
            // Task B: Arr 3, Burst 4, Dline 8, Priority 5
            Task taskA = createTask(1, 3, 0.0, 10.0, 30.0);
            Task taskB = createTask(2, 5, 3.0, 4.0, 8.0);

            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(1, SchedulingMode.PREEMPTIVE);
            List<ProposedSchedulingResult> results = scheduler.schedule(List.of(taskA, taskB));

            assertEquals(2, results.size());
            assertEquals(1, scheduler.getTotalPreemptions(), "Preemption count must be exactly 1");

            // Task B should complete first (at t = 7.0)
            ProposedSchedulingResult resB = results.stream()
                .filter(r -> r.taskId() == 2)
                .findFirst()
                .orElseThrow();

            assertEquals(3.0, resB.startTime(), 1e-9, "Task B starts immediately at arrival t = 3");
            assertEquals(7.0, resB.completionTime(), 1e-9, "Task B finishes at t = 7.0");
            assertFalse(resB.deadlineMissed(), "Task B completed before deadline 8.0");
            assertEquals(0, resB.preemptionCount());
            assertEquals(0.0, resB.waitingTime(), 1e-9);
            assertEquals(4.0, resB.turnaroundTime(), 1e-9);

            // Task A should resume after B and finish at t = 14.0
            ProposedSchedulingResult resA = results.stream()
                .filter(r -> r.taskId() == 1)
                .findFirst()
                .orElseThrow();

            assertEquals(0.0, resA.startTime(), 1e-9, "Task A first started at t = 0");
            assertEquals(14.0, resA.completionTime(), 1e-9, "Task A finishes at t = 14.0 (3 + 4 + 7)");
            assertFalse(resA.deadlineMissed(), "Task A completed before deadline 30.0");
            assertEquals(1, resA.preemptionCount(), "Task A was preempted exactly once");
            // Waiting time for A: turnaround (14 - 0) - total execution (10) = 4.0 seconds (waiting while B ran)
            assertEquals(4.0, resA.waitingTime(), 1e-9);
            assertEquals(14.0, resA.turnaroundTime(), 1e-9);

            // Verify task context tracking
            ProposedPriorityScheduler.TaskContext ctxA = scheduler.getTaskContext(1);
            assertNotNull(ctxA);
            assertEquals(TaskState.COMPLETED, ctxA.getState());
            assertEquals(10.0, ctxA.getOriginalExecutionTime(), 1e-9);
            assertEquals(0.0, ctxA.getRemainingExecutionTime(), 1e-9);
            assertEquals(10.0, ctxA.getCpuExecutionTimeAccumulated(), 1e-9);
            assertEquals(1, ctxA.getPreemptionCount());

            // Contrast with NON-PREEMPTIVE mode on the exact same workload
            Task taskA_np = createTask(1, 3, 0.0, 10.0, 30.0);
            Task taskB_np = createTask(2, 5, 3.0, 4.0, 8.0);
            ProposedPriorityScheduler npScheduler = new ProposedPriorityScheduler(1, SchedulingMode.NON_PREEMPTIVE);
            List<ProposedSchedulingResult> npResults = npScheduler.schedule(List.of(taskA_np, taskB_np));

            assertEquals(0, npScheduler.getTotalPreemptions(), "Non-preemptive mode must have 0 preemptions");
            ProposedSchedulingResult npB = npResults.stream().filter(r -> r.taskId() == 2).findFirst().orElseThrow();
            // In non-preemptive mode, Task A runs 0..10, so Task B runs 10..14 -> finishes at 14 > 8 (MISSED DEADLINE!)
            assertEquals(10.0, npB.startTime(), 1e-9);
            assertEquals(14.0, npB.completionTime(), 1e-9);
            assertTrue(npB.deadlineMissed(), "Task B must miss deadline in non-preemptive mode");
        }

        @Test
        @DisplayName("1. Non-preemptive basic scheduling preserves run-to-completion and 0 preemptions")
        void testNonPreemptiveBasicScheduling() {
            Task t1 = createTask(1, 5, 0.0, 10.0, 50.0);
            Task t2 = createTask(2, 5, 2.0, 10.0, 50.0);

            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(1, SchedulingMode.NON_PREEMPTIVE);
            List<ProposedSchedulingResult> results = scheduler.schedule(List.of(t1, t2));

            assertEquals(2, results.size());
            assertEquals(0, scheduler.getTotalPreemptions());
            assertEquals(0.0, results.get(0).startTime(), 1e-9);
            assertEquals(10.0, results.get(0).completionTime(), 1e-9);
            assertEquals(10.0, results.get(1).startTime(), 1e-9);
            assertEquals(20.0, results.get(1).completionTime(), 1e-9);
        }

        @Test
        @DisplayName("2. Preemptive basic scheduling when no preemption is needed")
        void testPreemptiveBasicSchedulingNoPreemption() {
            // Task 1 has higher priority and earlier deadline
            Task t1 = createTask(1, 8, 0.0, 5.0, 10.0);
            // Task 2 has lower priority and relaxed deadline
            Task t2 = createTask(2, 2, 2.0, 5.0, 50.0);

            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(1, SchedulingMode.PREEMPTIVE);
            List<ProposedSchedulingResult> results = scheduler.schedule(List.of(t1, t2));

            assertEquals(2, results.size());
            assertEquals(0, scheduler.getTotalPreemptions(), "No preemption when running task has higher priority");
            assertEquals(1, results.get(0).taskId());
            assertEquals(5.0, results.get(0).completionTime(), 1e-9);
            assertEquals(2, results.get(1).taskId());
            assertEquals(10.0, results.get(1).completionTime(), 1e-9);
        }

        @Test
        @DisplayName("3. New task arrival causes preemption when dynamic priority is higher")
        void testNewTaskCausesPreemption() {
            Task running = createTask(1, 2, 0.0, 20.0, 100.0);
            Task arrivingUrgent = createTask(2, 9, 5.0, 5.0, 12.0);

            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(1, SchedulingMode.PREEMPTIVE);
            List<ProposedSchedulingResult> results = scheduler.schedule(List.of(running, arrivingUrgent));

            assertEquals(1, scheduler.getTotalPreemptions());
            // Urgent task finishes first
            assertEquals(2, results.get(0).taskId());
            assertEquals(5.0, results.get(0).startTime(), 1e-9);
            assertEquals(10.0, results.get(0).completionTime(), 1e-9);
        }

        @Test
        @DisplayName("4. Preempted task resumes with correct remaining execution time")
        void testPreemptedTaskResumesWithCorrectRemainingBurst() {
            // Task 1: Burst 10, runs 4s before preemption -> remaining 6s
            Task t1 = createTask(1, 2, 0.0, 10.0, 60.0);
            Task t2 = createTask(2, 8, 4.0, 2.0, 8.0);

            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(1, SchedulingMode.PREEMPTIVE);
            scheduler.schedule(List.of(t1, t2));

            ProposedPriorityScheduler.TaskContext ctx1 = scheduler.getTaskContext(1);
            assertEquals(10.0, ctx1.getOriginalExecutionTime(), 1e-9);
            assertEquals(10.0, ctx1.getCpuExecutionTimeAccumulated(), 1e-9);
            assertEquals(0.0, ctx1.getRemainingExecutionTime(), 1e-9);
            // Completion time = 4 (slice 1) + 2 (t2) + 6 (slice 2) = 12.0
            assertEquals(12.0, ctx1.getCompletionTime(), 1e-9);
        }

        @Test
        @DisplayName("5. No duplicate or stale heap nodes after preemption")
        void testNoDuplicateHeapNodesAfterPreemption() {
            Task t1 = createTask(1, 2, 0.0, 15.0, 50.0);
            Task t2 = createTask(2, 8, 5.0, 3.0, 10.0);
            Task t3 = createTask(3, 9, 6.0, 2.0, 9.0);

            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(1, SchedulingMode.PREEMPTIVE);
            List<ProposedSchedulingResult> results = scheduler.schedule(List.of(t1, t2, t3));

            // Verify each task appears exactly once in results
            assertEquals(3, results.size());
            long uniqueTaskCount = results.stream().map(ProposedSchedulingResult::taskId).distinct().count();
            assertEquals(3, uniqueTaskCount, "There must never be duplicate task completions");
        }

        @Test
        @DisplayName("6. Priority recalculation after time advances reflects waiting time increase")
        void testPriorityRecalculationAfterTimeAdvances() {
            DeadlineAwarePriorityCalculator calc = new DeadlineAwarePriorityCalculator();
            Task task = createTask(1, 5, 0.0, 10.0, 40.0);

            double pAt0 = calc.calculateDynamicPriority(task, 0.0);
            double pAt5 = calc.calculateDynamicPriority(task, 5.0);

            assertTrue(pAt5 > pAt0, "Dynamic priority must strictly increase as time advances due to aging and urgency");
        }

        @Test
        @DisplayName("7. decreaseKey works after priority changes in persistent heap")
        void testDecreaseKeyWorksAfterPriorityChanges() {
            Task tLow = createTask(1, 3, 0.0, 20.0, 50.0);
            Task tMid = createTask(2, 5, 0.0, 10.0, 30.0);

            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(1, SchedulingMode.PREEMPTIVE);
            List<ProposedSchedulingResult> results = scheduler.schedule(List.of(tLow, tMid));

            assertEquals(2, results.size());
            // Task 2 should complete before Task 1
            assertEquals(2, results.get(0).taskId());
            assertEquals(1, results.get(1).taskId());
        }

        @Test
        @DisplayName("8. Task completes correctly after multiple preemptions")
        void testMultiplePreemptionsOnSingleTask() {
            // Task 1: Burst 12. Runs 0..3 (3s), preempted by t2.
            // t2 runs 3..5. Task 1 resumes at 5..7 (2s, total 5s), preempted by t3.
            // t3 runs 7..9. Task 1 resumes at 9..16 (7s remaining, total 12s).
            Task t1 = createTask(1, 1, 0.0, 12.0, 80.0);
            Task t2 = createTask(2, 8, 3.0, 2.0, 7.0);
            Task t3 = createTask(3, 9, 7.0, 2.0, 11.0);

            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(1, SchedulingMode.PREEMPTIVE);
            List<ProposedSchedulingResult> results = scheduler.schedule(List.of(t1, t2, t3));

            assertEquals(3, results.size());
            assertEquals(2, scheduler.getTotalPreemptions(), "Task 1 should be preempted twice");

            ProposedPriorityScheduler.TaskContext ctx1 = scheduler.getTaskContext(1);
            assertEquals(2, ctx1.getPreemptionCount());
            assertEquals(12.0, ctx1.getCpuExecutionTimeAccumulated(), 1e-9);
            assertEquals(16.0, ctx1.getCompletionTime(), 1e-9);
            assertEquals(TaskState.COMPLETED, ctx1.getState());
        }

        @Test
        @DisplayName("9. Task arriving after current task starts does not preempt if lower priority")
        void testTaskArrivingAfterStartDoesNotPreemptIfLower() {
            Task tHigh = createTask(1, 9, 0.0, 10.0, 20.0);
            Task tLow = createTask(2, 2, 4.0, 5.0, 50.0);

            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(1, SchedulingMode.PREEMPTIVE);
            List<ProposedSchedulingResult> results = scheduler.schedule(List.of(tHigh, tLow));

            assertEquals(0, scheduler.getTotalPreemptions());
            assertEquals(1, results.get(0).taskId());
            assertEquals(10.0, results.get(0).completionTime(), 1e-9);
            assertEquals(2, results.get(1).taskId());
            assertEquals(15.0, results.get(1).completionTime(), 1e-9);
        }

        @Test
        @DisplayName("10. Deadline miss calculation is accurately recorded in preemptive mode")
        void testDeadlineMissCalculation() {
            // Both tasks cannot possibly make their tight deadlines on 1 VM
            Task t1 = createTask(1, 5, 0.0, 10.0, 8.0); // misses deadline at t=10 > 8
            Task t2 = createTask(2, 5, 0.0, 10.0, 15.0); // misses deadline at t=20 > 15

            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(1, SchedulingMode.PREEMPTIVE);
            List<ProposedSchedulingResult> results = scheduler.schedule(List.of(t1, t2));

            assertTrue(results.get(0).deadlineMissed());
            assertTrue(results.get(1).deadlineMissed());
        }

        @Test
        @DisplayName("11. Waiting time calculation correctly handles preempted interruptions")
        void testWaitingTimeCalculation() {
            // Task A: Arr 0, B 10. Runs 0..3, preempted. B runs 3..7. A resumes 7..14.
            // Wait time for A = (14 - 0) - 10 = 4.0
            Task tA = createTask(1, 3, 0.0, 10.0, 30.0);
            Task tB = createTask(2, 5, 3.0, 4.0, 8.0);

            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(1, SchedulingMode.PREEMPTIVE);
            scheduler.schedule(List.of(tA, tB));

            ProposedPriorityScheduler.TaskContext ctxA = scheduler.getTaskContext(1);
            assertEquals(4.0, ctxA.getWaitingTime(), 1e-9);

            ProposedPriorityScheduler.TaskContext ctxB = scheduler.getTaskContext(2);
            assertEquals(0.0, ctxB.getWaitingTime(), 1e-9);
        }

        @Test
        @DisplayName("12. Turnaround time calculation: Completion - Arrival")
        void testTurnaroundTimeCalculation() {
            Task tA = createTask(1, 3, 0.0, 10.0, 30.0);
            Task tB = createTask(2, 5, 3.0, 4.0, 8.0);

            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(1, SchedulingMode.PREEMPTIVE);
            scheduler.schedule(List.of(tA, tB));

            ProposedPriorityScheduler.TaskContext ctxA = scheduler.getTaskContext(1);
            assertEquals(14.0, ctxA.getTurnaroundTime(), 1e-9);

            ProposedPriorityScheduler.TaskContext ctxB = scheduler.getTaskContext(2);
            assertEquals(4.0, ctxB.getTurnaroundTime(), 1e-9); // 7.0 - 3.0
        }

        @Test
        @DisplayName("13. Makespan calculation: max(completion) - min(arrival)")
        void testMakespanCalculation() {
            Task tA = createTask(1, 3, 0.0, 10.0, 30.0);
            Task tB = createTask(2, 5, 3.0, 4.0, 8.0);

            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(1, SchedulingMode.PREEMPTIVE);
            List<ProposedSchedulingResult> results = scheduler.schedule(List.of(tA, tB));

            double makespan = results.stream().mapToDouble(ProposedSchedulingResult::completionTime).max().orElse(0.0)
                - results.stream().mapToDouble(ProposedSchedulingResult::arrivalTime).min().orElse(0.0);
            assertEquals(14.0, makespan, 1e-9);
        }

        @Test
        @DisplayName("14. Preemption count is accurately accumulated across multi-task workload")
        void testPreemptionCountAccumulation() {
            // Multi-VM cluster (2 VMs)
            List<CloudVmSpec> vms = List.of(
                CloudVmSpec.of(0, 1000.0, 2048, 1000, 10000),
                CloudVmSpec.of(1, 1000.0, 2048, 1000, 10000)
            );

            Task t1 = createTask(1, 2, 0.0, 20.0, 100.0);
            Task t2 = createTask(2, 2, 0.0, 20.0, 100.0);
            // At t = 5, two urgent tasks arrive
            Task t3 = createTask(3, 9, 5.0, 5.0, 12.0);
            Task t4 = createTask(4, 9, 5.0, 5.0, 12.0);

            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(vms, 1000.0, new DeadlineAwarePriorityCalculator(), SchedulingMode.PREEMPTIVE);
            scheduler.schedule(List.of(t1, t2, t3, t4));

            assertEquals(2, scheduler.getTotalPreemptions(), "Both running low-priority tasks should be preempted");
        }
    }
}
