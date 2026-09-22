package scheduler.experiment;

import scheduler.baseline.SchedulingResult;
import scheduler.model.Task;
import scheduler.proposed.ProposedSchedulingResult;

import java.util.Locale;

/**
 * Normalized execution record for an individual task, capturing timing, placement,
 * priority/score, and observed deadline compliance across both Baseline and Proposed schedulers.
 */
public record TaskExecutionRecord(
        long taskId,
        long assignedVmId,
        double arrivalTime,
        double startTime,
        double completionTime,
        double executionTime,
        double waitingTime,
        double turnaroundTime,
        int basePriority,
        double dynamicScore,
        double deadline,
        boolean deadlineMissed
) {
    public static final String CSV_HEADER =
            "scenario,taskCount,seed,schedulerName,taskId,assignedVmId,arrivalTime,startTime,completionTime,executionTime,waitingTime,turnaroundTime,basePriority,dynamicScore,deadline,deadlineMissed";

    public static TaskExecutionRecord fromBaseline(SchedulingResult r) {
        return new TaskExecutionRecord(
                r.taskId(),
                r.assignedVmId(),
                r.arrivalTime(),
                r.startTime(),
                r.completionTime(),
                r.executionTime(),
                r.waitingTime(),
                r.turnaroundTime(),
                r.basePriority(),
                r.dynamicPriority(),
                r.deadline(),
                r.deadlineMissed()
        );
    }

    public static TaskExecutionRecord fromProposed(ProposedSchedulingResult r) {
        return new TaskExecutionRecord(
                r.taskId(),
                r.assignedVmId(),
                r.arrivalTime(),
                r.startTime(),
                r.completionTime(),
                r.executionTime(),
                r.waitingTime(),
                r.turnaroundTime(),
                r.basePriority(),
                r.finalScore(),
                r.deadline(),
                r.deadlineMissed()
        );
    }

    public static TaskExecutionRecord fromTask(Task t, double dynamicScore) {
        double arrival = t.getArrivalTime();
        double start = t.getStartTime();
        double completion = t.getCompletionTime();
        double wait = start - arrival;
        double turnaround = completion - arrival;
        boolean missed = completion > t.getDeadline();
        return new TaskExecutionRecord(
                t.getTaskId(),
                t.getAllocatedVmId(),
                arrival,
                start,
                completion,
                t.getExecutionTime(),
                wait,
                turnaround,
                t.getPriority(),
                dynamicScore,
                t.getDeadline(),
                missed
        );
    }

    public String toCsvRow(String scenario, int taskCount, long seed, String schedulerName) {
        return String.format(Locale.US, "%s,%d,%d,%s,%d,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%.4f,%.2f,%b",
                scenario,
                taskCount,
                seed,
                schedulerName,
                taskId,
                assignedVmId,
                arrivalTime,
                startTime,
                completionTime,
                executionTime,
                waitingTime,
                turnaroundTime,
                basePriority,
                dynamicScore,
                deadline,
                deadlineMissed
        );
    }
}
