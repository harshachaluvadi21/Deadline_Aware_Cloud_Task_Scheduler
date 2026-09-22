package scheduler.experiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import scheduler.model.Task;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Fairness & Reproducibility Tests")
class FairnessAndReproducibilityTest {

    @Test
    @DisplayName("Both schedulers receive bitwise-identical input task specifications")
    void testIdenticalInputWorkloadGuarantees(@TempDir Path tempDir) throws IOException {
        ExperimentConfig config = ExperimentConfig.of(
                WorkloadScenarioType.NORMAL_LOAD,
                15,
                1001L,
                4,
                tempDir
        );

        List<Task> generated = WorkloadGenerator.generateWorkload(config);
        List<Task> cloneBaseline = WorkloadGenerator.cloneWorkload(generated);
        List<Task> cloneProposed = WorkloadGenerator.cloneWorkload(generated);

        assertEquals(generated.size(), cloneBaseline.size());
        assertEquals(generated.size(), cloneProposed.size());

        for (int i = 0; i < generated.size(); i++) {
            Task gen = generated.get(i);
            Task base = cloneBaseline.get(i);
            Task prop = cloneProposed.get(i);

            // Verify independent instances
            assertNotSame(base, prop);
            assertNotSame(gen, base);
            assertNotSame(gen, prop);

            // Verify identical parameters
            assertEquals(gen.getTaskId(), base.getTaskId());
            assertEquals(gen.getTaskId(), prop.getTaskId());

            assertEquals(gen.getPriority(), base.getPriority());
            assertEquals(gen.getPriority(), prop.getPriority());

            assertEquals(gen.getArrivalTime(), base.getArrivalTime(), 1e-6);
            assertEquals(gen.getArrivalTime(), prop.getArrivalTime(), 1e-6);

            assertEquals(gen.getExecutionTime(), base.getExecutionTime(), 1e-6);
            assertEquals(gen.getExecutionTime(), prop.getExecutionTime(), 1e-6);

            assertEquals(gen.getDeadline(), base.getDeadline(), 1e-6);
            assertEquals(gen.getDeadline(), prop.getDeadline(), 1e-6);
        }
    }

    @Test
    @DisplayName("Identical seeds produce bitwise-identical experimental outcomes across repeat executions")
    void testEndToEndReproducibility(@TempDir Path tempDir1, @TempDir Path tempDir2) throws IOException {
        ExperimentConfig config1 = ExperimentConfig.of(WorkloadScenarioType.NORMAL_LOAD, 10, 1001L, 4, tempDir1);
        ExperimentConfig config2 = ExperimentConfig.of(WorkloadScenarioType.NORMAL_LOAD, 10, 1001L, 4, tempDir2);

        ExperimentRunner.ExperimentPair pair1 = ExperimentRunner.runExperiment(config1);
        ExperimentRunner.ExperimentPair pair2 = ExperimentRunner.runExperiment(config2);

        // Baseline reproducibility
        assertEquals(pair1.baseline().makespan(), pair2.baseline().makespan(), 1e-6);
        assertEquals(pair1.baseline().averageWaitingTime(), pair2.baseline().averageWaitingTime(), 1e-6);
        assertEquals(pair1.baseline().resourceUtilization(), pair2.baseline().resourceUtilization(), 1e-6);
        assertEquals(pair1.baseline().deadlineMissRate(), pair2.baseline().deadlineMissRate(), 1e-6);

        // Proposed reproducibility
        assertEquals(pair1.proposed().makespan(), pair2.proposed().makespan(), 1e-6);
        assertEquals(pair1.proposed().averageWaitingTime(), pair2.proposed().averageWaitingTime(), 1e-6);
        assertEquals(pair1.proposed().resourceUtilization(), pair2.proposed().resourceUtilization(), 1e-6);
        assertEquals(pair1.proposed().deadlineMissRate(), pair2.proposed().deadlineMissRate(), 1e-6);
    }
}
