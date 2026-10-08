package scheduler.experiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import scheduler.baseline.BaselinePriorityScheduler;
import scheduler.baseline.SchedulingResult;
import scheduler.model.CloudVmSpec;
import scheduler.model.Task;
import scheduler.proposed.DeadlineAwarePriorityCalculator;
import scheduler.proposed.ProposedPriorityScheduler;
import scheduler.proposed.ProposedSchedulingResult;
import scheduler.proposed.SchedulingMode;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WorkloadEvaluationSuiteRunnerTest {

    public record SchedulerResultRow(
        String workloadName,
        int taskCount,
        String schedulerName,
        double makespan,
        double avgWait,
        double avgTurnaround,
        double throughput,
        double deadlineMissRate,
        double vmUtilization,
        int preemptions
    ) {}

    @Test
    @DisplayName("Execute full evaluation across all 8 workloads and 3 schedulers")
    void executeFullWorkloadEvaluationSuite() {
        int vmCount = 4;
        double refMips = 1000.0;
        List<CloudVmSpec> vms = ExperimentConfig.createHomogeneousVms(vmCount, refMips);
        DeadlineAwarePriorityCalculator calc = new DeadlineAwarePriorityCalculator();

        List<EvaluationWorkloadSuite.WorkloadDefinition> workloads = EvaluationWorkloadSuite.getAllWorkloads();
        assertEquals(8, workloads.size(), "Must contain exactly 8 workload presets");

        List<SchedulerResultRow> rows = new ArrayList<>();

        System.out.println("========================================================================================================================");
        System.out.println("COMPREHENSIVE WORKLOAD EVALUATION SUITE: 3 SCHEDULING CONFIGURATIONS");
        System.out.println("A: Standard Priority Scheduler (IEEE Baseline)");
        System.out.println("B: Proposed Deadline-Aware Scheduler — NON_PREEMPTIVE");
        System.out.println("C: Proposed Deadline-Aware Scheduler — PREEMPTIVE");
        System.out.println("Topology: 4 Heterogeneous/Homogeneous Virtual Machines @ 1,000 MIPS each");
        System.out.println("========================================================================================================================");

        for (EvaluationWorkloadSuite.WorkloadDefinition def : workloads) {
            List<Task> originalTasks = def.tasks();
            int n = originalTasks.size();

            // 1. Standard Priority
            List<Task> tasksA = cloneTasks(originalTasks);
            BaselinePriorityScheduler baselineSched = new BaselinePriorityScheduler(vms, refMips);
            List<SchedulingResult> resultsA = baselineSched.schedule(tasksA);
            MetricsCalculator.MetricsSummary sumA = MetricsCalculator.calculateFromBaseline(resultsA, vmCount);

            rows.add(new SchedulerResultRow(
                def.name(), n, "Standard Priority",
                sumA.makespan(), sumA.averageWaitingTime(), sumA.averageTurnaroundTime(),
                sumA.throughput(), sumA.deadlineMissRate(), sumA.resourceUtilization(), 0
            ));

            // 2. Proposed Non-Preemptive
            List<Task> tasksB = cloneTasks(originalTasks);
            ProposedPriorityScheduler proposedNpSched = new ProposedPriorityScheduler(vms, refMips, calc, SchedulingMode.NON_PREEMPTIVE);
            List<ProposedSchedulingResult> resultsB = proposedNpSched.schedule(tasksB);
            MetricsCalculator.MetricsSummary sumB = MetricsCalculator.calculateFromProposed(resultsB, vmCount);

            rows.add(new SchedulerResultRow(
                def.name(), n, "Proposed Non-Preemptive",
                sumB.makespan(), sumB.averageWaitingTime(), sumB.averageTurnaroundTime(),
                sumB.throughput(), sumB.deadlineMissRate(), sumB.resourceUtilization(), proposedNpSched.getTotalPreemptions()
            ));

            // 3. Proposed Preemptive
            List<Task> tasksC = cloneTasks(originalTasks);
            ProposedPriorityScheduler proposedPSched = new ProposedPriorityScheduler(vms, refMips, calc, SchedulingMode.PREEMPTIVE);
            List<ProposedSchedulingResult> resultsC = proposedPSched.schedule(tasksC);
            MetricsCalculator.MetricsSummary sumC = MetricsCalculator.calculateFromProposed(resultsC, vmCount);

            rows.add(new SchedulerResultRow(
                def.name(), n, "Proposed Preemptive",
                sumC.makespan(), sumC.averageWaitingTime(), sumC.averageTurnaroundTime(),
                sumC.throughput(), sumC.deadlineMissRate(), sumC.resourceUtilization(), proposedPSched.getTotalPreemptions()
            ));

            // Validations
            assertTrue(sumA.makespan() > 0.0);
            assertTrue(sumB.makespan() > 0.0);
            assertTrue(sumC.makespan() > 0.0);
            assertEquals(0, proposedNpSched.getTotalPreemptions());
        }

        // Print Full Comparative Results Table (Part 11)
        System.out.println("\n| Workload | Tasks | Scheduler | Makespan (s) | Avg Waiting (s) | Avg Turnaround (s) | Throughput (t/s) | Deadline Miss Rate | VM Utilization | Preemptions |");
        System.out.println("|---|---:|---|---:|---:|---:|---:|---:|---:|---:|");

        for (SchedulerResultRow r : rows) {
            System.out.printf("| %s | %d | %s | %.2f | %.2f | %.2f | %.4f | %.2f%% | %.2f%% | %d |\n",
                r.workloadName(), r.taskCount(), r.schedulerName(),
                r.makespan(), r.avgWait(), r.avgTurnaround(), r.throughput(),
                r.deadlineMissRate() * 100.0, r.vmUtilization(), r.preemptions()
            );
        }

        // Print Summary Analysis Table (Part 11)
        System.out.println("\n| Workload | Best Deadline Miss Rate | Best Makespan | Best Throughput | Best Overall for Deadline Work |");
        System.out.println("|---|---|---|---|---|");

        for (EvaluationWorkloadSuite.WorkloadDefinition def : workloads) {
            String name = def.name();
            SchedulerResultRow rA = findRow(rows, name, "Standard Priority");
            SchedulerResultRow rB = findRow(rows, name, "Proposed Non-Preemptive");
            SchedulerResultRow rC = findRow(rows, name, "Proposed Preemptive");

            // Best DMR
            double minDmr = Math.min(rA.deadlineMissRate(), Math.min(rB.deadlineMissRate(), rC.deadlineMissRate()));
            String bestDmr = formatBest(rA.deadlineMissRate(), rB.deadlineMissRate(), rC.deadlineMissRate(), minDmr, true);

            // Best Makespan (min)
            double minSpan = Math.min(rA.makespan(), Math.min(rB.makespan(), rC.makespan()));
            String bestSpan = formatBest(rA.makespan(), rB.makespan(), rC.makespan(), minSpan, true);

            // Best Throughput (max)
            double maxTput = Math.max(rA.throughput(), Math.max(rB.throughput(), rC.throughput()));
            String bestTput = formatBest(rA.throughput(), rB.throughput(), rC.throughput(), maxTput, false);

            // Overall
            String bestOverall;
            if (rC.deadlineMissRate() < rA.deadlineMissRate()) {
                bestOverall = "Proposed Preemptive";
            } else if (rB.deadlineMissRate() < rA.deadlineMissRate()) {
                bestOverall = "Proposed Non-Preemptive";
            } else if (rA.deadlineMissRate() < rB.deadlineMissRate() && rA.deadlineMissRate() < rC.deadlineMissRate()) {
                bestOverall = "Standard Priority";
            } else {
                bestOverall = "Tied / Similar";
            }

            System.out.printf("| %s | %s | %s | %s | %s |\n", name, bestDmr, bestSpan, bestTput, bestOverall);
        }
    }

    private SchedulerResultRow findRow(List<SchedulerResultRow> rows, String workload, String sched) {
        return rows.stream()
            .filter(r -> r.workloadName().equals(workload) && r.schedulerName().equals(sched))
            .findFirst()
            .orElseThrow();
    }

    private String formatBest(double vA, double vB, double vC, double target, boolean isMin) {
        List<String> best = new ArrayList<>();
        double eps = 1e-4;
        if (Math.abs(vA - target) <= eps) best.add("Standard");
        if (Math.abs(vB - target) <= eps) best.add("Proposed-NP");
        if (Math.abs(vC - target) <= eps) best.add("Proposed-P");
        if (best.size() == 3) return "All Tied (%.2f)".formatted(target);
        return String.join(" & ", best) + " (%.2f)".formatted(target);
    }

    private List<Task> cloneTasks(List<Task> tasks) {
        List<Task> copy = new ArrayList<>(tasks.size());
        for (Task t : tasks) {
            copy.add(new Task(t.getTaskId(), t.getPriority(), t.getArrivalTime(), t.getExecutionTime(), t.getDeadline()));
        }
        return copy;
    }
}
