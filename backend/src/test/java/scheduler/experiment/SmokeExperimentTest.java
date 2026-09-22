package scheduler.experiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SmokeExperiment Tests")
class SmokeExperimentTest {

    @Test
    @DisplayName("5-task smoke experiment executes both schedulers and generates all required artifacts")
    void testSmokeExperimentExecution(@TempDir Path tempDir) throws IOException {
        ExperimentRunner.ExperimentPair pair = ExperimentBatchRunner.runSmokeExperiment(tempDir);

        assertNotNull(pair);
        assertNotNull(pair.baseline());
        assertNotNull(pair.proposed());

        // Validate Baseline metrics
        ExperimentResult base = pair.baseline();
        assertEquals(5, base.completedTaskCount());
        assertEquals("BASELINE_IEEE_PRIORITY", base.schedulerName());
        assertTrue(base.makespan() > 0.0, "Makespan must be positive");
        assertTrue(base.averageWaitingTime() >= 0.0, "Waiting time must be non-negative");
        assertTrue(base.averageTurnaroundTime() >= 0.0, "Turnaround time must be non-negative");
        assertTrue(base.throughput() > 0.0, "Throughput must be positive");
        assertTrue(base.resourceUtilization() >= 0.0 && base.resourceUtilization() <= 100.0,
                "Resource utilization must be within [0, 100]");
        assertTrue(base.deadlineMissRate() >= 0.0 && base.deadlineMissRate() <= 1.0,
                "Deadline miss rate must be within [0, 1]");

        // Validate Proposed metrics
        ExperimentResult prop = pair.proposed();
        assertEquals(5, prop.completedTaskCount());
        assertEquals("PROPOSED_DEADLINE_AWARE", prop.schedulerName());
        assertTrue(prop.makespan() > 0.0, "Makespan must be positive");
        assertTrue(prop.averageWaitingTime() >= 0.0, "Waiting time must be non-negative");
        assertTrue(prop.averageTurnaroundTime() >= 0.0, "Turnaround time must be non-negative");
        assertTrue(prop.throughput() > 0.0, "Throughput must be positive");
        assertTrue(prop.resourceUtilization() >= 0.0 && prop.resourceUtilization() <= 100.0,
                "Resource utilization must be within [0, 100]");
        assertTrue(prop.deadlineMissRate() >= 0.0 && prop.deadlineMissRate() <= 1.0,
                "Deadline miss rate must be within [0, 1]");

        // Validate generated files
        Path workloadFile = tempDir.resolve("workloads").resolve("normal_load_5_seed1001.csv");
        assertTrue(Files.exists(workloadFile), "Workload CSV must exist: " + workloadFile);
        List<String> workloadLines = Files.readAllLines(workloadFile);
        assertEquals(6, workloadLines.size(), "1 header + 5 task rows");

        Path manifestFile = tempDir.resolve("workload_manifest.csv");
        assertTrue(Files.exists(manifestFile), "Workload manifest must exist: " + manifestFile);
        List<String> manifestLines = Files.readAllLines(manifestFile);
        assertEquals(2, manifestLines.size(), "1 header + 1 manifest row");
        assertTrue(manifestLines.get(1).contains("NORMAL_LOAD,5,1001,normal_load_5_seed1001.csv"));

        Path summaryFile = tempDir.resolve("smoke_summary.csv");
        assertTrue(Files.exists(summaryFile), "Summary CSV must exist: " + summaryFile);
        List<String> summaryLines = Files.readAllLines(summaryFile);
        assertEquals(3, summaryLines.size(), "1 header + 2 scheduler summary rows");

        Path detailsFile = tempDir.resolve("smoke_task_details.csv");
        assertTrue(Files.exists(detailsFile), "Task details CSV must exist: " + detailsFile);
        List<String> detailLines = Files.readAllLines(detailsFile);
        assertEquals(11, detailLines.size(), "1 header + 10 task rows (5 for baseline, 5 for proposed)");
    }
}
