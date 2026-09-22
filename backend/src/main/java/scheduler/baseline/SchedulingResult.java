package scheduler.baseline;

import scheduler.model.Task;

/**
 * Immutable representation of a completed task's scheduling outcome and observed metrics.
 *
 * <p><b>Traceability:</b> [Implementation assumption / Project Evaluation Framework]
 * Contains comprehensive execution timestamps and metrics for downstream performance analysis.
 *
 * <p><b>CRITICAL BASELINE CONSTRAINT:</b>
 * The {@code deadlineMissed} field is strictly an <em>observed measurement result</em>,
 * evaluated post-execution (\(completionTime > deadline\)).
 * Deadline has zero influence on baseline priority calculation, queue extraction, or VM allocation.
 */
public record SchedulingResult(
    long taskId,
    long assignedVmId,
    double arrivalTime,
    double startTime,
    double completionTime,
    double executionTime,
    double waitingTime,
    double turnaroundTime,
    int basePriority,
    double dynamicPriority,
    double deadline,
    boolean deadlineMissed
) {
    /**
     * Compact constructor validating finite timestamps.
     */
    public SchedulingResult {
        if (taskId < 0) {
            throw new IllegalArgumentException("Task ID must be non-negative: " + taskId);
        }
        if (assignedVmId < 0) {
            throw new IllegalArgumentException("Assigned VM ID must be non-negative: " + assignedVmId);
        }
        if (!Double.isFinite(arrivalTime) || arrivalTime < 0.0) {
            throw new IllegalArgumentException("Arrival time must be finite and non-negative: " + arrivalTime);
        }
        if (!Double.isFinite(startTime) || startTime < arrivalTime) {
            throw new IllegalArgumentException("Start time cannot be before arrival time");
        }
        if (!Double.isFinite(completionTime) || completionTime < startTime) {
            throw new IllegalArgumentException("Completion time cannot be before start time");
        }
        if (!Double.isFinite(executionTime) || executionTime < 0.0) {
            throw new IllegalArgumentException("Execution time must be non-negative: " + executionTime);
        }
        if (!Double.isFinite(waitingTime) || waitingTime < 0.0) {
            throw new IllegalArgumentException("Waiting time must be non-negative: " + waitingTime);
        }
        if (!Double.isFinite(turnaroundTime) || turnaroundTime < waitingTime) {
            throw new IllegalArgumentException("Turnaround time cannot be less than waiting time");
        }
    }

    /**
     * Constructs a SchedulingResult directly from a finished {@link Task} instance.
     *
     * @param task the completed or deadline-missed Task
     * @return populated SchedulingResult
     * @throws IllegalArgumentException if the task has not finished execution
     */
    public static SchedulingResult fromTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (task.getStartTime() < 0.0 || task.getCompletionTime() < 0.0 || task.getAllocatedVmId() < 0) {
            throw new IllegalArgumentException("Task has not completed execution: " + task);
        }

        double arrivalTime = task.getArrivalTime();
        double startTime = task.getStartTime();
        double completionTime = task.getCompletionTime();
        double waitingTime = startTime - arrivalTime;
        double turnaroundTime = completionTime - arrivalTime;
        boolean deadlineMissed = completionTime > task.getDeadline();

        return new SchedulingResult(
            task.getTaskId(),
            task.getAllocatedVmId(),
            arrivalTime,
            startTime,
            completionTime,
            task.getExecutionTime(),
            waitingTime,
            turnaroundTime,
            task.getPriority(),
            task.getDynamicPriority(),
            task.getDeadline(),
            deadlineMissed
        );
    }
}
