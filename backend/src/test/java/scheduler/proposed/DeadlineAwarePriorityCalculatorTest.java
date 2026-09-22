package scheduler.proposed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import scheduler.model.Task;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DeadlineAwarePriorityCalculator} validating:
 * - Waiting factor saturation
 * - Weighted score combination
 * - Boundedness & numerical stability
 * - Trade-off between base priority and deadline urgency
 * - Dynamic score evolution over time
 */
class DeadlineAwarePriorityCalculatorTest {

    private Task createTask(long id, int priority, double arrivalTime, double executionTime, double deadline) {
        return new Task(id, priority, arrivalTime, executionTime, deadline);
    }

    @Nested
    @DisplayName("Waiting Factor & Saturation Tests")
    class WaitingFactorTests {

        private final DeadlineAwarePriorityCalculator calc = new DeadlineAwarePriorityCalculator();

        @Test
        @DisplayName("Newly arrived task (wait=0) has waitingFactor = 0.0")
        void testNewlyArrivedWaitingFactor() {
            Task task = createTask(1, 5, 10.0, 5.0, 50.0);
            var breakdown = calc.calculateScoreBreakdown(task, 10.0);

            assertEquals(0.0, breakdown.waitingFactor(), 1e-9);
        }

        @Test
        @DisplayName("Task waiting for tau (50s) has waitingFactor = 0.5")
        void testHalfSaturationWaitingFactor() {
            Task task = createTask(1, 5, 0.0, 5.0, 200.0);
            var breakdown = calc.calculateScoreBreakdown(task, 50.0);

            // 50 / (50 + 50) = 0.5
            assertEquals(0.5, breakdown.waitingFactor(), 1e-9);
        }

        @Test
        @DisplayName("Long-waiting task approaches 1.0 asymptotically without exceeding 1.0")
        void testLongWaitSaturation() {
            Task task = createTask(1, 5, 0.0, 5.0, 1000.0);
            var breakdown = calc.calculateScoreBreakdown(task, 450.0);

            // 450 / (450 + 50) = 0.90
            assertEquals(0.90, breakdown.waitingFactor(), 1e-9);
            assertTrue(breakdown.waitingFactor() < 1.0);
        }
    }

    @Nested
    @DisplayName("Score Combination & Stability Tests")
    class CombinationTests {

        private final DeadlineAwarePriorityCalculator calc = new DeadlineAwarePriorityCalculator();

        @Test
        @DisplayName("Verifies exact weighted combination: 0.35 * P + 0.50 * U + 0.15 * W")
        void testExactWeightedCombination() {
            // Priority = 10 -> P_norm = 1.0
            // Arrival = 0, at t = 50 -> wait = 50 -> W = 50/(50+50) = 0.5
            // Exec = 10, Deadline = 60, at t = 50 -> slack = 60 - 50 - 10 = 0.0 -> U = 1.0
            Task task = createTask(1, 10, 0.0, 10.0, 60.0);
            var breakdown = calc.calculateScoreBreakdown(task, 50.0);

            assertEquals(1.0, breakdown.basePriorityNormalized(), 1e-9);
            assertEquals(1.0, breakdown.deadlineUrgency(), 1e-9);
            assertEquals(0.5, breakdown.waitingFactor(), 1e-9);

            // Expected score = 0.35 * 1.0 + 0.50 * 1.0 + 0.15 * 0.5 = 0.35 + 0.50 + 0.075 = 0.925
            assertEquals(0.925, breakdown.finalScore(), 1e-9);
        }

        @Test
        @DisplayName("Verifies bounded, non-NaN, non-infinite outputs across extreme inputs")
        void testNumericalStability() {
            Task overdueTask = createTask(1, 1, 0.0, 100.0, 5.0); // already overdue
            Task distantTask = createTask(2, 10, 0.0, 0.1, 10000.0); // huge slack

            var b1 = calc.calculateScoreBreakdown(overdueTask, 50.0);
            var b2 = calc.calculateScoreBreakdown(distantTask, 0.0);

            assertTrue(Double.isFinite(b1.finalScore()));
            assertTrue(Double.isFinite(b2.finalScore()));
            assertFalse(Double.isNaN(b1.finalScore()));
            assertFalse(Double.isNaN(b2.finalScore()));
            assertTrue(b1.finalScore() >= 0.0 && b1.finalScore() <= 1.0);
            assertTrue(b2.finalScore() >= 0.0 && b2.finalScore() <= 1.0);
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
        @DisplayName("Verifies trade-off between high base priority and urgent deadline follows formula")
        void testPriorityVsUrgencyTradeOff() {
            // Task HighBase: Base priority 10 (P_norm = 1.0), but relaxed deadline (slack = 200 -> U ~ 0.000045)
            //   Wait at t=0 is 0 -> W = 0
            //   Score ~ 0.35 * 1.0 + 0.50 * 0.0 + 0.15 * 0.0 = 0.35
            Task highBase = createTask(1, 10, 0.0, 10.0, 210.0);

            // Task Urgent: Lower base priority 2 (P_norm = 0.2), but very urgent (slack = 0 -> U = 1.0)
            //   Score ~ 0.35 * 0.2 + 0.50 * 1.0 + 0.15 * 0.0 = 0.07 + 0.50 = 0.57
            Task urgent = createTask(2, 2, 0.0, 10.0, 10.0);

            double scoreHighBase = calc.calculateFinalScore(highBase, 0.0);
            double scoreUrgent = calc.calculateFinalScore(urgent, 0.0);

            // Urgent task scores higher because urgency weight (0.50) outweighs base priority difference (0.35 * 0.8 = 0.28)
            assertTrue(scoreUrgent > scoreHighBase,
                "Urgent task (score=" + scoreUrgent + ") should score higher than relaxed task (score=" + scoreHighBase + ")");
            assertEquals(0.35, scoreHighBase, 0.01);
            assertEquals(0.57, scoreUrgent, 0.01);
        }

        @Test
        @DisplayName("Verifies high base priority wins when deadlines are similarly non-urgent")
        void testBasePriorityWinsWhenUrgencySimilar() {
            // Both tasks have comfortable slack = 100.0 (U ~ 0.0067)
            Task taskP10 = createTask(1, 10, 0.0, 10.0, 110.0);
            Task taskP2 = createTask(2, 2, 0.0, 10.0, 110.0);

            double scoreP10 = calc.calculateFinalScore(taskP10, 0.0);
            double scoreP2 = calc.calculateFinalScore(taskP2, 0.0);

            assertTrue(scoreP10 > scoreP2, "When urgency is equal/low, higher base priority must dominate");
        }

        @Test
        @DisplayName("Verifies dynamic score increases for the same task as simulation time advances")
        void testDynamicScoreEvolution() {
            // Task arrives at t=0, exec=10, deadline=30
            Task task = createTask(1, 5, 0.0, 10.0, 30.0);

            // At t=0: slack = 20, wait = 0
            double scoreAt0 = calc.calculateFinalScore(task, 0.0);

            // At t=10: slack = 10, wait = 10
            double scoreAt10 = calc.calculateFinalScore(task, 10.0);

            // At t=20: slack = 0 (critical), wait = 20
            double scoreAt20 = calc.calculateFinalScore(task, 20.0);

            assertTrue(scoreAt10 > scoreAt0, "Score must increase as slack decreases and wait increases");
            assertTrue(scoreAt20 > scoreAt10, "Score must reach near-peak as task becomes critical");
        }
    }
}
