package scheduler.proposed;

import scheduler.model.Task;

/**
 * Implementation of the Dynamic Deadline Urgency Algorithm
 * for the proposed deadline-aware cloud task scheduling method.
 *
 * <p><b>Algorithm Name:</b>
 * Dynamic Deadline Urgency Algorithm
 *
 * <p><b>Purpose:</b>
 * Dynamically calculates the deadline urgency \(\Omega(t)\) of a waiting
 * task using its remaining deadline, execution/burst time, and slack.
 *
 * <p><b>Algorithmic Traceability:</b>
 * <ul>
 *   <li><b>Input:</b> Task \(T_i\), current simulation time \(t\), task deadline,
 *       execution/burst time \(B\), and arrival time.</li>
 *
 *   <li><b>Step 1 - Remaining Deadline:</b>
 *       \(D(t) = \text{deadline} - t\).</li>
 *
 *   <li><b>Step 2 - Burst Time:</b>
 *       \(B = \text{executionTime}\).</li>
 *
 *   <li><b>Step 3 - Slack:</b>
 *       \(\text{Slack}(t) = D(t) - B\).</li>
 *
 *   <li><b>Step 4 - Effective Slack:</b>
 *       \(\text{EffectiveSlack}(t) = \max(0.0, \, \text{Slack}(t))\).</li>
 *
 *   <li><b>Step 5 - Effective Deadline:</b>
 *       \(D_{\text{effective}}(t) = \max(0.0, \, D(t))\).</li>
 *
 *   <li><b>Step 6 - Calculate Deadline Urgency:</b>
 *       \[
 *         \Omega(t) = \left[ \frac{1000}{\text{EffectiveSlack}(t) + 1} \right] \times \left[ 1 + \frac{B}{D_{\text{effective}}(t) + 1} \right]
 *       \]
 *   </li>
 *
 *   <li><b>Step 7 - Return Urgency:</b>
 *       Return dynamic deadline urgency \(\Omega(t)\).</li>
 *
 *   <li><b>Output:</b> Dynamic deadline urgency \(\Omega(t)\).</li>
 * </ul>
 *
 * <p><b>Algorithm Specification:</b>
 * <pre>
 * Algorithm: Dynamic Deadline Urgency Algorithm
 * Input:
 *   - Task Ti
 *   - Current simulation time t
 *   - Deadline
 *   - Execution/Burst time B
 * Steps:
 *   1. Calculate remaining deadline: D(t) = deadline - t
 *   2. Calculate burst time: B = executionTime
 *   3. Calculate slack: Slack(t) = D(t) - B
 *   4. Clamp slack: EffectiveSlack(t) = max(0, Slack(t))
 *   5. Clamp remaining deadline: D_effective(t) = max(0, D(t))
 *   6. Calculate burst-aware deadline urgency:
 *        Omega(t) = [1000 / (EffectiveSlack(t) + 1)] * [1 + B / (D_effective(t) + 1)]
 *   7. Return Omega(t)
 * Output:
 *   - Dynamic deadline urgency Omega(t)
 * </pre>
 *
 * <p><b>Dynamic Behavior:</b>
 * \(\Omega(t)\) is recalculated whenever the simulation time changes.
 * Therefore, as the remaining deadline and slack decrease,
 * the urgency dynamically increases.
 *
 * <p><b>Numerical Safety:</b>
 * \(D_{\text{effective}}(t) = \max(0.0, D(t))\) prevents division by zero
 * and negative denominators for overdue tasks, ensuring the burst factor
 * denominator is always \(\ge 1.0\).
 *
 * <p><b>Role in Proposed Algorithm:</b>
 * This is Component 1 of the proposed scheduling algorithm.
 * Its output \(\Omega(t)\) is passed to the Dynamic Priority Algorithm.
 */
public class DeadlineUrgencyCalculator {

    /**
     * Constructs a deadline urgency calculator.
     */
    public DeadlineUrgencyCalculator() {
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
        return calculateSlack(task, currentTime, task.getExecutionTime());
    }

