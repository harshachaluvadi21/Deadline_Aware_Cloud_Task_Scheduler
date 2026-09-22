package scheduler.baseline;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import scheduler.model.Task;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite for {@link WaitingTimeMatrix} verifying exact compliance with Section III
 * of the IEEE Access 2023 reference paper.
 */
class WaitingTimeMatrixTest {

    private Task createTask(long id, double arrivalTime, double executionTime) {
        return new Task(id, 5, arrivalTime, executionTime, arrivalTime + executionTime * 3.0);
    }

    @Nested
    @DisplayName("Matrix Construction & Formulation Tests")
    class FormulationTests {

        @Test
        @DisplayName("Verifies sequential waiting times: WT(T1)=0, WT(T2)=PT(T1), WT(T3)=PT(T1)+PT(T2)")
        void testSequentialWaitingTimes() {
            Task t1 = createTask(1, 0.0, 5.0);
            Task t2 = createTask(2, 0.0, 8.0);
            Task t3 = createTask(3, 0.0, 12.0);

            WaitingTimeMatrix wtm = new WaitingTimeMatrix(List.of(t1, t2, t3));

            assertEquals(3, wtm.getDimension());
            assertEquals(0.0, wtm.getWaitingTime(0), 1e-9, "WT(T1) must be 0.0");
            assertEquals(5.0, wtm.getWaitingTime(1), 1e-9, "WT(T2) must be PT(T1) = 5.0");
            assertEquals(13.0, wtm.getWaitingTime(2), 1e-9, "WT(T3) must be PT(T1) + PT(T2) = 13.0");
        }

        @Test
        @DisplayName("Verifies diagonal elements are strictly 1.0")
        void testDiagonalElements() {
            List<Task> tasks = List.of(
                createTask(1, 0.0, 2.0),
                createTask(2, 1.0, 4.0),
                createTask(3, 2.0, 6.0),
                createTask(4, 3.0, 8.0)
            );

            WaitingTimeMatrix wtm = new WaitingTimeMatrix(tasks);

            for (int i = 0; i < 4; i++) {
                assertEquals(1.0, wtm.get(i, i), 1e-9, "Diagonal WM[" + i + "][" + i + "] must be 1.0");
            }
        }

        @Test
        @DisplayName("Verifies upper triangle contains WT(T_j) and lower triangle contains 1 / WT(T_i)")
        void testUpperAndLowerTriangles() {
            Task t1 = createTask(1, 0.0, 10.0);
            Task t2 = createTask(2, 0.0, 20.0);
            Task t3 = createTask(3, 0.0, 30.0);

            WaitingTimeMatrix wtm = new WaitingTimeMatrix(List.of(t1, t2, t3));

            // WT(T1) = 0.0, WT(T2) = 10.0, WT(T3) = 30.0
            // Upper triangle:
            assertEquals(10.0, wtm.get(0, 1), 1e-9, "WM[0][1] should be WT(T2) = 10.0");
            assertEquals(30.0, wtm.get(0, 2), 1e-9, "WM[0][2] should be WT(T3) = 30.0");
            assertEquals(30.0, wtm.get(1, 2), 1e-9, "WM[1][2] should be WT(T3) = 30.0");

            // Lower triangle (reciprocals):
            assertEquals(1.0 / 10.0, wtm.get(1, 0), 1e-9, "WM[1][0] should be 1 / WM[0][1] = 0.1");
            assertEquals(1.0 / 30.0, wtm.get(2, 0), 1e-9, "WM[2][0] should be 1 / WM[0][2] = 1/30");
            assertEquals(1.0 / 30.0, wtm.get(2, 1), 1e-9, "WM[2][1] should be 1 / WM[1][2] = 1/30");
        }

        @Test
        @DisplayName("Handles single-task edge case m=1 gracefully")
        void testSingleTask() {
            Task t1 = createTask(1, 0.0, 15.0);
            WaitingTimeMatrix wtm = new WaitingTimeMatrix(List.of(t1));

            assertEquals(1, wtm.getDimension());
            assertEquals(0.0, wtm.getWaitingTime(0));
            assertEquals(1.0, wtm.get(0, 0));
        }

        @Test
        @DisplayName("Alias WaitingTimeControlMatrix operates identically")
        void testAliasClass() {
            Task t1 = createTask(1, 0.0, 5.0);
            Task t2 = createTask(2, 0.0, 5.0);

            WaitingTimeControlMatrix wtcm = new WaitingTimeControlMatrix(List.of(t1, t2));
            assertEquals(2, wtcm.getDimension());
            assertEquals(1.0, wtcm.get(0, 0));
            assertEquals(5.0, wtcm.get(0, 1));
            assertEquals(0.2, wtcm.get(1, 0), 1e-9);
        }
    }

    @Nested
    @DisplayName("Validation & Defensive Copy Tests")
    class ValidationTests {

        @Test
        @DisplayName("Rejects null or empty task lists")
        void testNullOrEmptyInput() {
            assertThrows(IllegalArgumentException.class, () -> new WaitingTimeMatrix(null));
            assertThrows(IllegalArgumentException.class, () -> new WaitingTimeMatrix(List.of()));
        }

        @Test
        @DisplayName("Throws IndexOutOfBoundsException on invalid coordinate accesses")
        void testOutOfBounds() {
            Task t1 = createTask(1, 0.0, 5.0);
            WaitingTimeMatrix wtm = new WaitingTimeMatrix(List.of(t1));

            assertThrows(IndexOutOfBoundsException.class, () -> wtm.get(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> wtm.get(0, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> wtm.getWaitingTime(5));
        }

        @Test
        @DisplayName("Ensures defensive copying of matrix and waiting times")
        void testDefensiveCopies() {
            Task t1 = createTask(1, 0.0, 5.0);
            Task t2 = createTask(2, 0.0, 10.0);
            WaitingTimeMatrix wtm = new WaitingTimeMatrix(List.of(t1, t2));

            double[][] copy = wtm.getMatrixCopy();
            copy[0][0] = 999.0;
            assertEquals(1.0, wtm.get(0, 0), "Original matrix must not be mutated");

            double[] wtCopy = wtm.getWaitingTimesCopy();
            wtCopy[0] = 999.0;
            assertEquals(0.0, wtm.getWaitingTime(0), "Original waiting times must not be mutated");
        }
    }
}
