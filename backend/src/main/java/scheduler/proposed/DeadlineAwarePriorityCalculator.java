package scheduler.proposed;

import scheduler.model.Task;

/**
 * Implementation of the Dynamic Priority Algorithm
 * for the proposed deadline-aware cloud task scheduling method.
 *
 * <p><b>Algorithm Name:</b>
 * Dynamic Priority Algorithm
 *
 * <p><b>Purpose:</b>
 * Synthesizes the immutable base task priority (\(P_{\text{base}}\)), the Dynamic Deadline Urgency
 * (\(\Omega(t)\)), and elapsed waiting time (\(W(t)\)) into a single dynamic priority \(P(t)\).
 *
 * <p><b>Algorithmic Traceability:</b>
 * <ul>
 *   <li><b>Input:</b> Immutable base priority \(P_{\text{base}} = \text{task.getPriority()}\),
 *       task arrival time, execution time, deadline, and current simulation time \(t\).</li>
 *
 *   <li><b>Step 1 - Read Immutable Base Priority:</b>
 *       \(P_{\text{base}} = \text{task.getPriority()} \in [1, 10]\).</li>
 *
 *   <li><b>Step 2 - Calculate Waiting Time:</b>
 *       \(W(t) = \max(0.0, \, t - \text{arrivalTime})\).</li>
 *
 *   <li><b>Step 3 - Calculate Deadline Urgency:</b>
 *       Compute \(\Omega(t)\) via the Dynamic Deadline Urgency Algorithm:
 *       \[
 *         \Omega(t) = \left[ \frac{1000}{\text{EffectiveSlack}(t) + 1} \right] \times \left[ 1 + \frac{B}{D_{\text{effective}}(t) + 1} \right]
 *       \]
 *   </li>
 *
 *   <li><b>Step 4 - Calculate Dynamic Priority:</b>
 *       \[
 *         P(t) = 100 \cdot P_{\text{base}} + \Omega(t) + 10 \cdot W(t)
 *       \]
 *   </li>
 *
 *   <li><b>Step 5 - Store Dynamic Priority:</b>
 *       Store \(P(t)\) on the task via {@code task.setDynamicPriority(P(t))} during admission and updates.</li>
 *
 *   <li><b>Step 6 - Return Dynamic Priority:</b>
 *       Return \(P(t)\).</li>
 *
 *   <li><b>Output:</b> Dynamic priority score \(P(t)\).</li>
 * </ul>
 *
 * <p><b>Algorithm Specification:</b>
 * <pre>
 * Algorithm: Dynamic Priority Algorithm
 * Input:
 *   - Base priority P_base (immutable, from task.getPriority())
 *   - Dynamic deadline urgency Omega(t) (from Dynamic Deadline Urgency Algorithm)
 *   - Waiting time W(t)
 * Steps:
 *   1. Read immutable P_base = task.getPriority()
 *   2. Calculate waiting time: W(t) = max(0.0, currentTime - arrivalTime)
 *   3. Calculate deadline urgency: Omega(t) via Dynamic Deadline Urgency Algorithm
 *   4. Calculate dynamic priority:
 *        P(t) = 100 * P_base + Omega(t) + 10 * W(t)
 *   5. Store P(t) on task as dynamicPriority
 *   6. Return P(t)
 * Output:
 *   - Dynamic priority P(t)
 * </pre>
 *
 * <p><b>Guarantees:</b>
 * <ul>
 *   <li>The original task priority {@code task.getPriority()} is never overwritten or mutated.</li>
 *   <li>Does not use fixed 0.35, 0.50, 0.15 weights.</li>
 *   <li>Does not use AHP or PriorityNormalization.</li>
 * </ul>
 *
 * <p><b>Role in Proposed Algorithm:</b>
 * This is Component 2 of the proposed scheduling algorithm.
 * Its output \(P(t)\) is mapped to heap key \((-P(t), \text{deadline}, \text{taskId})\) in the
 * Persistent Fibonacci-Heap Scheduling Algorithm.
 */
public class DeadlineAwarePriorityCalculator {

    /** Characteristic waiting time constant retained for backward compatibility with telemetry exporters. */
    public static final double DEFAULT_TAU_WAITING = 50.0;

    private final DeadlineUrgencyCalculator urgencyCalculator;

    /**
     * Immutable container detailing the contribution of each factor to dynamic priority.
     */
    public record ScoreBreakdown(
        double finalScore,
        double basePriority,
        double deadlineUrgency,
        double waitingTime,
        double slack
    ) {}

    /**
     * Constructs a calculator with default urgency calculator instance.
     */
    public DeadlineAwarePriorityCalculator() {
        this(new DeadlineUrgencyCalculator());
    }

