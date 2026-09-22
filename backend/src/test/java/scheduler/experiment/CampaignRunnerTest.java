package scheduler.experiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CampaignRunner Tests")
class CampaignRunnerTest {

    @Test
    @DisplayName("CampaignRunner executes all 12 workloads and 24 scheduler runs with integrity")
    void testCampaignExecution(@TempDir Path tempDir) throws Exception {
        CampaignRunner.CampaignExecutionReport report = CampaignRunner.runCampaign(tempDir);

        assertEquals(12, report.totalWorkloads());
        assertEquals(24, report.totalRuns());
        assertTrue(report.integrityPassed());
        assertTrue(report.fairnessPassed());
        assertTrue(report.numericalSanityPassed());
        assertTrue(report.reproducibilityPassed());

        // Check raw summary CSV
        Path summaryCsv = tempDir.resolve("experiments").resolve("summary").resolve("raw_experiment_results.csv");
        assertTrue(Files.exists(summaryCsv), "Summary CSV must exist");
        List<String> summaryLines = Files.readAllLines(summaryCsv);
        assertEquals(25, summaryLines.size(), "1 header + 24 scheduler runs");

        // Check manifest
        Path manifestCsv = tempDir.resolve("manifests").resolve("final_experiment_manifest.csv");
        assertTrue(Files.exists(manifestCsv), "Manifest CSV must exist");
        List<String> manifestLines = Files.readAllLines(manifestCsv);
        assertEquals(13, manifestLines.size(), "1 header + 12 workload rows");

        // Check validation report
        Path valReport = tempDir.resolve("experiments").resolve("validation").resolve("workload_integrity_report.md");
        assertTrue(Files.exists(valReport), "Validation report must exist");
    }
}