    /**
     * Calculates remaining slack time using a specified remaining execution duration:
     * \[
     *   \text{slack}(t) = \text{deadline} - t - \text{remainingExecutionTime}
     * \]
     *
     * @param task                   the task under evaluation
     * @param currentTime            current simulation timestamp
     * @param remainingExecutionTime remaining execution duration in simulation seconds
     * @return remaining slack in simulation seconds
     */
    public double calculateSlack(Task task, double currentTime, double remainingExecutionTime) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (!Double.isFinite(currentTime) || currentTime < 0.0) {
            throw new IllegalArgumentException("Current time must be finite and non-negative, got: " + currentTime);
        }
        if (!Double.isFinite(remainingExecutionTime) || remainingExecutionTime < 0.0) {
            throw new IllegalArgumentException("Remaining execution time must be finite and non-negative, got: " + remainingExecutionTime);
        }
        return task.getDeadline() - currentTime - remainingExecutionTime;
    }

    // ============================================================
    // DYNAMIC DEADLINE URGENCY ALGORITHM
    // ============================================================
    /**
     * Calculates the time-dependent burst-aware deadline urgency \(\Omega(t)\):
     * \[
     *   \Omega(t) = \left[ \frac{1000}{\text{EffectiveSlack}(t) + 1} \right] \times \left[ 1 + \frac{B}{D_{\text{effective}}(t) + 1} \right]
     * \]
     *
     * <p>This is the core deadline-specific algorithm, dynamically evaluated
     * at the current simulation timestamp using the task's nominal execution time.
     *
     * @param task        the task under evaluation
     * @param currentTime current simulation timestamp
     * @return dynamic urgency score \(\Omega(t) \ge 0.0\)
     */
    public double calculateUrgency(Task task, double currentTime) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        return calculateUrgency(task, currentTime, task.getExecutionTime());
    }

    /**
     * Calculates the time-dependent burst-aware deadline urgency \(\Omega(t)\)
     * using the task's remaining execution time \(B\):
     * \[
     *   \Omega(t) = \left[ \frac{1000}{\text{EffectiveSlack}(t) + 1} \right] \times \left[ 1 + \frac{B}{D_{\text{effective}}(t) + 1} \right]
     * \]
     *
     * <p>Supports both non-preemptive (where \(B = \text{executionTime}\)) and preemptive
     * (where \(B = \text{remainingExecutionTime}\)) scheduling modes identically.
     *
     * @param task                   the task under evaluation
     * @param currentTime            current simulation timestamp
     * @param remainingExecutionTime remaining execution duration \(B \ge 0.0\)
     * @return dynamic urgency score \(\Omega(t) \ge 0.0\)
     */
    public double calculateUrgency(Task task, double currentTime, double remainingExecutionTime) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (!Double.isFinite(currentTime) || currentTime < 0.0) {
            throw new IllegalArgumentException("Current time must be finite and non-negative, got: " + currentTime);
        }
        if (!Double.isFinite(remainingExecutionTime) || remainingExecutionTime < 0.0) {
            throw new IllegalArgumentException("Remaining execution time must be finite and non-negative, got: " + remainingExecutionTime);
        }

        // ============================================================
        // DYNAMIC DEADLINE URGENCY ALGORITHM
        // ============================================================

        // Step 1: Calculate remaining deadline D(t)
        double remainingDeadline = task.getDeadline() - currentTime;

        // Step 2: Obtain execution/burst time B
        double burst = remainingExecutionTime;

        // Step 3: Calculate Slack(t)
        double slack = remainingDeadline - burst;

        // Step 4: Calculate EffectiveSlack(t)
        double effectiveSlack = Math.max(0.0, slack);

        // Step 5: Calculate D_effective(t) (numerical safety guard for overdue tasks)
        double dEffective = Math.max(0.0, remainingDeadline);

        // Step 6: Calculate burst-aware deadline urgency Omega(t)
        double slackTerm = 1000.0 / (effectiveSlack + 1.0);
        double burstFactor = 1.0 + (burst / (dEffective + 1.0));
        double omega = slackTerm * burstFactor;

        // Step 7: Return dynamic deadline urgency
        return omega;
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
}
