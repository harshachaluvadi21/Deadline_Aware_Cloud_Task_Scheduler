package scheduler.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 4 Comprehensive Unit Tests for {@link Task} and {@link TaskStatus}.
 * Verifies construction, validation, lifecycle state transitions, timing rules,
 * deadline semantics, and immutability guarantees.
 */
public class TaskModelTest {

    @Nested
    @DisplayName("Task Construction & Validation Tests")
    class ConstructionAndValidationTests {

        @Test
        @DisplayName("1. Valid task creation succeeds with correct properties")
        void testValidTaskCreation() {
            Task task = new Task(101, 5, 2.5, 10.0, 25.0);

            assertEquals(101, task.getTaskId());
            assertEquals(5, task.getPriority());
            assertEquals(2.5, task.getArrivalTime(), 0.001);
            assertEquals(10.0, task.getExecutionTime(), 0.001);
            assertEquals(25.0, task.getDeadline(), 0.001);
            assertEquals(TaskStatus.SUBMITTED, task.getStatus());
            assertEquals(-1.0, task.getStartTime(), 0.001);
            assertEquals(-1.0, task.getCompletionTime(), 0.001);
            assertEquals(-1, task.getAllocatedVmId());
            assertEquals(0.0, task.getDynamicPriority(), 0.001);
        }

        @Test
        @DisplayName("2. Invalid task ID (negative) throws IllegalArgumentException")
        void testInvalidTaskId() {
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Task(-1, 5, 0.0, 10.0, 20.0)
            );
            assertTrue(ex.getMessage().contains("Task ID must be non-negative"));
        }

        @Test
        @DisplayName("3. Invalid priority throws IllegalArgumentException")
        void testInvalidPriority() {
            // Below minimum priority (1)
            assertThrows(IllegalArgumentException.class, () -> new Task(1, 0, 0.0, 10.0, 20.0));

            // Above maximum priority (10)
            assertThrows(IllegalArgumentException.class, () -> new Task(1, 11, 0.0, 10.0, 20.0));

            // Custom priority bounds validation
            assertThrows(IllegalArgumentException.class, () -> new Task(1, 4, 0.0, 10.0, 20.0, 1, 3));
            assertThrows(IllegalArgumentException.class, () -> new Task(1, 2, 0.0, 10.0, 20.0, 5, 3));
        }

        @Test
        @DisplayName("4. Negative arrival time throws IllegalArgumentException")
        void testNegativeArrivalTime() {
            assertThrows(IllegalArgumentException.class, () -> new Task(1, 5, -0.1, 10.0, 20.0));
            assertThrows(IllegalArgumentException.class, () -> new Task(1, 5, Double.NaN, 10.0, 20.0));
            assertThrows(IllegalArgumentException.class, () -> new Task(1, 5, Double.NEGATIVE_INFINITY, 10.0, 20.0));
        }

        @Test
        @DisplayName("5. Zero or negative execution time throws IllegalArgumentException")
        void testZeroOrNegativeExecutionTime() {
            assertThrows(IllegalArgumentException.class, () -> new Task(1, 5, 0.0, 0.0, 20.0));
            assertThrows(IllegalArgumentException.class, () -> new Task(1, 5, 0.0, -5.0, 20.0));
            assertThrows(IllegalArgumentException.class, () -> new Task(1, 5, 0.0, Double.NaN, 20.0));
        }

