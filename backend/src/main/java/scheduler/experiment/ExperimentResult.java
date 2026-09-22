package scheduler.experiment;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Encapsulates the complete outcome of an experimental scheduling run,
 * including summary metrics and detailed per-task execution records.
 */
public record ExperimentResult(
        String scenario,
        int taskCount,
        long seed,
        String schedulerName,
        int vmCount,
        double makespan,
        double averageWaitingTime,
        double averageTurnaroundTime,
        double throughput,
        double deadlineMissRate,
        int deadlineMissedCount,
        int completedTaskCount,
        double resourceUtilization,
        List<TaskExecutionRecord> taskRecords
) {
    public static final String SUMMARY_HEADER =
            "scenario,taskCount,seed,schedulerName,vmCount,makespan,averageWaitingTime,averageTurnaroundTime,throughput,deadlineMissRate,deadlineMissedCount,completedTaskCount,resourceUtilization";

    public ExperimentResult {
        if (taskRecords == null) {
            taskRecords = Collections.emptyList();
        } else {
            taskRecords = Collections.unmodifiableList(taskRecords);
        }
    }

    public static ExperimentResult from(
            String scenario,
            int taskCount,
            long seed,
            String schedulerName,
            int vmCount,
            MetricsCalculator.MetricsSummary summary,
            List<TaskExecutionRecord> records
    ) {
        return new ExperimentResult(
                scenario,
                taskCount,
                seed,
                schedulerName,
                vmCount,
                summary.makespan(),
                summary.averageWaitingTime(),
                summary.averageTurnaroundTime(),
                summary.throughput(),
                summary.deadlineMissRate(),
                summary.deadlineMissedCount(),
                summary.completedTaskCount(),
                summary.resourceUtilization(),
                records
        );
    }

    public String toSummaryCsvRow() {
        return String.format(Locale.US, "%s,%d,%d,%s,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%d,%d,%.4f",
                scenario,
                taskCount,
                seed,
                schedulerName,
                vmCount,
                makespan,
                averageWaitingTime,
                averageTurnaroundTime,
                throughput,
                deadlineMissRate,
                deadlineMissedCount,
                completedTaskCount,
                resourceUtilization
        );
    }
}
