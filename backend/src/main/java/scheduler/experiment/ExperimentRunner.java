package scheduler.experiment;

import scheduler.baseline.BaselinePriorityScheduler;
import scheduler.baseline.SchedulingResult;
import scheduler.model.Task;
import scheduler.proposed.ProposedPriorityScheduler;
import scheduler.proposed.ProposedSchedulingResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Orchestrates a fair, controlled experimental evaluation comparing
 * the Phase 6 Baseline Scheduler and the Phase 7 Proposed Scheduler
 * on identical, cloned workloads.
 */
public class ExperimentRunner {

    /**
     * Paired outcomes from running both schedulers on an identical workload.
     */
    public record ExperimentPair(
            ExperimentResult baseline,
            ExperimentResult proposed
    ) {}

    /**
     * Executes a complete controlled experiment for the given configuration:
     * 1. Generates deterministic workload using configuration seed and scenario.
     * 2. Persists the workload CSV to {@code outputDirectory/workloads/} and updates the workload manifest.
     * 3. Executes the Baseline Scheduler on a pristine clone of the workload.
     * 4. Executes the Proposed Scheduler on an identical pristine clone of the workload.
     * 5. Computes and packages all objective metrics.
     *
     * @param config the experiment configuration
     * @return paired baseline and proposed experiment results
     * @throws IOException if error occurs while persisting workload or manifest
     */
    public static ExperimentPair runExperiment(ExperimentConfig config) throws IOException {
        Objects.requireNonNull(config, "ExperimentConfig cannot be null");

        // 1. Generate deterministic workload
        List<Task> workload = WorkloadGenerator.generateWorkload(config);

        // 2. Persist workload and update manifest
        Path outputDir = config.outputDirectory();
        Path workloadsDir = outputDir.resolve("workloads");
        String workloadFileName = WorkloadSerializer.getWorkloadFileName(
                config.scenarioType(),
                config.taskCount(),
                config.seed()
        );
        Path workloadFilePath = workloadsDir.resolve(workloadFileName);

        WorkloadSerializer.writeWorkloadToCsv(workload, workloadFilePath);

        Path manifestPath = outputDir.resolve(WorkloadSerializer.DEFAULT_MANIFEST_NAME);
        WorkloadSerializer.recordManifest(manifestPath, new WorkloadSerializer.WorkloadManifestEntry(
                config.scenarioType().name(),
                config.taskCount(),
                config.seed(),
                workloadFileName
        ));

        // 3 & 4. Execute both schedulers on identical clones
        return runOnWorkload(config, workload);
    }

    /**
     * Executes both schedulers on a pre-existing workload without regenerating or re-saving.
     *
     * @param config   experiment configuration (VMs, reference MIPS, etc.)
     * @param workload the identical workload to evaluate
     * @return paired baseline and proposed experiment results
     */
    public static ExperimentPair runOnWorkload(ExperimentConfig config, List<Task> workload) {
        Objects.requireNonNull(config, "ExperimentConfig cannot be null");
        Objects.requireNonNull(workload, "Workload cannot be null");

        int vmCount = config.vms().size();

        // 1. Run Baseline Scheduler on pristine clone
        List<Task> baselineTasks = WorkloadGenerator.cloneWorkload(workload);
        BaselinePriorityScheduler baselineScheduler = new BaselinePriorityScheduler(config.vms(), config.referenceMips());
        List<SchedulingResult> baselineRawResults = baselineScheduler.schedule(baselineTasks);

        MetricsCalculator.MetricsSummary baselineMetrics = MetricsCalculator.calculateFromBaseline(baselineRawResults, vmCount);
        List<TaskExecutionRecord> baselineRecords = baselineRawResults.stream()
                .map(TaskExecutionRecord::fromBaseline)
                .toList();
        ExperimentResult baselineResult = ExperimentResult.from(
                config.scenarioType().name(),
                config.taskCount(),
                config.seed(),
                "BASELINE_IEEE_PRIORITY",
                vmCount,
                baselineMetrics,
                baselineRecords
        );

        // 2. Run Proposed Scheduler on identical pristine clone
        List<Task> proposedTasks = WorkloadGenerator.cloneWorkload(workload);
        ProposedPriorityScheduler proposedScheduler = new ProposedPriorityScheduler(config.vms(), config.referenceMips());
        List<ProposedSchedulingResult> proposedRawResults = proposedScheduler.schedule(proposedTasks);

        MetricsCalculator.MetricsSummary proposedMetrics = MetricsCalculator.calculateFromProposed(proposedRawResults, vmCount);
        List<TaskExecutionRecord> proposedRecords = proposedRawResults.stream()
                .map(TaskExecutionRecord::fromProposed)
                .toList();
        ExperimentResult proposedResult = ExperimentResult.from(
                config.scenarioType().name(),
                config.taskCount(),
                config.seed(),
                "PROPOSED_DEADLINE_AWARE",
                vmCount,
                proposedMetrics,
                proposedRecords
        );

        return new ExperimentPair(baselineResult, proposedResult);
    }

    /**
     * Exports experiment summary records to a designated CSV file.
     *
     * @param targetPath target CSV file path
     * @param results    list of experiment results
     * @throws IOException if I/O error occurs
     */
    public static void exportSummaryCsv(Path targetPath, List<ExperimentResult> results) throws IOException {
        Objects.requireNonNull(targetPath, "Target path cannot be null");
        Objects.requireNonNull(results, "Results cannot be null");

        if (targetPath.getParent() != null) {
            Files.createDirectories(targetPath.getParent());
        }

        try (BufferedWriter writer = Files.newBufferedWriter(targetPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(ExperimentResult.SUMMARY_HEADER);
            writer.newLine();
            for (ExperimentResult res : results) {
                writer.write(res.toSummaryCsvRow());
                writer.newLine();
            }
        }
    }

    /**
     * Exports detailed per-task execution records across all given results to CSV.
     *
     * @param targetPath target CSV file path
     * @param results    list of experiment results
     * @throws IOException if I/O error occurs
     */
    public static void exportDetailedTasksCsv(Path targetPath, List<ExperimentResult> results) throws IOException {
        Objects.requireNonNull(targetPath, "Target path cannot be null");
        Objects.requireNonNull(results, "Results cannot be null");

        if (targetPath.getParent() != null) {
            Files.createDirectories(targetPath.getParent());
        }

        try (BufferedWriter writer = Files.newBufferedWriter(targetPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(TaskExecutionRecord.CSV_HEADER);
            writer.newLine();
            for (ExperimentResult res : results) {
                for (TaskExecutionRecord taskRec : res.taskRecords()) {
                    writer.write(taskRec.toCsvRow(res.scenario(), res.taskCount(), res.seed(), res.schedulerName()));
                    writer.newLine();
                }
            }
        }
    }
}
