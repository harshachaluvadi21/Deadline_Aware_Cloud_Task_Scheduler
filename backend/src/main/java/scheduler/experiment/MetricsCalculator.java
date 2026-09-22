package scheduler.experiment;

import scheduler.baseline.SchedulingResult;
import scheduler.proposed.ProposedSchedulingResult;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Mathematical metrics calculation engine for scheduling experiments.
 *
 * <p><b>Primary Metric - Time-based VM Resource Utilization:</b>
 * \[
 *   \text{ResourceUtilization} = \frac{\sum(\text{VM busy time})}{\text{number of VMs} \times \text{experiment makespan}} \times 100
 * \]
 * <ul>
 *   <li>Designated explicitly as <b>"Time-based VM Resource Utilization"</b>.</li>
 *   <li>Does NOT use MIPS-weighted utilization.</li>
 *   <li>Guaranteed to be strictly bounded within \([0.0, 100.0]\).</li>
 * </ul>
 *
 * <p>All metric calculations are strictly deterministic, objective, and neutral,
 * handling edge cases such as empty task sets, single-task runs, and zero makespan.
 */
public class MetricsCalculator {

    /**
     * Metrics calculation container for experiment summary.
     */
    public record MetricsSummary(
            double makespan,
            double averageWaitingTime,
            double averageTurnaroundTime,
            double throughput,
            double deadlineMissRate,
            int deadlineMissedCount,
            int completedTaskCount,
            double resourceUtilization
    ) {}

    /**
     * Computes all standard metrics for a set of task execution records.
     *
     * @param records list of executed task records
     * @param vmCount total number of allocated VMs
     * @return populated MetricsSummary
     */
    public static MetricsSummary calculate(List<TaskExecutionRecord> records, int vmCount) {
        if (records == null || records.isEmpty()) {
            return new MetricsSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0.0);
        }

        double makespan = calculateMakespan(records);
        double avgWait = calculateAverageWaitingTime(records);
        double avgTurnaround = calculateAverageTurnaroundTime(records);
        double throughput = calculateThroughput(records, makespan);
        int missCount = calculateDeadlineMissCount(records);
        double missRate = (double) missCount / records.size();
        double utilization = calculateTimeBasedVmResourceUtilization(records, vmCount, makespan);

        return new MetricsSummary(
                round4(makespan),
                round4(avgWait),
                round4(avgTurnaround),
                round4(throughput),
                round4(missRate),
                missCount,
                records.size(),
                round4(utilization)
        );
    }

    /**
     * Convenience method to calculate metrics directly from Baseline {@link SchedulingResult} list.
     */
    public static MetricsSummary calculateFromBaseline(List<SchedulingResult> results, int vmCount) {
        if (results == null || results.isEmpty()) {
            return new MetricsSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0.0);
        }
        List<TaskExecutionRecord> records = results.stream()
                .map(TaskExecutionRecord::fromBaseline)
                .toList();
        return calculate(records, vmCount);
    }

    /**
     * Convenience method to calculate metrics directly from Proposed {@link ProposedSchedulingResult} list.
     */
    public static MetricsSummary calculateFromProposed(List<ProposedSchedulingResult> results, int vmCount) {
        if (results == null || results.isEmpty()) {
            return new MetricsSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0.0);
        }
        List<TaskExecutionRecord> records = results.stream()
                .map(TaskExecutionRecord::fromProposed)
                .toList();
        return calculate(records, vmCount);
    }

    /**
     * Computes makespan:
     * \[
     *   \text{Makespan} = \max(\text{completionTime}) - \min(\text{arrivalTime})
     * \]
     */
    public static double calculateMakespan(List<TaskExecutionRecord> records) {
        if (records == null || records.isEmpty()) return 0.0;
        double minArrival = Double.MAX_VALUE;
        double maxCompletion = Double.MIN_VALUE;

        for (TaskExecutionRecord r : records) {
            if (r.arrivalTime() < minArrival) minArrival = r.arrivalTime();
            if (r.completionTime() > maxCompletion) maxCompletion = r.completionTime();
        }

        double span = maxCompletion - minArrival;
        return Math.max(0.0, span);
    }

    /**
     * Computes average waiting time:
     * \[
     *   \text{AvgWait} = \frac{1}{N} \sum_{i=1}^N \text{waitingTime}_i
     * \]
     */
    public static double calculateAverageWaitingTime(List<TaskExecutionRecord> records) {
        if (records == null || records.isEmpty()) return 0.0;
        double totalWait = 0.0;
        for (TaskExecutionRecord r : records) {
            totalWait += r.waitingTime();
        }
        return totalWait / records.size();
    }

    /**
     * Computes average turnaround time:
     * \[
     *   \text{AvgTurnaround} = \frac{1}{N} \sum_{i=1}^N \text{turnaroundTime}_i
     * \]
     */
    public static double calculateAverageTurnaroundTime(List<TaskExecutionRecord> records) {
        if (records == null || records.isEmpty()) return 0.0;
        double totalTurnaround = 0.0;
        for (TaskExecutionRecord r : records) {
            totalTurnaround += r.turnaroundTime();
        }
        return totalTurnaround / records.size();
    }

    /**
     * Computes throughput in tasks completed per second:
     * \[
     *   \text{Throughput} = \frac{N_{\text{completed}}}{\text{makespan}}
     * \]
     */
    public static double calculateThroughput(List<TaskExecutionRecord> records, double makespan) {
        if (records == null || records.isEmpty() || makespan <= 0.0) return 0.0;
        return records.size() / makespan;
    }

    /**
     * Counts the total number of tasks that missed their deadlines.
     */
    public static int calculateDeadlineMissCount(List<TaskExecutionRecord> records) {
        if (records == null || records.isEmpty()) return 0;
        int count = 0;
        for (TaskExecutionRecord r : records) {
            if (r.deadlineMissed()) count++;
        }
        return count;
    }

    /**
     * Primary Project Metric: <b>Time-based VM Resource Utilization</b>.
     *
     * <p>Defined as:
     * \[
     *   \text{ResourceUtilization} = \frac{\sum(\text{VM busy time})}{\text{number of VMs} \times \text{experiment makespan}} \times 100
     * \]
     *
     * <p>where \(\sum(\text{VM busy time})\) is the total time spent executing tasks across all VMs
     * (\(\sum_{i=1}^N (\text{completionTime}_i - \text{startTime}_i)\)).
     *
     * <p>Guaranteed to return a value strictly bounded in \([0.0, 100.0]\).
     *
     * @param records  task execution records
     * @param vmCount  number of VMs in the experiment
     * @param makespan total experiment makespan
     * @return time-based VM utilization percentage in [0.0, 100.0]
     */
    public static double calculateTimeBasedVmResourceUtilization(List<TaskExecutionRecord> records, int vmCount, double makespan) {
        if (records == null || records.isEmpty() || vmCount <= 0 || makespan <= 0.0) {
            return 0.0;
        }

        double totalBusyTime = 0.0;
        for (TaskExecutionRecord r : records) {
            double busy = r.completionTime() - r.startTime();
            if (busy > 0.0) {
                totalBusyTime += busy;
            }
        }

        double capacity = vmCount * makespan;
        if (capacity <= 0.0) {
            return 0.0;
        }

        double utilization = (totalBusyTime / capacity) * 100.0;
        return Math.max(0.0, Math.min(100.0, utilization));
    }

    private static double round4(double val) {
        return Math.round(val * 10000.0) / 10000.0;
    }
}
