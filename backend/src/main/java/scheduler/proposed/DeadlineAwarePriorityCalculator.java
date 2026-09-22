package scheduler.proposed;

import scheduler.model.Task;

/**
 * Calculates the multi-factor deadline-aware priority score combining:
 * <ul>
 *   <li>Base Task Priority (\(P_{\text{norm}}\))</li>
 *   <li>Deadline Urgency (\(U_{\text{deadline}}(t)\))</li>
 *   <li>Elapsed Waiting Time (\(W_{\text{wait}}(t)\))</li>
 * </ul>
 *
 * <p><b>[Project Design Decision]</b>
 * Mathematical formulation:
 * \[
 *   \text{FinalScore}(t) = W_p \cdot P_{\text{norm}} + W_d \cdot U_{\text{deadline}}(t) + W_w \cdot W_{\text{wait}}(t)
 * \]
 * where:
 * <ul>
 *   <li>\(W_p = 0.35\) (Base Priority Weight)</li>
 *   <li>\(W_d = 0.50\) (Deadline Urgency Weight)</li>
 *   <li>\(W_w = 0.15\) (Waiting Factor Weight)</li>
 *   <li>\(W_p + W_d + W_w = 1.0\)</li>
 * </ul>
 * <b>NOTE ON WEIGHTS:</b> These are <b>initial project design weights</b>.
 * They are not derived from the IEEE paper and are not claimed to be optimal.
 *
 * <p>Execution time is incorporated directly through the slack calculation in
 * {@link DeadlineUrgencyCalculator#calculateSlack} and is not double-counted.
 */
public class DeadlineAwarePriorityCalculator {

    /** Initial project design weight for base priority. */
    public static final double DEFAULT_WP = 0.35;
    /** Initial project design weight for deadline urgency. */
    public static final double DEFAULT_WD = 0.50;
    /** Initial project design weight for waiting factor. */
    public static final double DEFAULT_WW = 0.15;

    /** Characteristic waiting time saturation constant (seconds). */
    public static final double DEFAULT_TAU_WAITING = 50.0;

    private final double wp;
    private final double wd;
    private final double ww;
    private final double tauWaiting;
    private final DeadlineUrgencyCalculator urgencyCalculator;

    /**
     * Immutable container detailing the contribution of each factor to the final score.
     */
    public record ScoreBreakdown(
        double finalScore,
        double basePriorityNormalized,
        double deadlineUrgency,
        double waitingFactor,
        double slack
    ) {}

    /**
     * Constructs a calculator using default initial project design weights (0.35, 0.50, 0.15)
     * and default decay parameters.
     */
    public DeadlineAwarePriorityCalculator() {
        this(DEFAULT_WP, DEFAULT_WD, DEFAULT_WW, DEFAULT_TAU_WAITING, new DeadlineUrgencyCalculator());
    }

    /**
     * Constructs a calculator with custom factor weights.
     *
     * @param wp weight for base priority (&gt;= 0)
     * @param wd weight for deadline urgency (&gt;= 0)
     * @param ww weight for waiting factor (&gt;= 0)
     * @throws IllegalArgumentException if weights are negative, non-finite, or do not sum to 1.0
     */
    public DeadlineAwarePriorityCalculator(double wp, double wd, double ww) {
        this(wp, wd, ww, DEFAULT_TAU_WAITING, new DeadlineUrgencyCalculator());
    }

    /**
     * Full constructor supporting custom weights, saturation constants, and urgency calculators.
     *
     * @param wp                base priority weight
     * @param wd                deadline urgency weight
     * @param ww                waiting factor weight
     * @param tauWaiting        waiting saturation parameter in seconds (&gt; 0)
     * @param urgencyCalculator urgency calculator instance (must not be null)
     */
    public DeadlineAwarePriorityCalculator(
            double wp, double wd, double ww,
            double tauWaiting,
            DeadlineUrgencyCalculator urgencyCalculator) {

        if (!Double.isFinite(wp) || wp < 0.0) throw new IllegalArgumentException("wp must be finite and >= 0, got: " + wp);
        if (!Double.isFinite(wd) || wd < 0.0) throw new IllegalArgumentException("wd must be finite and >= 0, got: " + wd);
        if (!Double.isFinite(ww) || ww < 0.0) throw new IllegalArgumentException("ww must be finite and >= 0, got: " + ww);

        double sum = wp + wd + ww;
        if (Math.abs(sum - 1.0) > 1e-6) {
            throw new IllegalArgumentException("Factor weights must sum to 1.0, got: " + sum);
        }

        if (!Double.isFinite(tauWaiting) || tauWaiting <= 0.0) {
            throw new IllegalArgumentException("tauWaiting must be positive, got: " + tauWaiting);
        }
        if (urgencyCalculator == null) {
            throw new IllegalArgumentException("Urgency calculator cannot be null");
        }

        this.wp = wp;
        this.wd = wd;
        this.ww = ww;
        this.tauWaiting = tauWaiting;
        this.urgencyCalculator = urgencyCalculator;
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
        if (!Double.isFinite(currentTime) || currentTime < 0.0) {
            throw new IllegalArgumentException("Current time must be finite and non-negative, got: " + currentTime);
        }

        // 1. Base Priority Normalized
        double pNorm = PriorityNormalization.normalize(task.getPriority());

        // 2. Deadline Urgency & Slack
        double slack = urgencyCalculator.calculateSlack(task, currentTime);
        double uDeadline = urgencyCalculator.calculateUrgencyFromSlack(slack, task.getExecutionTime());

        // 3. Waiting Factor Normalized via saturation curve
        double waitDuration = Math.max(0.0, currentTime - task.getArrivalTime());
        double wWait = waitDuration / (waitDuration + this.tauWaiting);

        // 4. Final Weighted Score
        double finalScore = (this.wp * pNorm) + (this.wd * uDeadline) + (this.ww * wWait);

        return new ScoreBreakdown(finalScore, pNorm, uDeadline, wWait, slack);
    }

    /**
     * Convenience method returning only the scalar final score.
     *
     * @param task        the task under evaluation
     * @param currentTime current simulation timestamp
     * @return scalar final score in [0.0, 1.0]
     */
    public double calculateFinalScore(Task task, double currentTime) {
        return calculateScoreBreakdown(task, currentTime).finalScore();
    }

    // Getters
    public double getWp() { return wp; }
    public double getWd() { return wd; }
    public double getWw() { return ww; }
    public double getTauWaiting() { return tauWaiting; }
    public DeadlineUrgencyCalculator getUrgencyCalculator() { return urgencyCalculator; }
}
