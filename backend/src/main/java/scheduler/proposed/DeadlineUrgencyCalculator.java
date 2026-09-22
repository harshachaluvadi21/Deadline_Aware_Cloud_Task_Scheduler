package scheduler.proposed;

import scheduler.model.Task;

/**
 * Calculates remaining slack time and normalized deadline urgency for tasks at a given simulation timestamp.
 *
 * <p><b>[Project Design Decision]</b>
 * The calculation distinguishes three critical timing states:
 * <ul>
 *   <li><b>slack &le; 0:</b> Critical / Infeasible to meet if started now (\(currentTime + executionTime \ge deadline\)).
 *       Assigned maximum urgency (1.0).</li>
 *   <li><b>currentTime &ge; deadline:</b> Overdue (deadline has elapsed before execution began).
 *       Assigned maximum urgency (1.0).</li>
 *   <li><b>completionTime &gt; deadline:</b> Deadline Missed (strictly a post-execution outcome).</li>
 * </ul>
 *
 * <p><b>Urgency Function:</b>
 * \[
 *   U_{\text{deadline}}(t) = \begin{cases}
 *     1.0 & \text{if } \text{slack}(t) \le 0 \\
 *     \exp\left(-\frac{\text{slack}(t)}{k \cdot \text{executionTime}}\right) & \text{if } \text{slack}(t) > 0
 *   \end{cases}
 * \]
 * where scaling parameter \(k = 2.0\) is an <b>initial project design parameter</b>
 * (initial design choice controlling urgency decay), not a calibrated or optimal value.
 */
public class DeadlineUrgencyCalculator {

    /** Default initial project design parameter controlling urgency decay. */
    public static final double DEFAULT_DECAY_PARAMETER_K = 2.0;

    private final double decayParameterK;

    /**
     * Constructs a calculator with the default initial design decay parameter (k = 2.0).
     */
    public DeadlineUrgencyCalculator() {
        this(DEFAULT_DECAY_PARAMETER_K);
    }

    /**
     * Constructs a calculator with a custom decay parameter k.
     *
     * @param decayParameterK positive decay scaling factor
     * @throws IllegalArgumentException if decayParameterK <= 0 or non-finite
     */
    public DeadlineUrgencyCalculator(double decayParameterK) {
        if (!Double.isFinite(decayParameterK) || decayParameterK <= 0.0) {
            throw new IllegalArgumentException("Decay parameter k must be finite and positive, got: " + decayParameterK);
        }
        this.decayParameterK = decayParameterK;
    }

    /**
     * Calculates remaining slack time for a task at simulation time t:
     * \[
     *   \text{slack}(t) = \text{deadline} - t - \text{executionTime}
     * \]
     *
     * @param task        the task under evaluation
     * @param currentTime current simulation timestamp
     * @return remaining slack in simulation seconds
     */
    public double calculateSlack(Task task, double currentTime) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (!Double.isFinite(currentTime) || currentTime < 0.0) {
            throw new IllegalArgumentException("Current time must be finite and non-negative, got: " + currentTime);
        }
        return task.getDeadline() - currentTime - task.getExecutionTime();
    }

    /**
     * Calculates the normalized deadline urgency score in [0.0, 1.0].
     *
     * @param task        the task under evaluation
     * @param currentTime current simulation timestamp
     * @return urgency score in [0.0, 1.0] (higher value = greater urgency)
     */
    public double calculateUrgency(Task task, double currentTime) {
        double slack = calculateSlack(task, currentTime);
        return calculateUrgencyFromSlack(slack, task.getExecutionTime());
    }

    /**
     * Computes urgency directly from slack and execution time.
     *
     * @param slack         remaining slack time
     * @param executionTime task execution duration
     * @return urgency score in [0.0, 1.0]
     */
    public double calculateUrgencyFromSlack(double slack, double executionTime) {
        if (slack <= 0.0) {
            // Critical or infeasible if started now
            return 1.0;
        }
        if (executionTime <= 0.0) {
            return 1.0;
        }

        double denominator = this.decayParameterK * executionTime;
        double exponent = -slack / denominator;

        // Bounded exponential decay
        double urgency = Math.exp(exponent);
        return Math.max(0.0, Math.min(1.0, urgency));
    }

    /**
     * Checks whether the task has entered the critical/infeasible state at time t (slack <= 0).
     *
     * @param task        the task under evaluation
     * @param currentTime current simulation timestamp
     * @return true if slack <= 0
     */
    public boolean isCritical(Task task, double currentTime) {
        return calculateSlack(task, currentTime) <= 0.0;
    }

    /**
     * Checks whether the task is overdue at time t (currentTime >= deadline).
     *
     * @param task        the task under evaluation
     * @param currentTime current simulation timestamp
     * @return true if currentTime >= task.getDeadline()
     */
    public boolean isOverdue(Task task, double currentTime) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        return currentTime >= task.getDeadline();
    }

    public double getDecayParameterK() {
        return decayParameterK;
    }
}
