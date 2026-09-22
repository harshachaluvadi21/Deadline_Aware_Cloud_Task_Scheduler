package scheduler.baseline;

import scheduler.model.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of Algorithm 1: <em>Priority_Assignment_to_Task</em> from the IEEE Access 2023 paper:
 * <em>"Task Scheduling in Cloud Computing: A Priority-Based Heuristic Approach"</em>
 * (Lipsa, Dash, Ivkovic, Cengiz, DOI: 10.1109/ACCESS.2023.3255781).
 *
 * <p><b>Algorithmic Traceability:</b>
 * <ul>
 *   <li><b>Input:</b> [Directly specified by IEEE paper]
 *       A set of tasks \(T = \{T_1, T_2, \dots, T_m\}\) with calculated sequential waiting times \(WT(T_k)\).</li>
 *   <li><b>Waiting Matrix Generation:</b> [Directly specified by IEEE paper]
 *       Constructs \(WM\) where \(WM[i][i] = 1\), \(WM[i][j] = WT(T_j)\), and \(WM[j][i] = \frac{1}{WM[i][j]}\).</li>
 *   <li><b>Eigenvector &amp; Eigenvalue Computation:</b> [Directly specified by IEEE paper]
 *       Computes principal eigenvector (\(egvt\)) and maximum eigenvalue (\(\lambda_{max}\)).
 *       Solved deterministically via the numerical Power Iteration method [Clearly implied by paper].</li>
 *   <li><b>Consistency Verification:</b> [Directly specified by IEEE paper]
 *       Evaluates Consistency Index \(CI = \frac{\lambda_{max} - m}{m - 1}\) (for \(m > 1\)) and
 *       Consistency Ratio \(CR = \frac{CI}{RI}\). Uses Saaty's standard Random Index (RI) table.
 *       If \(CR \ge 0.1\), decreases \(WT\) of the maximum waiting task and iterates until convergence.</li>
 *   <li><b>Priority Derivation (Lemma 1):</b> [Directly specified by IEEE paper]
 *       For each task \(k\):
 *       \[
 *         Priority(T_k) = 1.0 - egvt[k]
 *       \]
 *       Since \(egvt[k] \in (0, 1)\) and \(\sum egvt = 1.0\), \(Priority(T_k) \in (0, 1)\) strictly.
 *       Tasks with shorter waiting delays receive smaller eigenvector components and higher priority.</li>
 * </ul>
 */
public class PriorityAssignment {

    /** Maximum allowed consistency ratio before convergence (AHP standard used in paper). */
    public static final double CONSISTENCY_RATIO_THRESHOLD = 0.1;

    /** Maximum iterations for consistency ratio convergence loop to prevent infinite loops. */
    private static final int MAX_CONVERGENCE_ITERATIONS = 50;

    /** Power iteration convergence tolerance. */
    private static final double POWER_ITERATION_TOLERANCE = 1e-9;
    private static final int MAX_POWER_ITERATIONS = 1000;

    /**
     * Saaty's Random Index (RI) lookup table for matrix dimensions m = 1 to 10.
     * (Index corresponds to matrix size m).
     */
    private static final double[] SAATY_RI = {
        0.00, // m = 0 (unused)
        0.00, // m = 1
        0.00, // m = 2
        0.58, // m = 3
        0.90, // m = 4
        1.12, // m = 5
        1.24, // m = 6
        1.32, // m = 7
        1.41, // m = 8
        1.45, // m = 9
        1.49  // m = 10
    };

    private final List<Task> tasks;
    private final int taskCount;
    private double[] eigenvector;
    private double maxEigenvalue;
    private double consistencyIndex;
    private double consistencyRatio;
    private double[] assignedPriorities;

    /**
     * Executes Algorithm 1 (Priority_Assignment_to_Task) on the supplied tasks.
     * Calculates WTM, checks consistency, derives priorities, and updates each task's dynamicPriority.
     *
     * @param tasks list of tasks to prioritize
     * @throws IllegalArgumentException if tasks is null or empty
     */
    public PriorityAssignment(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            throw new IllegalArgumentException("Task list for PriorityAssignment cannot be null or empty");
        }

