package scheduler.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Result Aggregator and Analytical Table Generator for Phase 10 & 11.
 *
 * <p>Produces publication-ready CSV datasets preserving raw measurements
 * without rankings, scores, or value judgments.
 */
public class ResultAggregator {

    public static final String AGGREGATED_HEADER =
            "scenario,taskCount,scheduler,meanMakespan,meanAverageWaitingTime,meanAverageTurnaroundTime,meanThroughput,meanDeadlineMissRate,meanResourceUtilization,totalDeadlineMissedTasks,totalCompletedTasks,totalTasks";

    public static final String DEADLINE_ANALYSIS_HEADER =
            "scenario,taskCount,scheduler,totalTasks,deadlineMissedTasks,deadlineMetTasks,deadlineMissRate";

    public static final String WORKLOAD_ANALYSIS_HEADER =
            "scenario,taskCount,scheduler,makespan,averageWaitingTime,averageTurnaroundTime,throughput,deadlineMissRate,resourceUtilization,meanPriority,meanExecutionTime,meanDeadlineSlackAtArrival";

    public static final String FINAL_METRIC_TABLE_HEADER =
            "Scenario,Tasks,Scheduler,Makespan (s),Avg Waiting Time (s),Avg Turnaround Time (s),Throughput (tasks/s),Deadline Miss Rate (%),Resource Utilization (%)";

    public static final String FINAL_PAIRWISE_TABLE_HEADER =
            "Scenario,Tasks,Baseline Makespan (s),Proposed Makespan (s),Baseline Wait (s),Proposed Wait (s),Baseline Turnaround (s),Proposed Turnaround (s),Baseline Throughput (tasks/s),Proposed Throughput (tasks/s),Baseline DMR (%),Proposed DMR (%),Baseline Utilization (%),Proposed Utilization (%)";

    public record AnalysisOutputSummary(
            int totalProcessedRuns,
            int pairwiseComparisonsCount,
            Path aggregatedResultsPath,
            Path pairwiseComparisonPath,
            Path deadlineAnalysisPath,
            Path workloadAnalysisPath
    ) {}

