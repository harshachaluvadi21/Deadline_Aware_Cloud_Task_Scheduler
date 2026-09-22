package web.dto;

import scheduler.experiment.MetricsCalculator;

/**
 * Data Transfer Object representing performance metrics computed for a simulation run.
 */
public record MetricsDto(
        double makespan,
        double averageWaitingTime,
        double averageTurnaroundTime,
        double throughput,
        double deadlineMissRate,
        int deadlineMissedCount,
        int completedTaskCount,
        double resourceUtilization
) {
    public static MetricsDto from(MetricsCalculator.MetricsSummary s) {
        return new MetricsDto(
                s.makespan(),
                s.averageWaitingTime(),
                s.averageTurnaroundTime(),
                s.throughput(),
                s.deadlineMissRate(),
                s.deadlineMissedCount(),
                s.completedTaskCount(),
                s.resourceUtilization()
        );
    }
}
