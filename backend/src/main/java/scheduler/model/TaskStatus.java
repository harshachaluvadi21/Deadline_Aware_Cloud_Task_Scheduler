package scheduler.model;

/**
 * Represents the execution lifecycle states of a {@link Task}.
 *
 * <p>Valid State Lifecycle Transitions:
 * <pre>
 *   SUBMITTED ──► READY ──► RUNNING ──┬──► COMPLETED (completionTime &lt;= deadline)
 *                                     └──► MISSED_DEADLINE (completionTime &gt; deadline)
 * </pre>
 *
 * Arbitrary transitions (e.g., from SUBMITTED directly to RUNNING, or from a terminal state) are prohibited.
 */
public enum TaskStatus {
    /**
     * Initial state when a task is instantiated and submitted to the scheduling system.
     */
    SUBMITTED,

    /**
     * Task is admitted into the scheduling queue (e.g. Fibonacci Heap) waiting for VM allocation.
     */
    READY,

    /**
     * Task has been dispatched and is actively executing on an assigned Virtual Machine.
     */
    RUNNING,

    /**
     * Task completed execution at or prior to its deadline (completionTime &lt;= deadline).
     */
    COMPLETED,

    /**
     * Task completed execution strictly after its deadline expired (completionTime &gt; deadline).
     */
    MISSED_DEADLINE;

    /**
     * Checks whether a transition from this state to the target state is valid according to the lifecycle.
     *
     * @param target the target state
     * @return true if the transition is permitted, false otherwise
     */
    public boolean canTransitionTo(TaskStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case SUBMITTED -> target == READY;
            case READY -> target == RUNNING;
            case RUNNING -> target == COMPLETED || target == MISSED_DEADLINE;
            case COMPLETED, MISSED_DEADLINE -> false; // Terminal states
        };
    }

    /**
     * Checks if this status represents a finished/terminal execution state.
     *
     * @return true if COMPLETED or MISSED_DEADLINE, false otherwise
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == MISSED_DEADLINE;
    }
}
