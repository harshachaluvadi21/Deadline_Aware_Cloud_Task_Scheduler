package scheduler.model;

import java.util.Objects;

/**
 * Common Task Data Model shared identically across both:
 * 1. IEEE Access 2023 Baseline Priority Scheduler (PAT + WTM + Fibonacci Heap)
 * 2. Proposed Deadline-Aware Priority Scheduler
 *
 * <p>Separation of Concerns:
 * <ul>
 *   <li>Initial task-definition properties (taskId, priority, arrivalTime, executionTime, deadline)
 *       are strictly immutable once instantiated.</li>
 *   <li>Runtime properties (status, startTime, completionTime, allocatedVmId, dynamicPriority)
 *       transition strictly through validated lifecycle methods.</li>
 *   <li>Completely decoupled from CloudSim Plus (no framework imports). Cloudlet translation
 *       is handled exclusively by adapter layers.</li>
 * </ul>
 *
 * <p>Time Semantics (Simulation Time in Seconds):
 * <ul>
 *   <li><b>arrivalTime:</b> Absolute simulation clock timestamp (seconds) when the task arrives.</li>
 *   <li><b>executionTime:</b> Nominal execution duration requirement (seconds).</li>
 *   <li><b>deadline:</b> Absolute simulation clock timestamp (seconds) by which execution must finish.</li>
 *   <li><b>startTime:</b> Absolute simulation clock timestamp (seconds) when VM execution begins.</li>
 *   <li><b>completionTime:</b> Absolute simulation clock timestamp (seconds) when VM execution finishes.</li>
 * </ul>
 */
public class Task {

    /** Default allowed minimum priority level. */
    public static final int DEFAULT_MIN_PRIORITY = 1;
    /** Default allowed maximum priority level (higher numerical value represents higher priority). */
    public static final int DEFAULT_MAX_PRIORITY = 10;

    // ==========================================
    // Immutable Task Definition Properties
    // ==========================================
    private final long taskId;
    private final int priority;
    private final double arrivalTime;
    private final double executionTime;
    private final double deadline;

    // ==========================================
    // Mutable Runtime State Properties
    // ==========================================
    private TaskStatus status;
    private double startTime;
    private double completionTime;
    private long allocatedVmId;
    private double dynamicPriority;

    /**
     * Constructs a new Task with default priority bounds [1, 10].
     *
     * @param taskId        unique non-negative task identifier
     * @param priority      base priority level between {@link #DEFAULT_MIN_PRIORITY} and {@link #DEFAULT_MAX_PRIORITY}
     * @param arrivalTime   arrival time in simulation seconds (&gt;= 0.0)
     * @param executionTime nominal execution duration in simulation seconds (&gt; 0.0)
     * @param deadline      absolute deadline timestamp in simulation seconds (&gt;= arrivalTime)
     * @throws IllegalArgumentException if any validation constraints are violated
     */
    public Task(long taskId, int priority, double arrivalTime, double executionTime, double deadline) {
        this(taskId, priority, arrivalTime, executionTime, deadline, DEFAULT_MIN_PRIORITY, DEFAULT_MAX_PRIORITY);
    }

    /**
     * Constructs a new Task with configurable priority bounds.
     *
     * @param taskId        unique non-negative task identifier
     * @param priority      base priority level within [minPriority, maxPriority]
     * @param arrivalTime   arrival time in simulation seconds (&gt;= 0.0)
     * @param executionTime nominal execution duration in simulation seconds (&gt; 0.0)
     * @param deadline      absolute deadline timestamp in simulation seconds (&gt;= arrivalTime)
     * @param minPriority   minimum valid priority value
     * @param maxPriority   maximum valid priority value
     * @throws IllegalArgumentException if any validation constraints are violated
     */
    public Task(long taskId, int priority, double arrivalTime, double executionTime, double deadline,
                int minPriority, int maxPriority) {

        if (taskId < 0) {
            throw new IllegalArgumentException("Task ID must be non-negative, got: " + taskId);
        }
        if (minPriority > maxPriority) {
            throw new IllegalArgumentException("minPriority (" + minPriority + ") cannot exceed maxPriority (" + maxPriority + ")");
        }
        if (priority < minPriority || priority > maxPriority) {
            throw new IllegalArgumentException(String.format(
                "Priority %d out of bounds [%d, %d] for task %d", priority, minPriority, maxPriority, taskId
            ));
        }
        if (!Double.isFinite(arrivalTime) || arrivalTime < 0.0) {
            throw new IllegalArgumentException("Arrival time must be finite and >= 0.0, got: " + arrivalTime);
        }
        if (!Double.isFinite(executionTime) || executionTime <= 0.0) {
            throw new IllegalArgumentException("Execution time must be finite and > 0.0, got: " + executionTime);
        }
        if (!Double.isFinite(deadline) || deadline < arrivalTime) {
            throw new IllegalArgumentException(String.format(
                "Deadline (%f) must be finite and >= arrival time (%f) for task %d", deadline, arrivalTime, taskId
            ));
        }

        this.taskId = taskId;
        this.priority = priority;
        this.arrivalTime = arrivalTime;
        this.executionTime = executionTime;
        this.deadline = deadline;

        // Initialize runtime defaults
        this.status = TaskStatus.SUBMITTED;
        this.startTime = -1.0;
        this.completionTime = -1.0;
        this.allocatedVmId = -1;
        this.dynamicPriority = 0.0;
    }

