package scheduler.proposed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import scheduler.model.Task;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DeadlineAwarePriorityCalculator} validating:
 * - Exact dynamic priority calculation: P(t) = 100 * P_base + Omega(t) + 10 * W(t)
 * - Waiting factor aging
 * - Trade-off between base priority and deadline urgency
 * - Monotonic dynamic priority evolution over time
 */
class DeadlineAwarePriorityCalculatorTest {

    private Task createTask(long id, int priority, double arrivalTime, double executionTime, double deadline) {
        return new Task(id, priority, arrivalTime, executionTime, deadline);
    }

    @Nested
    @DisplayName("Dynamic Priority Calculation Tests")
    class DynamicPriorityTests {

        private final DeadlineAwarePriorityCalculator calc = new DeadlineAwarePriorityCalculator();

        @Test
        @DisplayName("Verifies exact formulation: P(t) = 100 * P_base + Omega(t) + 10 * W(t)")
        void testExactDynamicPriorityCalculation() {
            // Priority = 8, Arrival = 0, at t = 50 -> wait = 50 -> 10 * W = 500
            // Exec = 10, Deadline = 60, at t = 50 -> D = 10, slack = 60 - 50 - 10 = 0.0 -> EffectiveSlack = 0
            // Omega = (1000 / (0 + 1)) * (1 + 10 / (10 + 1)) = 1000 * (1 + 10/11) = 1909.0909
            // Expected P(t) = 100 * 8 + 1909.0909 + 500 = 800 + 1909.0909 + 500 = 3209.0909
            Task task = createTask(1, 8, 0.0, 10.0, 60.0);
            var breakdown = calc.calculateScoreBreakdown(task, 50.0);

            assertEquals(8.0, breakdown.basePriority(), 1e-9);
            assertEquals(1000.0 * (1.0 + 10.0 / 11.0), breakdown.deadlineUrgency(), 1e-4);
            assertEquals(50.0, breakdown.waitingTime(), 1e-9);

            double expectedScore = (100.0 * 8) + (1000.0 * (1.0 + 10.0 / 11.0)) + (10.0 * 50.0);
            assertEquals(expectedScore, breakdown.finalScore(), 1e-4);
        }

        @Test
        @DisplayName("Verifies finite, non-NaN, positive outputs across extreme inputs")
        void testNumericalStability() {
            Task overdueTask = createTask(1, 1, 0.0, 100.0, 5.0); // already overdue
            Task distantTask = createTask(2, 10, 0.0, 0.1, 10000.0); // huge slack

            var b1 = calc.calculateScoreBreakdown(overdueTask, 50.0);
            var b2 = calc.calculateScoreBreakdown(distantTask, 0.0);

            assertTrue(Double.isFinite(b1.finalScore()));
            assertTrue(Double.isFinite(b2.finalScore()));
            assertFalse(Double.isNaN(b1.finalScore()));
            assertFalse(Double.isNaN(b2.finalScore()));
            assertTrue(b1.finalScore() > 0.0);
            assertTrue(b2.finalScore() > 0.0);
        }

        @Test
        @DisplayName("Deterministic repeated calculation produces bitwise identical results")
        void testDeterministicRepeatability() {
            Task task = createTask(1, 7, 5.0, 12.0, 45.0);

            double score1 = calc.calculateFinalScore(task, 15.0);
            double score2 = calc.calculateFinalScore(task, 15.0);

            assertEquals(score1, score2, 0.0);
        }
    }

    @Nested
    @DisplayName("Trade-Off & Dynamic Evolution Tests")
    class TradeOffTests {

        private final DeadlineAwarePriorityCalculator calc = new DeadlineAwarePriorityCalculator();

        @Test
        @DisplayName("Verifies urgent task with lower base priority overtakes relaxed task with higher base priority")
        void testPriorityVsUrgencyTradeOff() {
            // Task HighBase: Base priority 10, relaxed deadline (slack = 200, D = 210)
            //   100 * 10 = 1000
            //   Omega = (1000 / 201) * (1 + 10 / 211) ~ 5.21
            //   Score ~ 1005.21
            Task highBase = createTask(1, 10, 0.0, 10.0, 210.0);

            // Task Urgent: Lower base priority 2, but critical deadline (slack = 0, D = 10)
            //   100 * 2 = 200
            //   Omega = (1000 / 1) * (1 + 10 / 11) ~ 1909.09
            //   Score ~ 2109.09
            Task urgent = createTask(2, 2, 0.0, 10.0, 10.0);

            double scoreHighBase = calc.calculateFinalScore(highBase, 0.0);
            double scoreUrgent = calc.calculateFinalScore(urgent, 0.0);

            assertTrue(scoreUrgent > scoreHighBase,
                "Urgent task (score=" + scoreUrgent + ") must score higher than relaxed task (score=" + scoreHighBase + ")");
        }

        @Test
        @DisplayName("Verifies high base priority wins when deadlines are similarly non-urgent")
        void testBasePriorityWinsWhenUrgencySimilar() {
            // Both tasks have comfortable slack = 100.0
            Task taskP10 = createTask(1, 10, 0.0, 10.0, 110.0);
            Task taskP2 = createTask(2, 2, 0.0, 10.0, 110.0);

            double scoreP10 = calc.calculateFinalScore(taskP10, 0.0);
            double scoreP2 = calc.calculateFinalScore(taskP2, 0.0);

            assertTrue(scoreP10 > scoreP2, "When urgency is equal/low, higher base priority must dominate");
        }

        @Test
        @DisplayName("Verifies dynamic priority strictly increases for a waiting task as simulation time advances")
        void testDynamicPriorityMonotonicEvolution() {
            // Task arrives at t=0, exec=10, deadline=30
            Task task = createTask(1, 5, 0.0, 10.0, 30.0);

            // At t=0: slack = 20, wait = 0
            double priorityAt0 = calc.calculateFinalScore(task, 0.0);

            // At t=10: slack = 10, wait = 10
            double priorityAt10 = calc.calculateFinalScore(task, 10.0);

            // At t=20: slack = 0 (critical), wait = 20
            double priorityAt20 = calc.calculateFinalScore(task, 20.0);

            assertTrue(priorityAt10 > priorityAt0, "Priority must increase as slack decreases and wait increases");
            assertTrue(priorityAt20 > priorityAt10, "Priority must reach peak as task becomes critical");
        }

        @Test
        @DisplayName("Verifies dynamic priority calculation with remaining execution time for preempted tasks")
        void testDynamicPriorityWithRemainingExecutionTime() {
            // Task: Priority 3, Arr 0, Exec 10, Deadline 30
            Task task = createTask(1, 3, 0.0, 10.0, 30.0);
            // At t = 3, remaining burst = 7.0
            double pAt3WithRem7 = calc.calculateDynamicPriority(task, 3.0, 7.0);

            // D(3) = 27, B = 7, slack = 20, effSlack = 20, dEff = 27
            // Omega = (1000 / 21) * (1 + 7 / 28) = (1000 / 21) * 1.25 ~ 59.5238
            // P = 100 * 3 + 59.5238 + 10 * 3 = 300 + 59.5238 + 30 = 389.5238
            double expectedOmega = (1000.0 / 21.0) * (1.0 + 7.0 / 28.0);
            double expectedP = (100.0 * 3) + expectedOmega + (10.0 * 3.0);

            assertEquals(expectedP, pAt3WithRem7, 1e-4);
        }
    }
}
