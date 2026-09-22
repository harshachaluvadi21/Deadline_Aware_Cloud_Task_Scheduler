package scheduler.analysis;

import java.util.Locale;

/**
 * Encapsulates pairwise differences between Proposed and Baseline schedulers
 * evaluated on the exact same workload.
 *
 * <p>All differences are calculated strictly as:
 * \[
 *   \text{Difference} = \text{Proposed} - \text{Baseline}
 * \]
 * without labeling as "improvement" or "degradation".
 */
public record PairwiseComparison(
        String scenario,
        int taskCount,
        long seed,
        double baselineMakespan,
        double proposedMakespan,
        double baselineWaitingTime,
        double proposedWaitingTime,
        double baselineTurnaroundTime,
        double proposedTurnaroundTime,
        double baselineThroughput,
        double proposedThroughput,
        double baselineDeadlineMissRate,
        double proposedDeadlineMissRate,
        double baselineResourceUtilization,
        double proposedResourceUtilization,
        double makespanDifference,
        double waitingTimeDifference,
        double turnaroundDifference,
        double throughputDifference,
        double deadlineMissRateDifference,
        double resourceUtilizationDifference,
        String makespanRelativeChangePercent,
        String waitingTimeRelativeChangePercent,
        String turnaroundRelativeChangePercent,
        String throughputRelativeChangePercent,
        String deadlineMissRateRelativeChangePercent,
        String resourceUtilizationRelativeChangePercent
) {
    public static final String CSV_HEADER =
            "scenario,taskCount,baselineMakespan,proposedMakespan,baselineWaitingTime,proposedWaitingTime,baselineTurnaroundTime,proposedTurnaroundTime,baselineThroughput,proposedThroughput,baselineDeadlineMissRate,proposedDeadlineMissRate,baselineResourceUtilization,proposedResourceUtilization,makespanDifference,waitingTimeDifference,turnaroundDifference,throughputDifference,deadlineMissRateDifference,resourceUtilizationDifference,makespanRelativeChangePercent,waitingTimeRelativeChangePercent,turnaroundRelativeChangePercent,throughputRelativeChangePercent,deadlineMissRateRelativeChangePercent,resourceUtilizationRelativeChangePercent";

    public static PairwiseComparison compute(RawResultRow baseline, RawResultRow proposed) {
        if (!baseline.scenario().equals(proposed.scenario()) ||
            baseline.taskCount() != proposed.taskCount() ||
            baseline.seed() != proposed.seed()) {
            throw new IllegalArgumentException("Cannot compare mismatched workloads: " + baseline + " vs " + proposed);
        }

        double diffMakespan = proposed.makespan() - baseline.makespan();
        double diffWait = proposed.averageWaitingTime() - baseline.averageWaitingTime();
        double diffTurn = proposed.averageTurnaroundTime() - baseline.averageTurnaroundTime();
        double diffThroughput = proposed.throughput() - baseline.throughput();
        double diffDmr = proposed.deadlineMissRate() - baseline.deadlineMissRate();
        double diffUtil = proposed.resourceUtilization() - baseline.resourceUtilization();

        return new PairwiseComparison(
                baseline.scenario(),
                baseline.taskCount(),
                baseline.seed(),
                baseline.makespan(),
                proposed.makespan(),
                baseline.averageWaitingTime(),
                proposed.averageWaitingTime(),
                baseline.averageTurnaroundTime(),
                proposed.averageTurnaroundTime(),
                baseline.throughput(),
                proposed.throughput(),
                baseline.deadlineMissRate(),
                proposed.deadlineMissRate(),
                baseline.resourceUtilization(),
                proposed.resourceUtilization(),
                round4(diffMakespan),
                round4(diffWait),
                round4(diffTurn),
                round4(diffThroughput),
                round4(diffDmr),
                round4(diffUtil),
                formatRelChange(diffMakespan, baseline.makespan()),
                formatRelChange(diffWait, baseline.averageWaitingTime()),
                formatRelChange(diffTurn, baseline.averageTurnaroundTime()),
                formatRelChange(diffThroughput, baseline.throughput()),
                formatRelChange(diffDmr, baseline.deadlineMissRate()),
                formatRelChange(diffUtil, baseline.resourceUtilization())
        );
    }

    private static String formatRelChange(double diff, double baseVal) {
        if (Math.abs(baseVal) < 1e-9) {
            return "NA";
        }
        double rel = (diff / Math.abs(baseVal)) * 100.0;
        return String.format(Locale.US, "%.2f", rel);
    }

    private static double round4(double val) {
        return Math.round(val * 10000.0) / 10000.0;
    }

    public String toCsvRow() {
        return String.format(Locale.US,
                "%s,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%s,%s,%s,%s,%s,%s",
                scenario, taskCount,
                baselineMakespan, proposedMakespan,
                baselineWaitingTime, proposedWaitingTime,
                baselineTurnaroundTime, proposedTurnaroundTime,
                baselineThroughput, proposedThroughput,
                baselineDeadlineMissRate, proposedDeadlineMissRate,
                baselineResourceUtilization, proposedResourceUtilization,
                makespanDifference, waitingTimeDifference,
                turnaroundDifference, throughputDifference,
                deadlineMissRateDifference, resourceUtilizationDifference,
                makespanRelativeChangePercent, waitingTimeRelativeChangePercent,
                turnaroundRelativeChangePercent, throughputRelativeChangePercent,
                deadlineMissRateRelativeChangePercent, resourceUtilizationRelativeChangePercent
        );
    }
}
