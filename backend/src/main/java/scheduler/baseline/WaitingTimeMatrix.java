package scheduler.baseline;

import scheduler.model.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data structure representing the Waiting Time Matrix (WM / WTM) from the IEEE Access 2023 paper:
 * <em>"Task Scheduling in Cloud Computing: A Priority-Based Heuristic Approach"</em>
 * (Lipsa, Dash, Ivkovic, Cengiz, DOI: 10.1109/ACCESS.2023.3255781).
 *
 * <p><b>Algorithmic Traceability:</b>
 * <ul>
 *   <li><b>Waiting Time Formulation:</b> [Directly specified by IEEE paper]
 *       For a sequence of tasks ordered by arrival \(T_1 \succ T_2 \succ \dots \succ T_m\):
 *       \[
 *         WT(T_1) = 0
 *       \]
 *       \[
 *         WT(T_k) = \sum_{j=1}^{k-1} PT(T_j) \quad \text{for } k \ge 2
 *       \]
 *       where \(PT(T_j)\) is the processing/execution time of task \(T_j\).</li>
 *   <li><b>Matrix Construction:</b> [Directly specified by IEEE paper]
 *       \(WM\) is an \(m \times m\) matrix where:
 *       <ul>
 *         <li>Diagonal elements: \(WM[i][i] = 1.0\) for all \(i\).</li>
 *         <li>Upper triangle (\(i < j\)): \(WM[i][j] = WT(T_j)\).</li>
 *         <li>Lower triangle (\(i > j\)): \(WM[i][j] = \frac{1.0}{WM[j][i]} = \frac{1.0}{WT(T_i)}\).</li>
 *       </ul>
 *   </li>
 *   <li><b>Division-by-Zero Invariant:</b> [Clearly implied by paper]
 *       Since \(T_1\) only appears on the diagonal or in row 0, and for all \(j \ge 2\),
 *       \(WT(T_j) > 0\), the reciprocal \(\frac{1.0}{WT(T_i)}\) for \(i \ge 1\) is strictly well-defined.</li>
 *   <li><b>Single-Task Base Case (\(m=1\)):</b> [Clearly implied by paper]
 *       Produces a \(1 \times 1\) identity matrix \([[1.0]]\).</li>
 * </ul>
 */
public class WaitingTimeMatrix {

    private final int dimension;
    private final List<Task> tasks;
    private final double[] waitingTimes;
    private final double[][] matrix;

    /**
     * Constructs a WaitingTimeMatrix for the supplied list of tasks.
     * Tasks are evaluated in their arrival sequence.
     *
     * @param taskList the list of tasks (must not be null or empty)
     * @throws IllegalArgumentException if taskList is null or empty
     */
    public WaitingTimeMatrix(List<Task> taskList) {
        if (taskList == null || taskList.isEmpty()) {
            throw new IllegalArgumentException("Task list for WaitingTimeMatrix cannot be null or empty");
        }

        this.dimension = taskList.size();
        this.tasks = Collections.unmodifiableList(new ArrayList<>(taskList));
        this.waitingTimes = new double[this.dimension];
        this.matrix = new double[this.dimension][this.dimension];

        computeWaitingTimes();
        buildMatrix();
    }

    /**
     * Computes the sequential waiting time WT(T_k) for each task as specified in Section III of the paper:
     * WT(T_1) = 0
     * WT(T_k) = sum_{j=1}^{k-1} PT(T_j)
     */
    private void computeWaitingTimes() {
        this.waitingTimes[0] = 0.0;
        double accumulatedProcessingTime = 0.0;

        for (int k = 0; k < this.dimension; k++) {
            this.waitingTimes[k] = accumulatedProcessingTime;
            accumulatedProcessingTime += this.tasks.get(k).getExecutionTime();
        }
    }

    /**
     * Populates the m x m Waiting Time Matrix:
     * - Diagonal: 1.0
     * - Upper triangle (i < j): WT(T_j)
     * - Lower triangle (i > j): 1.0 / WT(T_i)
     */
    private void buildMatrix() {
        for (int i = 0; i < this.dimension; i++) {
            for (int j = 0; j < this.dimension; j++) {
                if (i == j) {
                    this.matrix[i][j] = 1.0;
                } else if (i < j) {
                    this.matrix[i][j] = this.waitingTimes[j];
                } else {
                    // Lower triangle: reciprocal of corresponding upper element WM[j][i]
                    double upperValue = this.matrix[j][i];
                    if (upperValue <= 0.0) {
                        // Fallback safety for edge cases where execution time is negligibly small
                        this.matrix[i][j] = 1.0;
                    } else {
                        this.matrix[i][j] = 1.0 / upperValue;
                    }
                }
            }
        }
    }

    /**
     * Returns the dimension m of the square matrix.
     *
     * @return number of tasks (matrix size m)
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * Returns the task at index k (0-indexed).
     *
     * @param index 0-indexed task position
     * @return the Task at index
     */
    public Task getTask(int index) {
        return tasks.get(index);
    }

    /**
     * Returns an unmodifiable view of the tasks represented in this matrix.
     *
     * @return unmodifiable list of tasks
     */
    public List<Task> getTasks() {
        return tasks;
    }

    /**
     * Returns the calculated sequential waiting time for task at index k.
     *
     * @param index 0-indexed task position
     * @return computed WT(T_k)
     */
    public double getWaitingTime(int index) {
        if (index < 0 || index >= dimension) {
            throw new IndexOutOfBoundsException("Task index out of bounds: " + index);
        }
        return waitingTimes[index];
    }

    /**
     * Returns a defensive copy of the waiting times array.
     *
     * @return copy of waitingTimes
     */
    public double[] getWaitingTimesCopy() {
        return waitingTimes.clone();
    }

    /**
     * Returns the matrix element WM[i][j].
     *
     * @param i row index (0-indexed)
     * @param j column index (0-indexed)
     * @return value at WM[i][j]
     */
    public double get(int i, int j) {
        if (i < 0 || i >= dimension || j < 0 || j >= dimension) {
            throw new IndexOutOfBoundsException(String.format("Matrix indices [%d, %d] out of bounds for size %d", i, j, dimension));
        }
        return matrix[i][j];
    }

    /**
     * Returns a deep copy of the underlying 2D matrix array.
     *
     * @return deep copy of matrix
     */
    public double[][] getMatrixCopy() {
        double[][] copy = new double[dimension][dimension];
        for (int i = 0; i < dimension; i++) {
            System.arraycopy(matrix[i], 0, copy[i], 0, dimension);
        }
        return copy;
    }
}