    /**
     * Constructs a calculator with custom urgency calculator.
     *
     * @param urgencyCalculator urgency calculator instance (must not be null)
     */
    public DeadlineAwarePriorityCalculator(DeadlineUrgencyCalculator urgencyCalculator) {
        if (urgencyCalculator == null) {
            throw new IllegalArgumentException("Urgency calculator cannot be null");
        }
        this.urgencyCalculator = urgencyCalculator;
    }

    // ============================================================
    // DYNAMIC PRIORITY ALGORITHM
    // ============================================================
    /**
     * Calculates the dynamic priority \(P(t)\) for the task at simulation clock \(t\):
     * \[
     *   P(t) = 100 \cdot P_{\text{base}} + \Omega(t) + 10 \cdot W(t)
     * \]
     *
     * @param task        the task under evaluation
     * @param currentTime current simulation timestamp
     * @return dynamic priority \(P(t)\)
     */
    public double calculateDynamicPriority(Task task, double currentTime) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        return calculateDynamicPriority(task, currentTime, task.getExecutionTime());
    }

    /**
     * Calculates the dynamic priority \(P(t)\) for the task at simulation clock \(t\)
     * using its specified remaining execution time \(B\):
     * \[
     *   P(t) = 100 \cdot P_{\text{base}} + \Omega(t) + 10 \cdot W(t)
     * \]
     *
     * <p>Identically supports non-preemptive (\(B = \text{executionTime}\)) and
     * preemptive (\(B = \text{remainingExecutionTime}\)) scheduling modes.
     *
     * @param task                   the task under evaluation
     * @param currentTime            current simulation timestamp
     * @param remainingExecutionTime remaining execution time \(B \ge 0.0\)
     * @return dynamic priority \(P(t)\)
     */
    public double calculateDynamicPriority(Task task, double currentTime, double remainingExecutionTime) {
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
        // DYNAMIC PRIORITY ALGORITHM
        // ============================================================

        // Step 1: Read immutable base priority P_base
        int basePriority = task.getPriority();

        // Step 2: Calculate waiting time W(t)
        double waitingTime = Math.max(0.0, currentTime - task.getArrivalTime());

        // Step 3: Calculate deadline urgency Omega(t) via Dynamic Deadline Urgency Algorithm
        double omega = urgencyCalculator.calculateUrgency(task, currentTime, remainingExecutionTime);

        // Step 4: Calculate dynamic priority P(t)
        double dynamicPriority = (100.0 * basePriority) + omega + (10.0 * waitingTime);

        // Step 5 & 6: Return dynamic priority P(t)
        return dynamicPriority;
    }

    /**
     * Computes the complete score breakdown for a task at the given simulation time.
     *
     * @param task        the task under evaluation
     * @param currentTime current simulation timestamp
     * @return populated ScoreBreakdown
     */
    public ScoreBreakdown calculateScoreBreakdown(Task task, double currentTime) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        return calculateScoreBreakdown(task, currentTime, task.getExecutionTime());
    }

    /**
     * Computes the complete score breakdown for a task at the given simulation time
     * using its specified remaining execution time.
     *
     * @param task                   the task under evaluation
     * @param currentTime            current simulation timestamp
     * @param remainingExecutionTime remaining execution duration
     * @return populated ScoreBreakdown
     */
    public ScoreBreakdown calculateScoreBreakdown(Task task, double currentTime, double remainingExecutionTime) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (!Double.isFinite(currentTime) || currentTime < 0.0) {
            throw new IllegalArgumentException("Current time must be finite and non-negative, got: " + currentTime);
        }
        if (!Double.isFinite(remainingExecutionTime) || remainingExecutionTime < 0.0) {
            throw new IllegalArgumentException("Remaining execution time must be finite and non-negative, got: " + remainingExecutionTime);
        }

        int basePriority = task.getPriority();
        double omega = urgencyCalculator.calculateUrgency(task, currentTime, remainingExecutionTime);
        double waitingTime = Math.max(0.0, currentTime - task.getArrivalTime());
        double slack = urgencyCalculator.calculateSlack(task, currentTime, remainingExecutionTime);

        double dynamicPriority = (100.0 * basePriority) + omega + (10.0 * waitingTime);

        return new ScoreBreakdown(dynamicPriority, basePriority, omega, waitingTime, slack);
    }

    /**
     * Convenience method returning only the scalar dynamic priority.
     *
     * @param task        the task under evaluation
     * @param currentTime current simulation timestamp
     * @return dynamic priority \(P(t)\)
     */
    public double calculateFinalScore(Task task, double currentTime) {
        return calculateDynamicPriority(task, currentTime);
    }

    public DeadlineUrgencyCalculator getUrgencyCalculator() {
        return urgencyCalculator;
    }
}
