package scheduler.proposed;

/**
 * Execution modes supported by the Proposed Deadline-Aware Priority Scheduler.
 *
 * <ul>
 *   <li>{@link #NON_PREEMPTIVE}: Selected task runs until completion. Newly arrived tasks
 *       wait until VM becomes available. Number of preemptions is 0.</li>
 *   <li>{@link #PREEMPTIVE}: Running tasks may be interrupted at scheduling events (e.g. task arrival)
 *       if an admitted waiting task achieves a strictly higher dynamic priority \(P(t)\).
 *       The preempted task preserves remaining execution time and is reinserted into the
 *       persistent Fibonacci heap.</li>
 * </ul>
 */
public enum SchedulingMode {
    /**
     * Non-preemptive scheduling mode: once dispatched, task runs to completion.
     */
    NON_PREEMPTIVE,

    /**
     * Preemptive scheduling mode: running task may be preempted by higher dynamic priority task.
     */
    PREEMPTIVE
}
