package scheduler.proposed;

/**
 * Execution states for cloud tasks within the Proposed Deadline-Aware Priority Scheduler.
 */
public enum TaskState {
    /**
     * Task has arrived and is waiting in the Persistent Fibonacci Heap for an available VM.
     */
    WAITING,

    /**
     * Task is actively executing on an assigned Virtual Machine.
     */
    RUNNING,

    /**
     * Task was interrupted prior to completion due to higher dynamic-priority arrival,
     * preserving remaining execution time in the Persistent Fibonacci Heap.
     */
    PREEMPTED,

    /**
     * Task has completed all required execution time (remaining execution time == 0.0).
     */
    COMPLETED
}
