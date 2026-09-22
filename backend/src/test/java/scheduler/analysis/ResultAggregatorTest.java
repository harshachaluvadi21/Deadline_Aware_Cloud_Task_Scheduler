package scheduler.analysis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import scheduler.experiment.CampaignRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ResultAggregator Tests")
class ResultAggregatorTest {

    @Test
    @DisplayName("Executes analysis pipeline on campaign results producing all required CSVs")
    void testAnalysisPipelineExecution(@TempDir Path tempDir) throws Exception {
        // Run campaign in temp dir
        CampaignRunner.runCampaign(tempDir);

        // Run analysis
        ResultAggregator.AnalysisOutputSummary summary = ResultAggregator.executeAnalysis(tempDir);

        assertEquals(24, summary.totalProcessedRuns());
        assertEquals(12, summary.pairwiseComparisonsCount());

        // Check aggregated results
        assertTrue(Files.exists(summary.aggregatedResultsPath()));
        List<String> aggLines = Files.readAllLines(summary.aggregatedResultsPath());
        assertEquals(25, aggLines.size(), "1 header + 24 rows");

        // Check pairwise comparison
        assertTrue(Files.exists(summary.pairwiseComparisonPath()));
        List<String> pairLines = Files.readAllLines(summary.pairwiseComparisonPath());
        assertEquals(13, pairLines.size(), "1 header + 12 comparison rows");

        // Check deadline analysis
        assertTrue(Files.exists(summary.deadlineAnalysisPath()));
        List<String> deadlineLines = Files.readAllLines(summary.deadlineAnalysisPath());
        assertEquals(25, deadlineLines.size(), "1 header + 24 rows");

        // Check workload analysis
        assertTrue(Files.exists(summary.workloadAnalysisPath()));
        List<String> workloadLines = Files.readAllLines(summary.workloadAnalysisPath());
        assertEquals(25, workloadLines.size(), "1 header + 24 rows");
    }
}
