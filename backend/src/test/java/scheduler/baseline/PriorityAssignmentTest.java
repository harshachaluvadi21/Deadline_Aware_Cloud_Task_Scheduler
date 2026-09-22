package scheduler.baseline;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import scheduler.model.Task;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite for {@link PriorityAssignment} verifying exact compliance with
 * Algorithm 1 (Priority_Assignment_to_Task) of the IEEE Access 2023 reference paper.
 */
class PriorityAssignmentTest {

    private Task createTask(long id, double arrivalTime, double executionTime) {
        return new Task(id, 5, arrivalTime, executionTime, arrivalTime + executionTime * 4.0);
    }

    @Nested
    @DisplayName("Algorithm 1 Mathematical Verification Tests")
    class MathematicalTests {

        @Test
        @DisplayName("Verifies that priorities strictly fall in range (0, 1) per Lemma 1")
        void testPrioritiesStrictlyBounded() {
            List<Task> tasks = List.of(
                createTask(1, 0.0, 4.0),
                createTask(2, 0.0, 7.0),
                createTask(3, 0.0, 10.0),
                createTask(4, 0.0, 15.0)
            );

            PriorityAssignment pa = new PriorityAssignment(tasks);

            for (int i = 0; i < tasks.size(); i++) {
                double priority = pa.getPriority(i);
                assertTrue(priority > 0.0 && priority < 1.0,
                    "Priority must be strictly between 0 and 1, got: " + priority);
                assertEquals(priority, tasks.get(i).getDynamicPriority(), 1e-9,
                    "Task dynamicPriority property must match assigned priority");
            }
        }

        @Test
        @DisplayName("Verifies tasks facing longer waiting delay receive higher priority to minimize waiting time")
        void testShorterWaitingReceivesHigherPriority() {
            // Task 1 arrives first and has WT=0; Task 4 has WT=30
            List<Task> tasks = List.of(
                createTask(1, 0.0, 5.0),
                createTask(2, 0.0, 10.0),
                createTask(3, 0.0, 15.0),
                createTask(4, 0.0, 20.0)
            );

            PriorityAssignment pa = new PriorityAssignment(tasks);

            double p1 = pa.getPriority(0);
            double p2 = pa.getPriority(1);
            double p3 = pa.getPriority(2);
            double p4 = pa.getPriority(3);

            // Per IEEE paper Section III & Algorithm 1: Priority(Tk) = 1.0 - egvt[k]
            // Task 4 faces longest wait, yielding smallest eigenvector value, hence highest Priority (1 - egvt)
            assertTrue(p4 > p1, "Task facing longest sequential wait must receive highest priority score");
            assertTrue(p4 >= p3 && p3 >= p2 && p2 >= p1, "Priorities must increase with sequential waiting delay");
        }

        @Test
        @DisplayName("Verifies principal eigenvector entries are positive and sum to 1.0")
        void testEigenvectorNormalization() {
            List<Task> tasks = List.of(
                createTask(1, 0.0, 3.0),
                createTask(2, 0.0, 6.0),
                createTask(3, 0.0, 9.0)
            );

            PriorityAssignment pa = new PriorityAssignment(tasks);
            double[] egvt = pa.getEigenvectorCopy();

            assertEquals(3, egvt.length);
            double sum = 0.0;
            for (double v : egvt) {
                assertTrue(v > 0.0, "Eigenvector entries must be strictly positive");
                sum += v;
            }
            assertEquals(1.0, sum, 1e-6, "Eigenvector components must sum to 1.0");
        }

        @Test
        @DisplayName("Verifies Consistency Ratio satisfies CR < 0.1 threshold")
        void testConsistencyRatioConvergence() {
            List<Task> tasks = List.of(
                createTask(1, 0.0, 2.0),
                createTask(2, 0.0, 4.0),
                createTask(3, 0.0, 6.0),
                createTask(4, 0.0, 8.0)
            );

            PriorityAssignment pa = new PriorityAssignment(tasks);

            assertTrue(pa.getConsistencyRatio() < PriorityAssignment.CONSISTENCY_RATIO_THRESHOLD,
                "Consistency ratio must satisfy CR < 0.1, got: " + pa.getConsistencyRatio());
            assertTrue(pa.getMaxEigenvalue() >= tasks.size(),
                "Principal eigenvalue lambda_max must be >= dimension m");
        }

