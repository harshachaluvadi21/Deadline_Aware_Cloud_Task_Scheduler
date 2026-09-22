package web.dto;

/**
 * Data Transfer Object representing pairwise differences (Proposed - Baseline)
 * without subjective superiority labels.
 */
public record PairwiseComparisonDto(
        double makespanDifference,
        double waitingTimeDifference,
        double turnaroundDifference,
        double throughputDifference,
        double deadlineMissRateDifference,
        double resourceUtilizationDifference
) {
    public static PairwiseComparisonDto of(MetricsDto baseline, MetricsDto proposed) {
        return new PairwiseComparisonDto(
                round4(proposed.makespan() - baseline.makespan()),
                round4(proposed.averageWaitingTime() - baseline.averageWaitingTime()),
                round4(proposed.averageTurnaroundTime() - baseline.averageTurnaroundTime()),
                round4(proposed.throughput() - baseline.throughput()),
                round4(proposed.deadlineMissRate() - baseline.deadlineMissRate()),
                round4(proposed.resourceUtilization() - baseline.resourceUtilization())
        );
    }

    private static double round4(double val) {
        return Math.round(val * 10000.0) / 10000.0;
    }
}
