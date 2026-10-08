package scheduler.proposed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import scheduler.experiment.MetricsCalculator;
import scheduler.model.CloudVmSpec;
import scheduler.model.Task;

import java.util.List;

public class DualModeEvaluationTest {

    @Test
    @DisplayName("Evaluate and compare Non-Preemptive vs Preemptive on representative workloads")
    void evaluateDualModes() {
        System.out.println("================================================================================");
        System.out.println("EVALUATION 1: PART 11 EXACT EDGE CASE (Single VM)");
        System.out.println("Task A (Image Processing): Arr 0, Burst 10, Deadline 30, Priority 3");
        System.out.println("Task B (Video Transcoding): Arr 3, Burst 4, Deadline 8, Priority 5");
        System.out.println("================================================================================");

        Task aNp = new Task(1, 3, 0.0, 10.0, 30.0);
        Task bNp = new Task(2, 5, 3.0, 4.0, 8.0);
        ProposedPriorityScheduler npSched1 = new ProposedPriorityScheduler(1, SchedulingMode.NON_PREEMPTIVE);
        List<ProposedSchedulingResult> npRes1 = npSched1.schedule(List.of(aNp, bNp));
        MetricsCalculator.MetricsSummary npSum1 = MetricsCalculator.calculateFromProposed(npRes1, 1);

        Task aP = new Task(1, 3, 0.0, 10.0, 30.0);
        Task bP = new Task(2, 5, 3.0, 4.0, 8.0);
        ProposedPriorityScheduler pSched1 = new ProposedPriorityScheduler(1, SchedulingMode.PREEMPTIVE);
        List<ProposedSchedulingResult> pRes1 = pSched1.schedule(List.of(aP, bP));
        MetricsCalculator.MetricsSummary pSum1 = MetricsCalculator.calculateFromProposed(pRes1, 1);

        printComparison("Part 11 Workload", npRes1, npSum1, npSched1.getTotalPreemptions(),
                        pRes1, pSum1, pSched1.getTotalPreemptions());

        System.out.println("\n================================================================================");
        System.out.println("EVALUATION 2: REPRESENTATIVE CLOUD WORKLOAD TYPES (2 VMs)");
        System.out.println("T1: Image Processing, T2: Video Transcoding, T3: Database Query, T4: ML Inference, etc.");
        System.out.println("================================================================================");

        List<CloudVmSpec> vms = List.of(
            CloudVmSpec.of(0, 1000.0, 2048, 1000, 10000),
            CloudVmSpec.of(1, 1000.0, 2048, 1000, 10000)
        );

        List<Task> npTasks = List.of(
            new Task(1, 3, 0.0, 8.0, 20.0),   // T1: Image Processing
            new Task(2, 5, 2.0, 15.0, 25.0),  // T2: Video Transcoding
            new Task(3, 4, 4.0, 4.0, 12.0),   // T3: Database Query (urgent!)
            new Task(4, 5, 6.0, 10.0, 18.0),  // T4: ML Model Inference (urgent!)
            new Task(5, 2, 8.0, 6.0, 30.0),   // T5: Log Analysis
            new Task(6, 3, 10.0, 7.0, 22.0),  // T6: File Compression
            new Task(7, 4, 12.0, 12.0, 28.0), // T7: Data Analytics
            new Task(8, 1, 14.0, 20.0, 45.0)  // T8: Backup Processing
        );

        ProposedPriorityScheduler npSched2 = new ProposedPriorityScheduler(vms, 1000.0, new DeadlineAwarePriorityCalculator(), SchedulingMode.NON_PREEMPTIVE);
        List<ProposedSchedulingResult> npRes2 = npSched2.schedule(npTasks);
        MetricsCalculator.MetricsSummary npSum2 = MetricsCalculator.calculateFromProposed(npRes2, 2);

        List<Task> pTasks = List.of(
            new Task(1, 3, 0.0, 8.0, 20.0),
            new Task(2, 5, 2.0, 15.0, 25.0),
            new Task(3, 4, 4.0, 4.0, 12.0),
            new Task(4, 5, 6.0, 10.0, 18.0),
            new Task(5, 2, 8.0, 6.0, 30.0),
            new Task(6, 3, 10.0, 7.0, 22.0),
            new Task(7, 4, 12.0, 12.0, 28.0),
            new Task(8, 1, 14.0, 20.0, 45.0)
        );

        ProposedPriorityScheduler pSched2 = new ProposedPriorityScheduler(vms, 1000.0, new DeadlineAwarePriorityCalculator(), SchedulingMode.PREEMPTIVE);
        List<ProposedSchedulingResult> pRes2 = pSched2.schedule(pTasks);
        MetricsCalculator.MetricsSummary pSum2 = MetricsCalculator.calculateFromProposed(pRes2, 2);

        printComparison("8-Task Representative Cloud Workload", npRes2, npSum2, npSched2.getTotalPreemptions(),
                        pRes2, pSum2, pSched2.getTotalPreemptions());
    }

    private void printComparison(
            String title,
            List<ProposedSchedulingResult> npRes,
            MetricsCalculator.MetricsSummary npSum,
            int npPreemptions,
            List<ProposedSchedulingResult> pRes,
            MetricsCalculator.MetricsSummary pSum,
            int pPreemptions) {

        System.out.println("Execution Order:");
        System.out.print("  NON-PREEMPTIVE: ");
        for (ProposedSchedulingResult r : npRes) {
            System.out.print("T" + r.taskId() + " [" + r.startTime() + "->" + r.completionTime() + (r.deadlineMissed() ? " MISSED" : "") + "]  ");
        }
        System.out.println();

        System.out.print("  PREEMPTIVE:     ");
        for (ProposedSchedulingResult r : pRes) {
            System.out.print("T" + r.taskId() + " [" + r.startTime() + "->" + r.completionTime() + (r.preemptionCount() > 0 ? " (preempted x" + r.preemptionCount() + ")" : "") + (r.deadlineMissed() ? " MISSED" : "") + "]  ");
        }
        System.out.println();

        System.out.println("\nMetric Comparison Table:");
        System.out.printf("%-30s | %-16s | %-16s\n", "Metric", "NON-PREEMPTIVE", "PREEMPTIVE");
        System.out.println("-------------------------------+------------------+-----------------");
        System.out.printf("%-30s | %-16.4f | %-16.4f\n", "Makespan (s)", npSum.makespan(), pSum.makespan());
        System.out.printf("%-30s | %-16.4f | %-16.4f\n", "Average Waiting Time (s)", npSum.averageWaitingTime(), pSum.averageWaitingTime());
        System.out.printf("%-30s | %-16.4f | %-16.4f\n", "Average Turnaround Time (s)", npSum.averageTurnaroundTime(), pSum.averageTurnaroundTime());
        System.out.printf("%-30s | %-16.4f | %-16.4f\n", "Throughput (tasks/s)", npSum.throughput(), pSum.throughput());
        System.out.printf("%-30s | %-16.2f%% | %-16.2f%%\n", "Deadline Miss Rate (%)", npSum.deadlineMissRate() * 100.0, pSum.deadlineMissRate() * 100.0);
        System.out.printf("%-30s | %-16.2f%% | %-16.2f%%\n", "VM Resource Utilization (%)", npSum.resourceUtilization(), pSum.resourceUtilization());
        System.out.printf("%-30s | %-16d | %-16d\n", "Number of Preemptions", npPreemptions, pPreemptions);
    }
}
