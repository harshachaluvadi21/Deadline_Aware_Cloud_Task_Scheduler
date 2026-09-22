package scheduler.experiment;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Batch execution coordinator for experimental campaigns.
 *
 * <p>Supports:
 * <ul>
 *   <li>Single smoke experiment (5 tasks under NORMAL_LOAD) for lightweight validation.</li>
 *   <li>Full evaluation matrix: 4 scenarios \(\times\) 3 task counts (20, 50, 100) = 12 workloads (24 scheduler runs).</li>
 * </ul>
 */
public class ExperimentBatchRunner {

    /** Standard task count levels specified for the complete campaign. */
    public static final int[] STANDARD_TASK_COUNTS = {20, 50, 100};

    /**
     * Executes the 5-task smoke experiment to verify end-to-end framework mechanics.
     *
     * @param outputDir base output directory for results and workloads
     * @return paired results of the smoke test
     * @throws IOException if error occurs during I/O
     */
    public static ExperimentRunner.ExperimentPair runSmokeExperiment(Path outputDir) throws IOException {
        Objects.requireNonNull(outputDir, "Output directory cannot be null");

        ExperimentConfig smokeConfig = ExperimentConfig.of(
                WorkloadScenarioType.NORMAL_LOAD,
                5,
                WorkloadScenarioType.NORMAL_LOAD.getDefaultSeed(),
                ExperimentConfig.DEFAULT_VM_COUNT,
                outputDir
        );

        ExperimentRunner.ExperimentPair pair = ExperimentRunner.runExperiment(smokeConfig);

        List<ExperimentResult> results = List.of(pair.baseline(), pair.proposed());
        ExperimentRunner.exportSummaryCsv(outputDir.resolve("smoke_summary.csv"), results);
        ExperimentRunner.exportDetailedTasksCsv(outputDir.resolve("smoke_task_details.csv"), results);

        return pair;
    }

    /**
     * Executes the complete evaluation matrix: 4 scenarios \(\times\) 3 task counts = 12 workloads (24 scheduler runs).
     *
     * <p><b>Note:</b> Per experimental discipline, this is only invoked when explicitly requested.
     *
     * @param outputDir base output directory
     * @return list of paired results for all 12 workloads
     * @throws IOException if error occurs during I/O
     */
    public static List<ExperimentRunner.ExperimentPair> runFullMatrix(Path outputDir) throws IOException {
        Objects.requireNonNull(outputDir, "Output directory cannot be null");

        List<ExperimentRunner.ExperimentPair> pairs = new ArrayList<>();
        List<ExperimentResult> allResults = new ArrayList<>();

        for (WorkloadScenarioType scenario : WorkloadScenarioType.values()) {
            for (int taskCount : STANDARD_TASK_COUNTS) {
                ExperimentConfig config = ExperimentConfig.of(
                        scenario,
                        taskCount,
                        scenario.getDefaultSeed(),
                        ExperimentConfig.DEFAULT_VM_COUNT,
                        outputDir
                );

                ExperimentRunner.ExperimentPair pair = ExperimentRunner.runExperiment(config);
                pairs.add(pair);
                allResults.add(pair.baseline());
                allResults.add(pair.proposed());
            }
        }

        ExperimentRunner.exportSummaryCsv(outputDir.resolve("experiment_summary.csv"), allResults);
        ExperimentRunner.exportDetailedTasksCsv(outputDir.resolve("experiment_task_details.csv"), allResults);

        return pairs;
    }

    /**
     * Standalone main method to run the 5-task smoke experiment from the CLI.
     */
    public static void main(String[] args) {
        Path outputDir = args.length > 0 ? Paths.get(args[0]) : Paths.get("results");
        try {
            System.out.println("Starting 5-task smoke experiment in: " + outputDir.toAbsolutePath());
            ExperimentRunner.ExperimentPair pair = runSmokeExperiment(outputDir);
            System.out.println("Smoke experiment completed successfully.");
            System.out.println("Baseline Makespan: " + pair.baseline().makespan()
                    + ", Utilization: " + pair.baseline().resourceUtilization() + "%"
                    + ", DMR: " + pair.baseline().deadlineMissRate());
            System.out.println("Proposed Makespan: " + pair.proposed().makespan()
                    + ", Utilization: " + pair.proposed().resourceUtilization() + "%"
                    + ", DMR: " + pair.proposed().deadlineMissRate());
        } catch (Exception e) {
            System.err.println("Smoke experiment failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
