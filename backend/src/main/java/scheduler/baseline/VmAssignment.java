package scheduler.baseline;

/**
 * Encapsulates the assignment of a Task to a specific Virtual Machine (VM).
 *
 * <p>Traceability: [Implementation assumption / Architectural container]
 * Captures the mapping between tasks and processing servers (VMs) without embedding
 * any deadline awareness.
 */
public record VmAssignment(
    long taskId,
    long vmId,
    double assignedTime,
    double startTime,
    double completionTime,
    double executionDuration
) {
    /**
     * Compact constructor validating timing constraints.
     */
    public VmAssignment {
        if (taskId < 0) {
            throw new IllegalArgumentException("Task ID must be non-negative: " + taskId);
        }
        if (vmId < 0) {
            throw new IllegalArgumentException("VM ID must be non-negative: " + vmId);
        }
        if (!Double.isFinite(assignedTime) || assignedTime < 0.0) {
            throw new IllegalArgumentException("Assigned time must be finite and non-negative: " + assignedTime);
        }
        if (!Double.isFinite(startTime) || startTime < assignedTime) {
            throw new IllegalArgumentException(String.format(
                "Start time (%f) cannot be earlier than assigned time (%f)", startTime, assignedTime
            ));
        }
        if (!Double.isFinite(completionTime) || completionTime < startTime) {
            throw new IllegalArgumentException(String.format(
                "Completion time (%f) cannot be earlier than start time (%f)", completionTime, startTime
            ));
        }
        if (!Double.isFinite(executionDuration) || executionDuration < 0.0) {
            throw new IllegalArgumentException("Execution duration must be non-negative: " + executionDuration);
        }
    }

    /**
     * Convenience factory method.
     */
    public static VmAssignment of(long taskId, long vmId, double assignedTime, double startTime, double completionTime) {
        return new VmAssignment(taskId, vmId, assignedTime, startTime, completionTime, completionTime - startTime);
    }
}
