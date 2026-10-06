package scheduler.experiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import scheduler.model.CloudVmSpec;
import scheduler.model.Task;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Production-path integration test verifying that {@link ExperimentRunner#runExperiment(ExperimentConfig)}
 * executes workloads through the discrete-event CloudSim Plus engine and derives benchmark metrics
 * exclusively from harvested CloudSim Cloudlet execution traces.
 */
@DisplayName("Production Path: ExperimentRunner CloudSim Integration Test")
public class ExperimentRunnerCloudSimIntegrationTest {

    @Test
    @DisplayName("Verify ExperimentRunner executes production path through CloudSim Plus for Baseline and Proposed")
    void testProductionExperimentPathThroughCloudSim(@TempDir Path tempDir) throws IOException {
        List<CloudVmSpec> vms = List.of(
                CloudVmSpec.of(0, 1000.0, 2048, 1000, 10000),
                CloudVmSpec.of(1, 1500.0, 4096, 1000, 10000)
        );

        ExperimentConfig config = new ExperimentConfig(
                WorkloadScenarioType.NORMAL_LOAD,
                5,
                42L,
                vms,
                1000.0,
                tempDir
        );

        // Call the REAL production experiment entry point
        ExperimentRunner.ExperimentPair pair = ExperimentRunner.runExperiment(config);

        assertNotNull(pair, "ExperimentPair must not be null");
        assertNotNull(pair.baseline(), "Baseline result must not be null");
        assertNotNull(pair.proposed(), "Proposed result must not be null");

        // 1. Verify Baseline executed through CloudSim Plus
        ExperimentResult baseResult = pair.baseline();
        assertEquals("BASELINE_IEEE_PRIORITY", baseResult.schedulerName());
        assertEquals(5, baseResult.taskCount());
        assertEquals(5, baseResult.completedTaskCount());
        assertEquals(5, baseResult.taskRecords().size());
        assertTrue(baseResult.makespan() > 0.0, "Makespan must be positive");
        assertTrue(baseResult.resourceUtilization() > 0.0 && baseResult.resourceUtilization() <= 100.0);

        for (TaskExecutionRecord rec : baseResult.taskRecords()) {
            assertTrue(rec.startTime() >= rec.arrivalTime(),
                    "CloudSim execution start time must be >= task arrival time");
            assertTrue(rec.completionTime() > rec.startTime(),
                    "CloudSim finish time must be strictly after start time");
            assertTrue(rec.assignedVmId() == 0 || rec.assignedVmId() == 1,
                    "Assigned VM must match provisioned VM topology");
            assertEquals(rec.completionTime() - rec.startTime(), rec.executionTime(), 1e-2,
                    "Execution duration must reflect VM execution");
        }

        // 2. Verify Proposed executed through CloudSim Plus
        ExperimentResult propResult = pair.proposed();
        assertEquals("PROPOSED_DEADLINE_AWARE", propResult.schedulerName());
        assertEquals(5, propResult.taskCount());
        assertEquals(5, propResult.completedTaskCount());
        assertEquals(5, propResult.taskRecords().size());
        assertTrue(propResult.makespan() > 0.0, "Makespan must be positive");
        assertTrue(propResult.resourceUtilization() > 0.0 && propResult.resourceUtilization() <= 100.0);

        for (TaskExecutionRecord rec : propResult.taskRecords()) {
            assertTrue(rec.startTime() >= rec.arrivalTime(),
                    "CloudSim execution start time must be >= task arrival time");
            assertTrue(rec.completionTime() > rec.startTime(),
                    "CloudSim finish time must be strictly after start time");
            assertTrue(rec.assignedVmId() == 0 || rec.assignedVmId() == 1,
                    "Assigned VM must match provisioned VM topology");
            assertEquals(rec.completionTime() - rec.startTime(), rec.executionTime(), 1e-2,
                    "Execution duration must reflect VM execution");
        }

        // 3. Verify Fairness: Identical task count, VM count, and workload file created
        assertEquals(baseResult.taskCount(), propResult.taskCount());
        assertEquals(baseResult.vmCount(), propResult.vmCount());
    }
}
