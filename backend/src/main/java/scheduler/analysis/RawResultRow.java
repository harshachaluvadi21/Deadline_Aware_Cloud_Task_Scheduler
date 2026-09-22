package scheduler.analysis;

/**
 * Immutable representation of a single scheduler run's summary metrics
 * loaded directly from raw_experiment_results.csv.
 */
public record RawResultRow(
        String scenario,
        int taskCount,
        long seed,
        String scheduler,
        String workloadFile,
        double makespan,
        double averageWaitingTime,
        double averageTurnaroundTime,
        double throughput,
        int deadlineMissedCount,
        double deadlineMissRate,
        int completedTaskCount,
        int totalTaskCount,
        double resourceUtilization
) {}