    /**
     * Executes the complete analytical aggregation pipeline.
     *
     * @param resultsDir base results directory ("results")
     * @return summary of generated analysis outputs
     * @throws IOException if error reading raw inputs or writing analysis tables
     */
    public static AnalysisOutputSummary executeAnalysis(Path resultsDir) throws IOException {
        Path rawSummaryPath = resultsDir.resolve("experiments").resolve("summary").resolve("raw_experiment_results.csv");
        Path analysisDir = resultsDir.resolve("analysis");
        Path workloadsDir = resultsDir.resolve("workloads");

        Files.createDirectories(analysisDir);

        List<RawResultRow> rawRows = ResultLoader.loadRawResults(rawSummaryPath);
        if (rawRows.size() != 24) {
            throw new IllegalStateException("Expected exactly 24 raw results, found: " + rawRows.size());
        }

        // 1. Group by (scenario, taskCount) to create pairs
        Map<String, List<RawResultRow>> grouped = new LinkedHashMap<>();
        for (RawResultRow r : rawRows) {
            String key = r.scenario() + "_" + r.taskCount();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        List<PairwiseComparison> pairwiseList = new ArrayList<>();
        List<String> aggregatedRows = new ArrayList<>();
        List<String> deadlineRows = new ArrayList<>();
        List<String> workloadAnalysisRows = new ArrayList<>();
        List<String> finalMetricRows = new ArrayList<>();
        List<String> finalPairwiseRows = new ArrayList<>();

        for (Map.Entry<String, List<RawResultRow>> entry : grouped.entrySet()) {
            List<RawResultRow> list = entry.getValue();
            if (list.size() != 2) {
                throw new IllegalStateException("Expected 2 runs for " + entry.getKey() + ", got " + list.size());
            }

            RawResultRow baseline = list.stream()
                    .filter(r -> r.scheduler().equals("IEEE_BASELINE"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Missing IEEE_BASELINE for " + entry.getKey()));

            RawResultRow proposed = list.stream()
                    .filter(r -> r.scheduler().equals("PROPOSED_DEADLINE_AWARE"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Missing PROPOSED_DEADLINE_AWARE for " + entry.getKey()));

            // Pairwise comparison (Phase 10.4)
            PairwiseComparison pair = PairwiseComparison.compute(baseline, proposed);
            pairwiseList.add(pair);

            // Workload characteristics (Phase 10.7)
            Path workloadPath = workloadsDir.resolve(baseline.workloadFile());
            ResultLoader.WorkloadCharacteristics chars = ResultLoader.computeWorkloadCharacteristics(workloadPath);

            for (RawResultRow r : List.of(baseline, proposed)) {
                // Aggregated results (Phase 10.3) - Note: for single deterministic seed, mean = observed value
                aggregatedRows.add(String.format(Locale.US,
                        "%s,%d,%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%d,%d,%d",
                        r.scenario(), r.taskCount(), r.scheduler(),
                        r.makespan(), r.averageWaitingTime(), r.averageTurnaroundTime(),
                        r.throughput(), r.deadlineMissRate(), r.resourceUtilization(),
                        r.deadlineMissedCount(), r.completedTaskCount(), r.totalTaskCount()
                ));

                // Deadline analysis (Phase 10.6)
                int met = r.totalTaskCount() - r.deadlineMissedCount();
                deadlineRows.add(String.format(Locale.US,
                        "%s,%d,%s,%d,%d,%d,%.4f",
                        r.scenario(), r.taskCount(), r.scheduler(),
                        r.totalTaskCount(), r.deadlineMissedCount(), met, r.deadlineMissRate()
                ));

                // Workload-level analysis (Phase 10.7)
                workloadAnalysisRows.add(String.format(Locale.US,
                        "%s,%d,%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f",
                        r.scenario(), r.taskCount(), r.scheduler(),
                        r.makespan(), r.averageWaitingTime(), r.averageTurnaroundTime(),
                        r.throughput(), r.deadlineMissRate(), r.resourceUtilization(),
                        chars.meanPriority(), chars.meanExecutionTime(), chars.meanDeadlineSlackAtArrival()
                ));

                // Phase 11 final metric table
                finalMetricRows.add(String.format(Locale.US,
                        "%s,%d,%s,%.2f,%.2f,%.2f,%.4f,%.2f,%.2f",
                        r.scenario(), r.taskCount(), r.scheduler(),
                        r.makespan(), r.averageWaitingTime(), r.averageTurnaroundTime(),
                        r.throughput(), r.deadlineMissRate() * 100.0, r.resourceUtilization()
                ));
            }

            // Phase 11 final pairwise table
            finalPairwiseRows.add(String.format(Locale.US,
                    "%s,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.4f,%.4f,%.2f,%.2f,%.2f,%.2f",
                    baseline.scenario(), baseline.taskCount(),
                    baseline.makespan(), proposed.makespan(),
                    baseline.averageWaitingTime(), proposed.averageWaitingTime(),
                    baseline.averageTurnaroundTime(), proposed.averageTurnaroundTime(),
                    baseline.throughput(), proposed.throughput(),
                    baseline.deadlineMissRate() * 100.0, proposed.deadlineMissRate() * 100.0,
                    baseline.resourceUtilization(), proposed.resourceUtilization()
            ));
        }

        // Write Phase 10 CSV tables
        Path aggPath = analysisDir.resolve("aggregated_results.csv");
        writeCsv(aggPath, AGGREGATED_HEADER, aggregatedRows);

        Path pairPath = analysisDir.resolve("pairwise_comparison.csv");
        List<String> pairRows = pairwiseList.stream().map(PairwiseComparison::toCsvRow).toList();
        writeCsv(pairPath, PairwiseComparison.CSV_HEADER, pairRows);

        Path deadlinePath = analysisDir.resolve("deadline_analysis.csv");
        writeCsv(deadlinePath, DEADLINE_ANALYSIS_HEADER, deadlineRows);

        Path workloadPath = analysisDir.resolve("workload_analysis.csv");
        writeCsv(workloadPath, WORKLOAD_ANALYSIS_HEADER, workloadAnalysisRows);

        // Write Phase 11 CSV tables
        Path finalMetricPath = analysisDir.resolve("final_metric_table.csv");
        writeCsv(finalMetricPath, FINAL_METRIC_TABLE_HEADER, finalMetricRows);

        Path finalPairwisePath = analysisDir.resolve("final_pairwise_table.csv");
        writeCsv(finalPairwisePath, FINAL_PAIRWISE_TABLE_HEADER, finalPairwiseRows);

        return new AnalysisOutputSummary(
                rawRows.size(),
                pairwiseList.size(),
                aggPath,
                pairPath,
                deadlinePath,
                workloadPath
        );
    }

    private static void writeCsv(Path path, String header, List<String> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(header);
            writer.newLine();
            for (String r : rows) {
                writer.write(r);
                writer.newLine();
            }
        }
    }

    public static void main(String[] args) {
        Path targetDir = args.length > 0 ? Paths.get(args[0]) : Paths.get("results");
        try {
            AnalysisOutputSummary summary = executeAnalysis(targetDir);
            System.out.println("Phase 10 Analysis Execution COMPLETED successfully.");
            System.out.println("Processed Runs: " + summary.totalProcessedRuns() + ", Pairwise: " + summary.pairwiseComparisonsCount());
        } catch (Exception e) {
            System.err.println("Phase 10 Analysis Execution FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
