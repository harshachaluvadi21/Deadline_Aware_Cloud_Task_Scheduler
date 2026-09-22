package scheduler.experiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import scheduler.model.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WorkloadSerializer Tests")
class WorkloadSerializerTest {

    @Test
    @DisplayName("Workload CSV round-trip preserves all task fields exactly")
    void testWorkloadCsvRoundTrip(@TempDir Path tempDir) throws IOException {
        List<Task> originalTasks = WorkloadGenerator.generateWorkload(WorkloadScenarioType.NORMAL_LOAD, 10, 1001L);
        Path csvPath = tempDir.resolve("workload.csv");

        WorkloadSerializer.writeWorkloadToCsv(originalTasks, csvPath);
        assertTrue(Files.exists(csvPath), "Workload CSV should exist");

        List<Task> loadedTasks = WorkloadSerializer.readWorkloadFromCsv(csvPath);
        assertEquals(originalTasks.size(), loadedTasks.size());

        for (int i = 0; i < originalTasks.size(); i++) {
            Task orig = originalTasks.get(i);
            Task loaded = loadedTasks.get(i);

            assertEquals(orig.getTaskId(), loaded.getTaskId());
            assertEquals(orig.getPriority(), loaded.getPriority());
            assertEquals(orig.getArrivalTime(), loaded.getArrivalTime(), 1e-2);
            assertEquals(orig.getExecutionTime(), loaded.getExecutionTime(), 1e-2);
            assertEquals(orig.getDeadline(), loaded.getDeadline(), 1e-2);
        }
    }

    @Test
    @DisplayName("Workload manifest records scenario, taskCount, seed, and filename")
    void testManifestRecording(@TempDir Path tempDir) throws IOException {
        Path manifestPath = tempDir.resolve("manifest.csv");

        WorkloadSerializer.WorkloadManifestEntry entry1 = new WorkloadSerializer.WorkloadManifestEntry(
                "NORMAL_LOAD", 20, 1001L, "normal_load_20_seed1001.csv"
        );
        WorkloadSerializer.recordManifest(manifestPath, entry1);

        WorkloadSerializer.WorkloadManifestEntry entry2 = new WorkloadSerializer.WorkloadManifestEntry(
                "HIGH_LOAD", 50, 2001L, "high_load_50_seed2001.csv"
        );
        WorkloadSerializer.recordManifest(manifestPath, entry2);

        // Attempt duplicate recording of entry1
        WorkloadSerializer.recordManifest(manifestPath, entry1);

        List<WorkloadSerializer.WorkloadManifestEntry> entries = WorkloadSerializer.readManifest(manifestPath);
        assertEquals(2, entries.size(), "Duplicate entry should not be appended");

        assertEquals("NORMAL_LOAD", entries.get(0).scenario());
        assertEquals(20, entries.get(0).taskCount());
        assertEquals(1001L, entries.get(0).seed());
        assertEquals("normal_load_20_seed1001.csv", entries.get(0).workloadFile());

        assertEquals("HIGH_LOAD", entries.get(1).scenario());
        assertEquals(50, entries.get(1).taskCount());
        assertEquals(2001L, entries.get(1).seed());
        assertEquals("high_load_50_seed2001.csv", entries.get(1).workloadFile());
    }

    @Test
    @DisplayName("Standard filename format adheres to specification")
    void testStandardFilename() {
        String filename = WorkloadSerializer.getWorkloadFileName(WorkloadScenarioType.NORMAL_LOAD, 20, 1001L);
        assertEquals("normal_load_20_seed1001.csv", filename);
    }
}