    // ==========================================
    // State Transition Methods (Lifecycle State Machine)
    // ==========================================

    /**
     * Transitions the task from {@link TaskStatus#SUBMITTED} to {@link TaskStatus#READY}.
     * Marks the task as placed in the scheduling queue awaiting VM assignment.
     *
     * @throws IllegalStateException if transition from current status is invalid
     */
    public void markReady() {
        validateTransition(TaskStatus.READY);
        this.status = TaskStatus.READY;
    }

    /**
     * Transitions the task from {@link TaskStatus#READY} to {@link TaskStatus#RUNNING}.
     * Assigns the task to a specific Virtual Machine and records the execution start time.
     *
     * @param startTime simulation timestamp when execution starts on the VM
     * @param vmId      identifier of the allocated VM (&gt;= 0)
     * @throws IllegalStateException    if transition from current status is invalid
     * @throws IllegalArgumentException if startTime &lt; arrivalTime or vmId &lt; 0
     */
    public void markRunning(double startTime, long vmId) {
        validateTransition(TaskStatus.RUNNING);

        if (!Double.isFinite(startTime) || startTime < this.arrivalTime) {
            throw new IllegalArgumentException(String.format(
                "Start time (%f) cannot be before arrival time (%f) for task %d", startTime, this.arrivalTime, this.taskId
            ));
        }
        if (vmId < 0) {
            throw new IllegalArgumentException("Allocated VM ID must be non-negative, got: " + vmId);
        }

        this.startTime = startTime;
        this.allocatedVmId = vmId;
        this.status = TaskStatus.RUNNING;
    }

    /**
     * Transitions the task from {@link TaskStatus#RUNNING} to {@link TaskStatus#COMPLETED}.
     * Enforces that the task finished at or prior to its deadline (completionTime &lt;= deadline).
     *
     * @param completionTime simulation timestamp when execution finished
     * @throws IllegalStateException    if transition from current status is invalid, or if completionTime &gt; deadline
     * @throws IllegalArgumentException if completionTime &lt; startTime
     */
    public void markCompleted(double completionTime) {
        validateTransition(TaskStatus.COMPLETED);

        if (!Double.isFinite(completionTime) || completionTime < this.startTime) {
            throw new IllegalArgumentException(String.format(
                "Completion time (%f) cannot be before start time (%f) for task %d",
                completionTime, this.startTime, this.taskId
            ));
        }
        if (completionTime > this.deadline) {
            throw new IllegalStateException(String.format(
                "Cannot mark task %d as COMPLETED: completion time (%f) strictly exceeds deadline (%f). Use markDeadlineMissed instead.",
                this.taskId, completionTime, this.deadline
            ));
        }

        this.completionTime = completionTime;
        this.status = TaskStatus.COMPLETED;
    }

    /**
     * Transitions the task from {@link TaskStatus#RUNNING} to {@link TaskStatus#MISSED_DEADLINE}.
     * Enforces that the task finished strictly after its deadline expired (completionTime &gt; deadline).
     *
     * @param completionTime simulation timestamp when execution finished
     * @throws IllegalStateException    if transition from current status is invalid, or if completionTime &lt;= deadline
     * @throws IllegalArgumentException if completionTime &lt; startTime
     */
    public void markDeadlineMissed(double completionTime) {
        validateTransition(TaskStatus.MISSED_DEADLINE);

        if (!Double.isFinite(completionTime) || completionTime < this.startTime) {
            throw new IllegalArgumentException(String.format(
                "Completion time (%f) cannot be before start time (%f) for task %d",
                completionTime, this.startTime, this.taskId
            ));
        }
        if (completionTime <= this.deadline) {
            throw new IllegalStateException(String.format(
                "Cannot mark task %d as MISSED_DEADLINE: completion time (%f) is at or before deadline (%f). Use markCompleted instead.",
                this.taskId, completionTime, this.deadline
            ));
        }

        this.completionTime = completionTime;
        this.status = TaskStatus.MISSED_DEADLINE;
    }

