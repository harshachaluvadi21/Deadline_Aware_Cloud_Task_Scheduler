package scheduler.proposed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import scheduler.model.Task;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DeadlineUrgencyCalculator} verifying slack time, burst-aware urgency response,
 * critical/overdue distinctions, and denominator safety.
 */
class DeadlineUrgencyCalculatorTest {

    private Task createTask(long id, double arrivalTime, double executionTime, double deadline) {
        return new Task(id, 5, arrivalTime, executionTime, deadline);
    }

    private Task createTask(long id, int priority, double arrivalTime, double executionTime, double deadline) {
        return new Task(id, priority, arrivalTime, executionTime, deadline);
    }

    @Nested
    @DisplayName("Slack and Urgency Regimes Tests")
    class UrgencyRegimeTests {

        private final DeadlineUrgencyCalculator calc = new DeadlineUrgencyCalculator();

        @Test
        @DisplayName("Large positive slack produces low urgency")
        void testLargeSlackLowUrgency() {
            // arrival=0, exec=10, deadline=210, at t=0 -> D=210, slack = 210 - 0 - 10 = 200
            // Omega = (1000 / 201) * (1 + 10 / 211) ~ 5.21
            Task task = createTask(1, 0.0, 10.0, 210.0);
            double slack = calc.calculateSlack(task, 0.0);
            double urgency = calc.calculateUrgency(task, 0.0);

            assertEquals(200.0, slack, 1e-9);
            assertTrue(urgency < 10.0, "Large slack must produce low urgency");
            assertTrue(urgency > 0.0, "Urgency must be strictly positive");
            assertFalse(calc.isCritical(task, 0.0));
            assertFalse(calc.isOverdue(task, 0.0));
        }

        @Test
        @DisplayName("Small positive slack produces moderate/high urgency")
        void testSmallPositiveSlack() {
            // arrival=0, exec=10, deadline=20, at t=0 -> D=20, slack = 10
            // Omega = (1000 / 11) * (1 + 10 / 21) = 90.909 * 1.4762 ~ 134.20
            Task task = createTask(1, 0.0, 10.0, 20.0);
            double slack = calc.calculateSlack(task, 0.0);
            double urgency = calc.calculateUrgency(task, 0.0);

            assertEquals(10.0, slack, 1e-9);
            assertEquals(1000.0 / 11.0 * (1.0 + 10.0 / 21.0), urgency, 1e-6);
            assertTrue(urgency > 100.0);
            assertFalse(calc.isCritical(task, 0.0));
        }

        @Test
        @DisplayName("Zero slack boundary yields high critical urgency without division by zero")
        void testZeroSlackBoundary() {
            // arrival=0, exec=10, deadline=10, at t=0 -> D=10, slack = 0.0
            // Omega = (1000 / 1) * (1 + 10 / 11) ~ 1909.09
            Task task = createTask(1, 0.0, 10.0, 10.0);
            double slack = calc.calculateSlack(task, 0.0);
            double urgency = calc.calculateUrgency(task, 0.0);

            assertEquals(0.0, slack, 1e-9);
            assertEquals(1000.0 * (1.0 + 10.0 / 11.0), urgency, 1e-6);
            assertTrue(calc.isCritical(task, 0.0), "Zero slack task is critical");
            assertFalse(calc.isOverdue(task, 0.0), "Task at boundary is not yet overdue");
        }

        @Test
        @DisplayName("Negative slack yields maximum effective slack urgency with denominator safety")
        void testNegativeSlackInfeasible() {
            // arrival=0, exec=10, deadline=15, at t=10 -> D=5, slack = 15 - 10 - 10 = -5.0
            // EffectiveSlack = 0 -> (1000 / 1) * (1 + 10 / 6) = 1000 * 2.6667 ~ 2666.67
            Task task = createTask(1, 0.0, 10.0, 15.0);
            double slack = calc.calculateSlack(task, 10.0);
            double urgency = calc.calculateUrgency(task, 10.0);

            assertEquals(-5.0, slack, 1e-9);
            assertEquals(1000.0 * (1.0 + 10.0 / 6.0), urgency, 1e-6);
            assertTrue(calc.isCritical(task, 10.0));
            assertFalse(calc.isOverdue(task, 10.0), "At t=10, deadline 15 has not passed yet");
        }

        @Test
        @DisplayName("Overdue task (currentTime >= deadline) uses D_effective guard safely")
        void testOverdueTask() {
            // arrival=0, exec=5, deadline=10, at t=12 -> D = -2 -> D_effective = 0
            // EffectiveSlack = 0 -> (1000 / 1) * (1 + 5 / 1) = 6000.0
            Task task = createTask(1, 0.0, 5.0, 10.0);
            double urgency = calc.calculateUrgency(task, 12.0);

            assertEquals(6000.0, urgency, 1e-6);
            assertTrue(calc.isCritical(task, 12.0));
            assertTrue(calc.isOverdue(task, 12.0));
        }
    }

    @Nested
    @DisplayName("Execution-Time Awareness Tests")
    class ExecutionTimeAwarenessTests {

        private final DeadlineUrgencyCalculator calc = new DeadlineUrgencyCalculator();

        @Test
        @DisplayName("With identical slack, a task with longer execution duration experiences higher urgency")
        void testExecutionTimeInfluenceOnUrgency() {
            // Both tasks have remaining slack = 20.0 seconds at t = 0
            // Task Short: exec = 5.0, deadline = 25.0 -> slack = 20.0, D = 25
            // Task Long: exec = 40.0, deadline = 60.0 -> slack = 20.0, D = 60
            Task shortTask = createTask(1, 5, 0.0, 5.0, 25.0);
            Task longTask = createTask(2, 5, 0.0, 40.0, 60.0);

            assertEquals(20.0, calc.calculateSlack(shortTask, 0.0), 1e-9);
            assertEquals(20.0, calc.calculateSlack(longTask, 0.0), 1e-9);

            double shortUrgency = calc.calculateUrgency(shortTask, 0.0);
            double longUrgency = calc.calculateUrgency(longTask, 0.0);

            assertTrue(longUrgency > shortUrgency,
                "Longer task with same slack must exhibit greater urgency");
        }

        @Test
        @DisplayName("Overloaded calculateUrgency with remainingExecutionTime works correctly for preemption")
        void testCalculateUrgencyWithRemainingBurst() {
            Task task = createTask(1, 0.0, 10.0, 30.0);
            // At t = 3, remaining burst = 7.0 -> D = 27, slack = 27 - 7 = 20
            double urgencyWithRemaining = calc.calculateUrgency(task, 3.0, 7.0);
            double expectedSlackTerm = 1000.0 / (20.0 + 1.0);
            double expectedBurstFactor = 1.0 + (7.0 / (27.0 + 1.0));
            double expectedUrgency = expectedSlackTerm * expectedBurstFactor;
            assertEquals(expectedUrgency, urgencyWithRemaining, 1e-6);
        }
    }
}
