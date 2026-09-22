package scheduler.proposed;

import scheduler.model.Task;

/**
 * Normalizes task base priority levels into the continuous domain [0.1, 1.0].
 *
 * <p><b>[Project Design Decision]</b>
 * Preserves initial task importance defined by users or client applications.
 * Uses linear normalization:
 * \[
 *   P_{\text{norm}} = \frac{\text{priority}}{\text{maxPriority}}
 * \]
 * For default priority bounds [1, 10], priority 1 maps to 0.1 and priority 10 maps to 1.0.
 */
public final class PriorityNormalization {

    public static final int DEFAULT_MAX_PRIORITY = Task.DEFAULT_MAX_PRIORITY;

    private PriorityNormalization() {
        // Utility class
    }

    /**
     * Normalizes a base priority using the default maximum priority bound (10).
     *
     * @param priority the integer base priority in [1, 10]
     * @return normalized priority in [0.1, 1.0]
     * @throws IllegalArgumentException if priority is not positive or exceeds default max
     */
    public static double normalize(int priority) {
        return normalize(priority, DEFAULT_MAX_PRIORITY);
    }

    /**
     * Normalizes a base priority using a custom maximum priority bound.
     *
     * @param priority    the integer base priority (&gt; 0)
     * @param maxPriority the maximum possible priority (&gt;= priority)
     * @return normalized priority in (0.0, 1.0]
     * @throws IllegalArgumentException if bounds are invalid
     */
    public static double normalize(int priority, int maxPriority) {
        if (maxPriority <= 0) {
            throw new IllegalArgumentException("Maximum priority must be positive, got: " + maxPriority);
        }
        if (priority <= 0) {
            throw new IllegalArgumentException("Priority must be positive, got: " + priority);
        }
        if (priority > maxPriority) {
            throw new IllegalArgumentException(String.format(
                "Priority (%d) cannot exceed maxPriority (%d)", priority, maxPriority
            ));
        }

        return (double) priority / (double) maxPriority;
    }
}