    /**
     * Convenience method to complete execution, automatically evaluating deadline compliance:
     * - If {@code completionTime <= deadline}, transitions to {@link TaskStatus#COMPLETED}.
     * - If {@code completionTime > deadline}, transitions to {@link TaskStatus#MISSED_DEADLINE}.
     *
     * @param completionTime simulation timestamp when execution finished
     */
    public void markFinished(double completionTime) {
        if (!Double.isFinite(completionTime) || completionTime < this.startTime) {
            throw new IllegalArgumentException(String.format(
                "Completion time (%f) cannot be before start time (%f) for task %d",
                completionTime, this.startTime, this.taskId
            ));
        }
        if (completionTime > this.deadline) {
            markDeadlineMissed(completionTime);
        } else {
            markCompleted(completionTime);
        }
    }

    private void validateTransition(TaskStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw new IllegalStateException(String.format(
                "Illegal state transition for task %d: cannot transition from %s to %s",
                this.taskId, this.status, target
            ));
        }
    }

    /**
     * Resets runtime state back to initial SUBMITTED status with unassigned VM and timestamps,
     * allowing subsequent execution lifecycle recording (e.g., from CloudSim Plus results).
     */
    public void resetRuntimeState() {
        this.status = TaskStatus.SUBMITTED;
        this.startTime = -1.0;
        this.completionTime = -1.0;
        this.allocatedVmId = -1L;
    }

    // ==========================================
    // Dynamic Priority (Scheduler Scoring)
    // ==========================================

    /**
     * Updates the dynamic priority score calculated by either the baseline PAT heuristic
     * or the proposed deadline-aware priority calculator.
     *
     * @param dynamicPriority numeric priority score
     * @throws IllegalArgumentException if dynamicPriority is not finite
     */
    public void setDynamicPriority(double dynamicPriority) {
        if (!Double.isFinite(dynamicPriority)) {
            throw new IllegalArgumentException("Dynamic priority must be finite, got: " + dynamicPriority);
        }
        this.dynamicPriority = dynamicPriority;
    }

    // ==========================================
    // Derived Metrics & Timing Queries
    // ==========================================

    /**
     * Calculates the queuing delay / waiting time before execution began:
     * {@code W = startTime - arrivalTime}.
     *
     * @return waiting time in seconds, or -1.0 if the task has not started running yet
     */
    public double getWaitingTime() {
        if (startTime < 0.0) {
            return -1.0;
        }
        return startTime - arrivalTime;
    }

    /**
     * Calculates the total turnaround time:
     * {@code T = completionTime - arrivalTime}.
     *
     * @return turnaround time in seconds, or -1.0 if the task has not finished yet
     */
    public double getTurnaroundTime() {
        if (completionTime < 0.0) {
            return -1.0;
        }
        return completionTime - arrivalTime;
    }

    /**
     * Evaluates whether the task missed its deadline:
     * True if status is {@link TaskStatus#MISSED_DEADLINE}, or completionTime &gt; deadline.
     *
     * @return true if deadline was strictly missed, false otherwise
     */
    public boolean isDeadlineMissed() {
        return this.status == TaskStatus.MISSED_DEADLINE || (completionTime >= 0.0 && completionTime > deadline);
    }

    // ==========================================
    // Read-Only Getters
    // ==========================================

    public long getTaskId() {
        return taskId;
    }

    public int getPriority() {
        return priority;
    }

    public double getArrivalTime() {
        return arrivalTime;
    }

    public double getExecutionTime() {
        return executionTime;
    }

    public double getDeadline() {
        return deadline;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getCompletionTime() {
        return completionTime;
    }

    public long getAllocatedVmId() {
        return allocatedVmId;
    }

    public double getDynamicPriority() {
        return dynamicPriority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return taskId == task.taskId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }

    @Override
    public String toString() {
        return String.format(
            "Task{id=%d, prio=%d, arr=%.2f, exec=%.2f, dline=%.2f, status=%s, vm=%d, dynPrio=%.4f}",
            taskId, priority, arrivalTime, executionTime, deadline, status, allocatedVmId, dynamicPriority
        );
    }
}
