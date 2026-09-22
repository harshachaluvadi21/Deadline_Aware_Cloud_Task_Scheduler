package scheduler.experiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import scheduler.model.Task;
import scheduler.model.TaskStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WorkloadGenerator Tests")
class WorkloadGeneratorTest {

    @Test
    @DisplayName("Identical seeds produce bitwise-identical workloads")
    void testDeterministicGeneration() {
        List<Task> run1 = WorkloadGenerator.generateWorkload(WorkloadScenarioType.NORMAL_LOAD, 20, 1001L);
        List<Task> run2 = WorkloadGenerator.generateWorkload(WorkloadScenarioType.NORMAL_LOAD, 20, 1001L);

        assertEquals(20, run1.size());
        assertEquals(20, run2.size());

        for (int i = 0; i < 20; i++) {
            Task t1 = run1.get(i);
            Task t2 = run2.get(i);

            assertEquals(t1.getTaskId(), t2.getTaskId());
            assertEquals(t1.getPriority(), t2.getPriority());
            assertEquals(t1.getArrivalTime(), t2.getArrivalTime(), 1e-6);
            assertEquals(t1.getExecutionTime(), t2.getExecutionTime(), 1e-6);
            assertEquals(t1.getDeadline(), t2.getDeadline(), 1e-6);
        }
    }

    @Test
    @DisplayName("Different seeds produce distinct workloads")
    void testDistinctSeeds() {
        List<Task> run1 = WorkloadGenerator.generateWorkload(WorkloadScenarioType.NORMAL_LOAD, 10, 1001L);
        List<Task> run2 = WorkloadGenerator.generateWorkload(WorkloadScenarioType.NORMAL_LOAD, 10, 9999L);

        boolean foundDifference = false;
        for (int i = 0; i < 10; i++) {
            if (run1.get(i).getPriority() != run2.get(i).getPriority() ||
                Math.abs(run1.get(i).getExecutionTime() - run2.get(i).getExecutionTime()) > 1e-3) {
                foundDifference = true;
                break;
            }
        }
        assertTrue(foundDifference, "Different seeds should produce different workloads");
    }

    @Test
    @DisplayName("Task fields strictly respect valid domain bounds")
    void testParameterBounds() {
        for (WorkloadScenarioType scenario : WorkloadScenarioType.values()) {
            List<Task> tasks = WorkloadGenerator.generateWorkload(scenario, 30, scenario.getDefaultSeed());

            for (Task t : tasks) {
                assertTrue(t.getTaskId() >= 0, "Task ID non-negative");
                assertTrue(t.getPriority() >= 1 && t.getPriority() <= 10, "Priority in [1, 10]");
                assertTrue(t.getArrivalTime() >= 0.0, "Arrival >= 0");
                assertTrue(t.getExecutionTime() > 0.0, "Execution time > 0");
                assertTrue(t.getDeadline() >= t.getArrivalTime() + t.getExecutionTime(),
                        "Deadline must accommodate arrival + execution time");
                assertEquals(TaskStatus.SUBMITTED, t.getStatus());
            }
        }
    }

    @Test
    @DisplayName("cloneWorkload creates deep unmutated copies")
    void testCloneWorkload() {
        List<Task> originals = WorkloadGenerator.generateWorkload(WorkloadScenarioType.NORMAL_LOAD, 5, 1001L);
        List<Task> clones = WorkloadGenerator.cloneWorkload(originals);

        assertEquals(originals.size(), clones.size());

        for (int i = 0; i < originals.size(); i++) {
            Task orig = originals.get(i);
            Task clone = clones.get(i);

            assertNotSame(orig, clone, "Must be distinct object references");
            assertEquals(orig.getTaskId(), clone.getTaskId());
            assertEquals(orig.getPriority(), clone.getPriority());
            assertEquals(orig.getArrivalTime(), clone.getArrivalTime(), 1e-6);
            assertEquals(orig.getExecutionTime(), clone.getExecutionTime(), 1e-6);
            assertEquals(orig.getDeadline(), clone.getDeadline(), 1e-6);
            assertEquals(TaskStatus.SUBMITTED, clone.getStatus());
        }

        // Mutating a clone does not alter original
        Task clone0 = clones.get(0);
        clone0.markReady();
        clone0.markRunning(10.0, 1L);
        clone0.markFinished(11.0); // before deadline, so COMPLETED

        assertTrue(clone0.getStatus().isTerminal());
        assertEquals(TaskStatus.SUBMITTED, originals.get(0).getStatus());
    }

    @Test
    @DisplayName("Invalid task count throws IllegalArgumentException")
    void testInvalidTaskCount() {
        assertThrows(IllegalArgumentException.class, () ->
                WorkloadGenerator.generateWorkload(WorkloadScenarioType.NORMAL_LOAD, 0, 1001L));
        assertThrows(IllegalArgumentException.class, () ->
                WorkloadGenerator.generateWorkload(WorkloadScenarioType.NORMAL_LOAD, -5, 1001L));
    }
}