        this.tasks = Collections.unmodifiableList(new ArrayList<>(tasks));
        this.taskCount = tasks.size();
        this.assignedPriorities = new double[this.taskCount];

        executeAlgorithm1();
    }

    /**
     * Executes Algorithm 1 from Lipsa et al. (IEEE Access 2023):
     * 1. While (Condition for convergence):
     *      Calculate WT(T_k)
     *      Generate Waiting Matrix (WM)
     *      Compute eigenvector (egvt) and eigenvalue (egv) of WM
     *      Let lambda_max = max(egv)
     *      Compute CI = (lambda_max - m) / (m - 1)
     *      Compute CR = CI / RI
     *      If CR < 0.1 then break
     *      Else decrease WT(T_k) for task with max WT
     * 2. End While
     * 3. For k = 1 to m do:
     *      Priority(T_k) = 1 - egvt(k)
     * 4. End For
     */
    private void executeAlgorithm1() {
        if (taskCount == 1) {
            handleSingleTask();
            return;
        }

        // Initialize mutable waiting times from initial sequential execution
        double[] currentWaitingTimes = new double[taskCount];
        currentWaitingTimes[0] = 0.0;
        double accumulated = 0.0;
        for (int k = 0; k < taskCount; k++) {
            currentWaitingTimes[k] = accumulated;
            accumulated += tasks.get(k).getExecutionTime();
        }

        int iterations = 0;
        double currentCr = 1.0;

        while (iterations < MAX_CONVERGENCE_ITERATIONS) {
            iterations++;

            // Step 1: Generate Waiting Matrix from current waiting times
            double[][] wm = constructMatrix(currentWaitingTimes);

            // Step 2: Compute principal eigenvector and maximum eigenvalue via Power Iteration
            double[] egvt = computePrincipalEigenvector(wm);
            double lambdaMax = computeEigenvalue(wm, egvt);

            // Step 3: Compute Consistency Index (CI) and Consistency Ratio (CR)
            double ci = (lambdaMax - taskCount) / (taskCount - 1);
            if (ci < 0.0) {
                ci = 0.0; // Numerical rounding safety
            }
            double ri = getRandomIndex(taskCount);
            currentCr = (ri > 0.0) ? (ci / ri) : 0.0;

            this.eigenvector = egvt;
            this.maxEigenvalue = lambdaMax;
            this.consistencyIndex = ci;
            this.consistencyRatio = currentCr;

            // Step 4: Check convergence condition
            if (currentCr < CONSISTENCY_RATIO_THRESHOLD) {
                break;
            }

            // Step 5: Adjust waiting time of the task with maximum waiting time
            int maxWtIdx = findMaxWaitingTimeIndex(currentWaitingTimes);
            currentWaitingTimes[maxWtIdx] *= 0.90; // Decrement step as specified by paper
        }

        // Step 6: Derive task priorities: Priority(T_k) = 1.0 - egvt[k]
        for (int k = 0; k < taskCount; k++) {
            double rawPriority = 1.0 - this.eigenvector[k];
            // Ensure strict bounding in (0, 1) per Lemma 1
            double boundedPriority = Math.max(0.0001, Math.min(0.9999, rawPriority));
            this.assignedPriorities[k] = boundedPriority;
            this.tasks.get(k).setDynamicPriority(boundedPriority);
        }
    }

    private void handleSingleTask() {
        this.eigenvector = new double[]{1.0};
        this.maxEigenvalue = 1.0;
        this.consistencyIndex = 0.0;
        this.consistencyRatio = 0.0;
        this.assignedPriorities[0] = 0.5; // Neutral baseline priority for lone task in system
        this.tasks.get(0).setDynamicPriority(0.5);
    }

    private double[][] constructMatrix(double[] wt) {
        int m = wt.length;
        double[][] matrix = new double[m][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                if (i == j) {
                    matrix[i][j] = 1.0;
                } else if (i < j) {
                    matrix[i][j] = wt[j];
                } else {
                    double upper = matrix[j][i];
                    matrix[i][j] = (upper > 0.0) ? (1.0 / upper) : 1.0;
                }
            }
        }
        return matrix;
    }

    /**
     * Deterministic Power Iteration method to compute the principal eigenvector of a positive matrix.
     * Perron-Frobenius theorem guarantees existence of unique positive dominant eigenvector.
     */
    private double[] computePrincipalEigenvector(double[][] a) {
        int n = a.length;
        double[] v = new double[n];
        for (int i = 0; i < n; i++) {
            v[i] = 1.0 / n;
        }

        for (int iter = 0; iter < MAX_POWER_ITERATIONS; iter++) {
            double[] nextV = new double[n];
            double sum = 0.0;

            for (int i = 0; i < n; i++) {
                double dot = 0.0;
                for (int j = 0; j < n; j++) {
                    dot += a[i][j] * v[j];
                }
                nextV[i] = dot;
                sum += dot;
            }

            if (sum <= 0.0) {
                sum = 1.0;
            }

            // Normalize so sum equals 1.0
            double maxDiff = 0.0;
            for (int i = 0; i < n; i++) {
                nextV[i] /= sum;
                double diff = Math.abs(nextV[i] - v[i]);
                if (diff > maxDiff) {
                    maxDiff = diff;
                }
            }

            v = nextV;
            if (maxDiff < POWER_ITERATION_TOLERANCE) {
                break;
            }
        }

        return v;
    }

    /**
     * Calculates the dominant eigenvalue lambda_max from matrix A and its eigenvector v.
     * lambda_max = (1 / n) * sum_{i=1}^n ((A * v)_i / v_i)
     */
    private double computeEigenvalue(double[][] a, double[] v) {
        int n = a.length;
        double sumLambda = 0.0;
        int validTerms = 0;

        for (int i = 0; i < n; i++) {
            double av_i = 0.0;
            for (int j = 0; j < n; j++) {
                av_i += a[i][j] * v[j];
            }
            if (v[i] > 1e-12) {
                sumLambda += (av_i / v[i]);
                validTerms++;
            }
        }

        return (validTerms > 0) ? (sumLambda / validTerms) : (double) n;
    }

    private int findMaxWaitingTimeIndex(double[] wt) {
        int maxIdx = 0;
        double maxVal = wt[0];
        for (int i = 1; i < wt.length; i++) {
            if (wt[i] > maxVal) {
                maxVal = wt[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    /**
     * Returns Saaty's Random Index (RI) for matrix dimension m.
     * Uses lookup table for m <= 10, and standard Golden-Wang approximation for m > 10.
     */
    public static double getRandomIndex(int m) {
        if (m <= 0) {
            return 0.0;
        }
        if (m < SAATY_RI.length) {
            return SAATY_RI[m];
        }
        // Standard AHP approximation for large m [Implementation assumption]
        return 1.98 * (m - 2.0) / m;
    }

    // ==========================================
    // Query Methods
    // ==========================================

    public List<Task> getTasks() {
        return tasks;
    }

    public int getTaskCount() {
        return taskCount;
    }

    public double[] getEigenvectorCopy() {
        return eigenvector.clone();
    }

    public double getMaxEigenvalue() {
        return maxEigenvalue;
    }

    public double getConsistencyIndex() {
        return consistencyIndex;
    }

    public double getConsistencyRatio() {
        return consistencyRatio;
    }

    public double[] getAssignedPrioritiesCopy() {
        return assignedPriorities.clone();
    }

    public double getPriority(int index) {
        if (index < 0 || index >= taskCount) {
            throw new IndexOutOfBoundsException("Task index out of bounds: " + index);
        }
        return assignedPriorities[index];
    }
}