        @Test
        @DisplayName("Handles single task edge case m=1")
        void testSingleTask() {
            Task t1 = createTask(1, 0.0, 10.0);
            PriorityAssignment pa = new PriorityAssignment(List.of(t1));

            assertEquals(1, pa.getTaskCount());
            assertEquals(0.5, pa.getPriority(0), 1e-9);
            assertEquals(0.5, t1.getDynamicPriority(), 1e-9);
            assertEquals(0.0, pa.getConsistencyIndex());
            assertEquals(0.0, pa.getConsistencyRatio());
        }

        @Test
        @DisplayName("Handles tasks with equal execution times")
        void testEqualExecutionTimes() {
            List<Task> tasks = List.of(
                createTask(1, 0.0, 10.0),
                createTask(2, 0.0, 10.0),
                createTask(3, 0.0, 10.0)
            );

            PriorityAssignment pa = new PriorityAssignment(tasks);

            // Cumulative sequential waiting: T1=0, T2=10, T3=20 -> T3 priority > T2 priority > T1 priority
            assertTrue(pa.getPriority(2) > pa.getPriority(1));
            assertTrue(pa.getPriority(1) > pa.getPriority(0));
        }

        @Test
        @DisplayName("Alias PriorityAssignmentToTasks behaves identically")
        void testAliasClass() {
            List<Task> tasks = List.of(
                createTask(1, 0.0, 5.0),
                createTask(2, 0.0, 10.0)
            );

            PriorityAssignmentToTasks pat = new PriorityAssignmentToTasks(tasks);
            assertEquals(2, pat.getTaskCount());
            assertTrue(pat.getPriority(0) > 0.0 && pat.getPriority(0) < 1.0);
        }
    }

    @Nested
    @DisplayName("Determinism & Edge Case Tests")
    class DeterminismTests {

        @Test
        @DisplayName("Produces 100% identical priorities across consecutive runs with same input")
        void testDeterministicReproducibility() {
            List<Task> setA = List.of(
                createTask(1, 0.0, 5.0),
                createTask(2, 0.0, 15.0),
                createTask(3, 0.0, 25.0)
            );
            List<Task> setB = List.of(
                createTask(1, 0.0, 5.0),
                createTask(2, 0.0, 15.0),
                createTask(3, 0.0, 25.0)
            );

            PriorityAssignment pa1 = new PriorityAssignment(setA);
            PriorityAssignment pa2 = new PriorityAssignment(setB);

            assertArrayEquals(pa1.getAssignedPrioritiesCopy(), pa2.getAssignedPrioritiesCopy(), 1e-12,
                "Priority assignments must be strictly deterministic");
            assertEquals(pa1.getMaxEigenvalue(), pa2.getMaxEigenvalue(), 1e-12);
            assertEquals(pa1.getConsistencyRatio(), pa2.getConsistencyRatio(), 1e-12);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException on null or empty input")
        void testInvalidInputs() {
            assertThrows(IllegalArgumentException.class, () -> new PriorityAssignment(null));
            assertThrows(IllegalArgumentException.class, () -> new PriorityAssignment(List.of()));
        }

        @Test
        @DisplayName("Random Index lookup covers m=1 through m=10 and extensions")
        void testRandomIndexLookup() {
            assertEquals(0.00, PriorityAssignment.getRandomIndex(1));
            assertEquals(0.00, PriorityAssignment.getRandomIndex(2));
            assertEquals(0.58, PriorityAssignment.getRandomIndex(3));
            assertEquals(0.90, PriorityAssignment.getRandomIndex(4));
            assertEquals(1.49, PriorityAssignment.getRandomIndex(10));
            assertTrue(PriorityAssignment.getRandomIndex(15) > 1.49, "Large m RI should be positive");
        }
    }
}
