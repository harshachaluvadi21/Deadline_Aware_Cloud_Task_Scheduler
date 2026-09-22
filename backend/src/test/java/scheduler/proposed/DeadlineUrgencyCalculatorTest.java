package scheduler.proposed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import scheduler.model.Task;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DeadlineUrgencyCalculator} verifying slack time, urgency response,
 * critical/overdue distinctions, and execution-time awareness.
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
        @DisplayName("Large positive slack produces low urgency approaching 0.0")
        void testLargeSlackLowUrgency() {
            // arrival=0, exec=10, deadline=210, at t=0 -> slack = 210 - 0 - 10 = 200
            // exponent = -200 / (2 * 10) = -10 -> e^(-10) ~ 0.000045
            Task task = createTask(1, 0.0, 10.0, 210.0);
            double slack = calc.calculateSlack(task, 0.0);
            double urgency = calc.calculateUrgency(task, 0.0);

            assertEquals(200.0, slack, 1e-9);
            assertTrue(urgency < 0.001, "Large slack must produce near-zero urgency");
            assertTrue(urgency > 0.0, "Urgency must be strictly positive");
            assertFalse(calc.isCritical(task, 0.0));
            assertFalse(calc.isOverdue(task, 0.0));
        }

        @Test
        @DisplayName("Small positive slack produces moderate/high urgency")
        void testSmallPositiveSlack() {
            // arrival=0, exec=10, deadline=20, at t=0 -> slack = 20 - 0 - 10 = 10 (slack == exec)
            // exponent = -10 / (2 * 10) = -0.5 -> e^(-0.5) ~ 0.6065
            Task task = createTask(1, 0.0, 10.0, 20.0);
            double slack = calc.calculateSlack(task, 0.0);
            double urgency = calc.calculateUrgency(task, 0.0);

            assertEquals(10.0, slack, 1e-9);
            assertEquals(Math.exp(-0.5), urgency, 1e-6);
            assertTrue(urgency > 0.5 && urgency < 1.0);
            assertFalse(calc.isCritical(task, 0.0));
        }

        @Test
        @DisplayName("Zero slack (critical boundary) yields maximum urgency (1.0)")
        void testZeroSlackBoundary() {
            // arrival=0, exec=10, deadline=10, at t=0 -> slack = 10 - 0 - 10 = 0.0
            Task task = createTask(1, 0.0, 10.0, 10.0);
            double slack = calc.calculateSlack(task, 0.0);
            double urgency = calc.calculateUrgency(task, 0.0);

            assertEquals(0.0, slack, 1e-9);
            assertEquals(1.0, urgency, 1e-9, "Zero slack must evaluate to maximum urgency (1.0)");
            assertTrue(calc.isCritical(task, 0.0), "Zero slack task is critical");
            assertFalse(calc.isOverdue(task, 0.0), "Task at boundary is not yet overdue (deadline not elapsed)");
        }

        @Test
        @DisplayName("Negative slack (infeasible if started now) yields maximum urgency (1.0)")
        void testNegativeSlackInfeasible() {
            // arrival=0, exec=10, deadline=15, at t=10 -> slack = 15 - 10 - 10 = -5.0
            Task task = createTask(1, 0.0, 10.0, 15.0);
            double slack = calc.calculateSlack(task, 10.0);
            double urgency = calc.calculateUrgency(task, 10.0);

            assertEquals(-5.0, slack, 1e-9);
            assertEquals(1.0, urgency, 1e-9);
            assertTrue(calc.isCritical(task, 10.0));
            assertFalse(calc.isOverdue(task, 10.0), "At t=10, deadline 15 has not passed yet");
        }

        @Test
        @DisplayName("Overdue task (currentTime >= deadline) yields maximum urgency and isOverdue=true")
        void testOverdueTask() {
            // arrival=0, exec=5, deadline=10, at t=12 -> overdue
            Task task = createTask(1, 0.0, 5.0, 10.0);
            double urgency = calc.calculateUrgency(task, 12.0);

            assertEquals(1.0, urgency, 1e-9);
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
            // Task Short: exec = 5.0, deadline = 25.0 -> slack = 20.0
            //   exp(-20 / (2 * 5)) = exp(-2.0) ~ 0.1353
            // Task Long: exec = 40.0, deadline = 60.0 -> slack = 20.0
            //   exp(-20 / (2 * 40)) = exp(-0.25) ~ 0.7788
            Task shortTask = createTask(1, 5, 0.0, 5.0, 25.0);
            Task longTask = createTask(2, 5, 0.0, 40.0, 60.0);

            assertEquals(20.0, calc.calculateSlack(shortTask, 0.0), 1e-9);
            assertEquals(20.0, calc.calculateSlack(longTask, 0.0), 1e-9);

            double shortUrgency = calc.calculateUrgency(shortTask, 0.0);
            double longUrgency = calc.calculateUrgency(longTask, 0.0);

            assertTrue(longUrgency > shortUrgency,
                "Longer task with same slack must exhibit greater urgency (deadline pressure)");
            assertEquals(Math.exp(-2.0), shortUrgency, 1e-6);
            assertEquals(Math.exp(-0.25), longUrgency, 1e-6);
        }
    }
}