        @Test
        @DisplayName("6. Deadline before arrival time throws IllegalArgumentException")
        void testDeadlineBeforeArrival() {
            // Deadline earlier than arrival time
            assertThrows(IllegalArgumentException.class, () -> new Task(1, 5, 10.0, 5.0, 9.9));

            // Deadline equal to arrival time is valid boundary case
            assertDoesNotThrow(() -> new Task(2, 5, 10.0, 5.0, 10.0));
        }
    }

    @Nested
    @DisplayName("Lifecycle State Transition Tests")
    class LifecycleAndTransitionTests {

        @Test
        @DisplayName("7. Initial status must strictly be SUBMITTED")
        void testInitialStatus() {
            Task task = new Task(1, 3, 0.0, 5.0, 15.0);
            assertEquals(TaskStatus.SUBMITTED, task.getStatus());
            assertFalse(task.getStatus().isTerminal());
        }

        @Test
        @DisplayName("8. Valid state transitions: SUBMITTED -> READY -> RUNNING -> COMPLETED")
        void testValidStateTransitions() {
            Task task = new Task(1, 5, 0.0, 10.0, 20.0);

            // SUBMITTED -> READY
            task.markReady();
            assertEquals(TaskStatus.READY, task.getStatus());

            // READY -> RUNNING
            task.markRunning(2.0, 0);
            assertEquals(TaskStatus.RUNNING, task.getStatus());
            assertEquals(2.0, task.getStartTime(), 0.001);
            assertEquals(0, task.getAllocatedVmId());
            assertEquals(2.0, task.getWaitingTime(), 0.001);

            // RUNNING -> COMPLETED
            task.markCompleted(12.0);
            assertEquals(TaskStatus.COMPLETED, task.getStatus());
            assertEquals(12.0, task.getCompletionTime(), 0.001);
            assertEquals(12.0, task.getTurnaroundTime(), 0.001);
            assertFalse(task.isDeadlineMissed());
            assertTrue(task.getStatus().isTerminal());
        }

        @Test
        @DisplayName("9. Invalid state transitions throw IllegalStateException")
        void testInvalidStateTransitions() {
            Task task = new Task(1, 5, 0.0, 10.0, 20.0);

            // Invalid: SUBMITTED directly to RUNNING
            assertThrows(IllegalStateException.class, () -> task.markRunning(0.0, 1));

            // Invalid: SUBMITTED directly to COMPLETED
            assertThrows(IllegalStateException.class, () -> task.markCompleted(10.0));

            // Invalid: SUBMITTED directly to MISSED_DEADLINE
            assertThrows(IllegalStateException.class, () -> task.markDeadlineMissed(25.0));

            task.markReady();

            // Invalid: READY directly to COMPLETED
            assertThrows(IllegalStateException.class, () -> task.markCompleted(10.0));

            // Invalid: READY directly to READY again
            assertThrows(IllegalStateException.class, task::markReady);

            task.markRunning(1.0, 0);

            // Invalid: RUNNING back to READY
            assertThrows(IllegalStateException.class, task::markReady);

            // Invalid: RUNNING back to SUBMITTED
            assertFalse(task.getStatus().canTransitionTo(TaskStatus.SUBMITTED));

            task.markCompleted(15.0);

            // Invalid: Terminal state COMPLETED cannot transition anywhere
            assertThrows(IllegalStateException.class, task::markReady);
            assertThrows(IllegalStateException.class, () -> task.markRunning(16.0, 1));
            assertThrows(IllegalStateException.class, () -> task.markCompleted(18.0));
            assertThrows(IllegalStateException.class, () -> task.markDeadlineMissed(25.0));
        }

        @Test
        @DisplayName("10. Start time validation (cannot be before arrival time, VM ID must be non-negative)")
        void testStartTimeValidation() {
            Task task = new Task(1, 5, 10.0, 5.0, 30.0);
            task.markReady();

            // Start time before arrival time (9.9 < 10.0)
            assertThrows(IllegalArgumentException.class, () -> task.markRunning(9.9, 0));

            // Negative VM ID
            assertThrows(IllegalArgumentException.class, () -> task.markRunning(10.0, -1));

            // Start time equal to arrival time is valid
            assertDoesNotThrow(() -> task.markRunning(10.0, 0));
            assertEquals(0.0, task.getWaitingTime(), 0.001);
        }

        @Test
        @DisplayName("11. Completion time validation (cannot be before start time)")
        void testCompletionTimeValidation() {
            Task task = new Task(1, 5, 0.0, 5.0, 20.0);
            task.markReady();
            task.markRunning(5.0, 0);

            // Completion time before start time (4.9 < 5.0)
            assertThrows(IllegalArgumentException.class, () -> task.markCompleted(4.9));
            assertThrows(IllegalArgumentException.class, () -> task.markDeadlineMissed(4.9));
            assertThrows(IllegalArgumentException.class, () -> task.markFinished(4.9));
        }

        @Test
        @DisplayName("12. Completion exactly at deadline is NOT a miss (status must be COMPLETED)")
        void testCompletionExactlyAtDeadline() {
            Task task = new Task(1, 5, 0.0, 10.0, 20.0);
            task.markReady();
            task.markRunning(10.0, 1);

            // Complete exactly at deadline = 20.0
            task.markCompleted(20.0);

            assertEquals(TaskStatus.COMPLETED, task.getStatus());
            assertEquals(20.0, task.getCompletionTime(), 0.001);
            assertFalse(task.isDeadlineMissed(), "Task completed exactly at deadline must not be counted as a miss");

            // Calling markDeadlineMissed on an on-time task throws IllegalStateException
            Task onTimeTask = new Task(2, 5, 0.0, 10.0, 20.0);
            onTimeTask.markReady();
            onTimeTask.markRunning(10.0, 1);
            assertThrows(IllegalStateException.class, () -> onTimeTask.markDeadlineMissed(20.0));
        }

        @Test
        @DisplayName("13. Completion after deadline strictly transitions to MISSED_DEADLINE")
        void testCompletionAfterDeadline() {
            Task task = new Task(1, 5, 0.0, 10.0, 20.0);
            task.markReady();
            task.markRunning(15.0, 2);

            // Attempting markCompleted when completionTime (25.0) > deadline (20.0) throws IllegalStateException
            assertThrows(IllegalStateException.class, () -> task.markCompleted(25.0));

            // Calling markDeadlineMissed properly transitions to MISSED_DEADLINE
            task.markDeadlineMissed(25.0);

            assertEquals(TaskStatus.MISSED_DEADLINE, task.getStatus());
            assertEquals(25.0, task.getCompletionTime(), 0.001);
            assertTrue(task.isDeadlineMissed(), "Task completing after deadline must report isDeadlineMissed = true");
            assertEquals(25.0, task.getTurnaroundTime(), 0.001);
            assertEquals(15.0, task.getWaitingTime(), 0.001);

            // Test convenience markFinished method
            Task autoTask = new Task(2, 5, 0.0, 10.0, 20.0);
            autoTask.markReady();
            autoTask.markRunning(12.0, 1);
            autoTask.markFinished(21.5);
            assertEquals(TaskStatus.MISSED_DEADLINE, autoTask.getStatus());
            assertTrue(autoTask.isDeadlineMissed());
        }

        @Test
        @DisplayName("14. Runtime field updates (dynamicPriority, allocatedVmId, timing metrics)")
        void testRuntimeFieldUpdates() {
            Task task = new Task(50, 4, 1.0, 8.0, 30.0);
            task.setDynamicPriority(0.8542);
            assertEquals(0.8542, task.getDynamicPriority(), 0.0001);

            assertThrows(IllegalArgumentException.class, () -> task.setDynamicPriority(Double.NaN));

            task.markReady();
            task.markRunning(3.0, 4);
            assertEquals(4, task.getAllocatedVmId());
            assertEquals(3.0, task.getStartTime(), 0.001);

            task.markCompleted(11.0);
            assertEquals(11.0, task.getCompletionTime(), 0.001);
            assertEquals(2.0, task.getWaitingTime(), 0.001);
            assertEquals(10.0, task.getTurnaroundTime(), 0.001);
        }

        @Test
        @DisplayName("15. Original task properties cannot be changed (immutability)")
        void testOriginalPropertiesImmutability() {
            Task task = new Task(99, 7, 5.0, 12.0, 40.0);

            task.markReady();
            task.markRunning(10.0, 3);
            task.setDynamicPriority(0.99);
            task.markCompleted(22.0);

            // Ensure initial definition fields remain completely unmodified
            assertEquals(99, task.getTaskId(), "Task ID must remain unchanged");
            assertEquals(7, task.getPriority(), "Base priority must remain unchanged");
            assertEquals(5.0, task.getArrivalTime(), 0.001, "Arrival time must remain unchanged");
            assertEquals(12.0, task.getExecutionTime(), 0.001, "Execution time must remain unchanged");
            assertEquals(40.0, task.getDeadline(), 0.001, "Deadline must remain unchanged");
        }
    }
}
