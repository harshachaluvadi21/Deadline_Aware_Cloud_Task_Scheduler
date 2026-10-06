package scheduler.experiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Full CloudSim Plus Benchmark Execution Test")
public class FullBenchmarkExecutionTest {

    @Test
    @DisplayName("Execute full 24-run Campaign into results/cloudsim/ and results/ through CloudSim Plus 8.0.0")
    void testExecuteOfficialBenchmarkSuite() throws Exception {
        // 1. Run Official 24-run Campaign to results/cloudsim/ (Section 15)
        Path cloudSimDir = Paths.get("results", "cloudsim");
        CampaignRunner.CampaignExecutionReport report = CampaignRunner.runCampaign(cloudSimDir);
        assertNotNull(report);
        assertEquals(12, report.totalWorkloads());
        assertEquals(24, report.totalRuns());
        assertTrue(report.integrityPassed());
        assertTrue(report.fairnessPassed());
        assertTrue(report.numericalSanityPassed());
        assertTrue(report.reproducibilityPassed());

        // Verify CSV file creation in results/cloudsim/
        Path benchmarkCsv = cloudSimDir.resolve("experiments").resolve("summary").resolve("cloudsim_benchmark_results.csv");
        assertTrue(Files.exists(benchmarkCsv), "cloudsim_benchmark_results.csv must exist in results/cloudsim/");
        List<String> benchmarkLines = Files.readAllLines(benchmarkCsv);
        assertEquals(25, benchmarkLines.size(), "1 header + 24 runs");

        // Also run to results/ for standard output compatibility
        Path resultsDir = Paths.get("results");
        CampaignRunner.runCampaign(resultsDir);

        // 2. Run 5-seed repetition evaluation on DEADLINE_SENSITIVE scenario into results/cloudsim/
        Path summaryDir = cloudSimDir.resolve("experiments").resolve("summary");
        MultiSeedExperimentRunner.MultiSeedRunResult multiSeedResult =
                MultiSeedExperimentRunner.runMultiSeedEvaluation(
                        WorkloadScenarioType.DEADLINE_SENSITIVE,
                        new int[]{20, 50, 100},
                        new long[]{101L, 202L, 303L, 404L, 505L},
                        summaryDir
                );

        assertNotNull(multiSeedResult);
        assertEquals(30, multiSeedResult.rawRows().size(), "3 task counts * 5 seeds * 2 algorithms = 30 runs");
        assertEquals(6, multiSeedResult.aggregatedResults().size(), "3 task counts * 2 algorithms = 6 aggregated groups");

        Path multiSeedCsv = summaryDir.resolve("multi_seed_experiment_results.csv");
        assertTrue(Files.exists(multiSeedCsv), "multi_seed_experiment_results.csv must exist");
        Path aggCsv = summaryDir.resolve("multi_seed_aggregated_summary.csv");
        assertTrue(Files.exists(aggCsv), "multi_seed_aggregated_summary.csv must exist");
    }
}
