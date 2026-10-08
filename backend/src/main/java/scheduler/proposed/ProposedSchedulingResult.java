package scheduler.proposed;

import scheduler.model.Task;

/**
 * Immutable representation of a task's scheduling outcome and observed metrics
 * produced by the Proposed Deadline-Aware Priority Scheduler.
 *
 * <p><b>[Project Design Decision]</b>
 * Captures comprehensive timing, execution parameters, calculated score components,
 * and preemption counts for downstream metric calculation (Makespan, Waiting Time,
 * Turnaround, DMR, Utilization, and Preemption Count).
 */
public record ProposedSchedulingResult(
    long taskId,
    long assignedVmId,
    double arrivalTime,
    double startTime,
    double completionTime,
    double executionTime,
    double waitingTime,
    double turnaroundTime,
    int basePriority,
    double finalScore,
    double deadlineUrgency,
    double deadline,
    boolean deadlineMissed,
    int preemptionCount
) {
    /**
     * Compact constructor validating constraints.
     */
    public ProposedSchedulingResult {
        if (taskId < 0) throw new IllegalArgumentException("Task ID must be non-negative: " + taskId);
        if (assignedVmId < 0) throw new IllegalArgumentException("Assigned VM ID must be non-negative: " + assignedVmId);
        if (!Double.isFinite(arrivalTime) || arrivalTime < 0.0) throw new IllegalArgumentException("Arrival time must be >= 0");
        if (!Double.isFinite(startTime) || startTime < arrivalTime) throw new IllegalArgumentException("Start time cannot be before arrival time");
        if (!Double.isFinite(completionTime) || completionTime < startTime) throw new IllegalArgumentException("Completion time cannot be before start time");
        if (!Double.isFinite(executionTime) || executionTime < 0.0) throw new IllegalArgumentException("Execution time must be >= 0");
        if (!Double.isFinite(waitingTime) || waitingTime < 0.0) throw new IllegalArgumentException("Waiting time must be >= 0");
        if (!Double.isFinite(turnaroundTime) || turnaroundTime < waitingTime) throw new IllegalArgumentException("Turnaround time cannot be < waiting time");
        if (preemptionCount < 0) throw new IllegalArgumentException("Preemption count cannot be negative: " + preemptionCount);
    }

    /**
     * Backward-compatible 13-parameter constructor defaulting preemption count to 0.
     */
    public ProposedSchedulingResult(
            long taskId,
            long assignedVmId,
            double arrivalTime,
            double startTime,
            double completionTime,
            double executionTime,
            double waitingTime,
            double turnaroundTime,
            int basePriority,
            double finalScore,
            double deadlineUrgency,
            double deadline,
            boolean deadlineMissed) {
        this(taskId, assignedVmId, arrivalTime, startTime, completionTime, executionTime,
             waitingTime, turnaroundTime, basePriority, finalScore, deadlineUrgency, deadline, deadlineMissed, 0);
    }

    /**
     * Constructs a ProposedSchedulingResult from a finished task and its recorded score components,
     * defaulting preemptionCount to 0.
     *
     * @param task            the finished Task
     * @param finalScore      the dynamic deadline-aware score calculated at dispatch
     * @param deadlineUrgency the deadline urgency component calculated at dispatch
     * @return populated ProposedSchedulingResult
     */
    public static ProposedSchedulingResult fromTask(Task task, double finalScore, double deadlineUrgency) {
        return fromTask(task, finalScore, deadlineUrgency, 0);
    }

    /**
     * Constructs a ProposedSchedulingResult from a finished task, recorded score components,
     * and actual preemption count.
     *
     * @param task            the finished Task
     * @param finalScore      the dynamic deadline-aware score calculated at dispatch
     * @param deadlineUrgency the deadline urgency component calculated at dispatch
     * @param preemptionCount the number of times this task was preempted
     * @return populated ProposedSchedulingResult
     */
    public static ProposedSchedulingResult fromTask(Task task, double finalScore, double deadlineUrgency, int preemptionCount) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (task.getStartTime() < 0.0 || task.getCompletionTime() < 0.0 || task.getAllocatedVmId() < 0) {
            throw new IllegalArgumentException("Task has not finished execution: " + task);
        }

        double arrival = task.getArrivalTime();
        double start = task.getStartTime();
        double completion = task.getCompletionTime();
        double waiting = start - arrival;
        double turnaround = completion - arrival;
        boolean deadlineMissed = completion > task.getDeadline();

        return new ProposedSchedulingResult(
            task.getTaskId(),
            task.getAllocatedVmId(),
            arrival,
            start,
            completion,
            task.getExecutionTime(),
            waiting,
            turnaround,
            task.getPriority(),
            finalScore,
            deadlineUrgency,
            task.getDeadline(),
            deadlineMissed,
            preemptionCount
        );
    }
}
