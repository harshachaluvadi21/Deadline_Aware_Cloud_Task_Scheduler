package scheduler.experiment;

import scheduler.baseline.BaselinePriorityScheduler;
import scheduler.cloudsim.CloudSimEnvironment;
import scheduler.model.CloudVmSpec;
import scheduler.model.Task;
import scheduler.proposed.ProposedPriorityScheduler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Executes repeated experimental runs across controlled random seeds to evaluate
 * statistical stability, empirical mean, and standard deviation for Baseline vs Proposed.
 */
public class MultiSeedExperimentRunner {

    public static final long[] DEFAULT_SEEDS = {101L, 202L, 303L, 404L, 505L};
    public static final int[] DEFAULT_TASK_COUNTS = {20, 50, 100};

    public record StatisticalMetric(double mean, double stdDev) {
        @Override
        public String toString() {
            return String.format(Locale.US, "%.4f ± %.4f", mean, stdDev);
        }
    }

    public record AggregatedResult(
            String algorithm,
            String scenario,
            int taskCount,
            int repetitions,
            StatisticalMetric makespan,
            StatisticalMetric averageWaitingTime,
            StatisticalMetric averageTurnaroundTime,
            StatisticalMetric throughput,
            StatisticalMetric deadlineMissRate,
            StatisticalMetric deadlineSuccessRate,
            StatisticalMetric resourceUtilization
    ) {}

    public record MultiSeedRunResult(
            List<String> rawRows,
            List<AggregatedResult> aggregatedResults
    ) {}

    public static MultiSeedRunResult runMultiSeedEvaluation(
            WorkloadScenarioType scenario,
            int[] taskCounts,
            long[] seeds,
            Path outputDir
    ) throws IOException {
        Objects.requireNonNull(scenario, "Scenario cannot be null");
        Objects.requireNonNull(taskCounts, "Task counts cannot be null");
        Objects.requireNonNull(seeds, "Seeds cannot be null");
        Objects.requireNonNull(outputDir, "Output directory cannot be null");

        Files.createDirectories(outputDir);

        List<CloudVmSpec> vms = ExperimentConfig.createHomogeneousVms(
                ExperimentConfig.DEFAULT_VM_COUNT,
                ExperimentConfig.DEFAULT_REFERENCE_MIPS
        );
        double refMips = ExperimentConfig.DEFAULT_REFERENCE_MIPS;
        int vmCount = vms.size();

        List<String> rawCsvRows = new ArrayList<>();
        List<AggregatedResult> aggregatedList = new ArrayList<>();

        for (int count : taskCounts) {
            List<MetricsCalculator.MetricsSummary> baselineRuns = new ArrayList<>();
            List<MetricsCalculator.MetricsSummary> proposedRuns = new ArrayList<>();

            for (long seed : seeds) {
                List<Task> workload = WorkloadGenerator.generateWorkload(scenario, count, seed);

                // 1. Baseline Run in CloudSim
                List<Task> baseTasks = WorkloadGenerator.cloneWorkload(workload);
                BaselinePriorityScheduler baseSched = new BaselinePriorityScheduler(vms, refMips);
                baseSched.schedule(baseTasks);
                CloudSimEnvironment baseEnv = new CloudSimEnvironment(vms);
                CloudSimEnvironment.CloudSimExecutionResult baseExec =
                        baseEnv.execute(baseTasks, baseSched.getAssignments(), refMips);
                MetricsCalculator.MetricsSummary baseMetrics =
                        MetricsCalculator.calculate(baseExec.records(), vmCount);
                baselineRuns.add(baseMetrics);

                rawCsvRows.add(formatRow("IEEE_BASELINE", scenario.name(), seed, count, baseMetrics));

                // 2. Proposed Run in CloudSim
                List<Task> propTasks = WorkloadGenerator.cloneWorkload(workload);
                ProposedPriorityScheduler propSched = new ProposedPriorityScheduler(vms, refMips);
                propSched.schedule(propTasks);
                CloudSimEnvironment propEnv = new CloudSimEnvironment(vms);
                CloudSimEnvironment.CloudSimExecutionResult propExec =
                        propEnv.execute(propTasks, propSched.getAssignments(), refMips);
                MetricsCalculator.MetricsSummary propMetrics =
                        MetricsCalculator.calculate(propExec.records(), vmCount);
                proposedRuns.add(propMetrics);

                rawCsvRows.add(formatRow("PROPOSED_DEADLINE_AWARE", scenario.name(), seed, count, propMetrics));
            }

            aggregatedList.add(aggregate("IEEE_BASELINE", scenario.name(), count, baselineRuns));
            aggregatedList.add(aggregate("PROPOSED_DEADLINE_AWARE", scenario.name(), count, proposedRuns));
        }

        // Write raw multi-seed CSV
        Path rawCsvPath = outputDir.resolve("multi_seed_experiment_results.csv");
        writeCsv(rawCsvPath, CampaignRunner.BENCHMARK_HEADER, rawCsvRows);

        // Write aggregated summary CSV
        Path aggCsvPath = outputDir.resolve("multi_seed_aggregated_summary.csv");
        String aggHeader = "Algorithm,WorkloadScenario,TaskCount,Repetitions,MakespanMean,MakespanStdDev,WaitMean,WaitStdDev,TurnaroundMean,TurnaroundStdDev,ThroughputMean,ThroughputStdDev,MissRateMean,MissRateStdDev,SuccessRateMean,SuccessRateStdDev,UtilMean,UtilStdDev";
        List<String> aggRows = new ArrayList<>();
        for (AggregatedResult ar : aggregatedList) {
            aggRows.add(String.format(Locale.US,
                    "%s,%s,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f",
                    ar.algorithm(), ar.scenario(), ar.taskCount(), ar.repetitions(),
                    ar.makespan().mean(), ar.makespan().stdDev(),
                    ar.averageWaitingTime().mean(), ar.averageWaitingTime().stdDev(),
                    ar.averageTurnaroundTime().mean(), ar.averageTurnaroundTime().stdDev(),
                    ar.throughput().mean(), ar.throughput().stdDev(),
                    ar.deadlineMissRate().mean(), ar.deadlineMissRate().stdDev(),
                    ar.deadlineSuccessRate().mean(), ar.deadlineSuccessRate().stdDev(),
                    ar.resourceUtilization().mean(), ar.resourceUtilization().stdDev()
            ));
        }
        writeCsv(aggCsvPath, aggHeader, aggRows);

        return new MultiSeedRunResult(rawCsvRows, aggregatedList);
    }

    private static String formatRow(String alg, String scenario, long seed, int count, MetricsCalculator.MetricsSummary m) {
        double successRate = Math.max(0.0, 1.0 - m.deadlineMissRate());
        return String.format(Locale.US, "%s,%s,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f",
                alg, scenario, seed, count,
                m.makespan(), m.averageWaitingTime(), m.averageTurnaroundTime(),
                m.throughput(), m.deadlineMissRate(), successRate, m.resourceUtilization());
    }

    private static AggregatedResult aggregate(String alg, String scenario, int count, List<MetricsCalculator.MetricsSummary> runs) {
        int n = runs.size();
        StatisticalMetric makespan = calcStat(runs.stream().mapToDouble(MetricsCalculator.MetricsSummary::makespan).toArray());
        StatisticalMetric wait = calcStat(runs.stream().mapToDouble(MetricsCalculator.MetricsSummary::averageWaitingTime).toArray());
        StatisticalMetric turnaround = calcStat(runs.stream().mapToDouble(MetricsCalculator.MetricsSummary::averageTurnaroundTime).toArray());
        StatisticalMetric throughput = calcStat(runs.stream().mapToDouble(MetricsCalculator.MetricsSummary::throughput).toArray());
        StatisticalMetric missRate = calcStat(runs.stream().mapToDouble(MetricsCalculator.MetricsSummary::deadlineMissRate).toArray());
        StatisticalMetric successRate = calcStat(runs.stream().mapToDouble(m -> Math.max(0.0, 1.0 - m.deadlineMissRate())).toArray());
        StatisticalMetric util = calcStat(runs.stream().mapToDouble(MetricsCalculator.MetricsSummary::resourceUtilization).toArray());

        return new AggregatedResult(alg, scenario, count, n, makespan, wait, turnaround, throughput, missRate, successRate, util);
    }

    private static StatisticalMetric calcStat(double[] values) {
        if (values.length == 0) return new StatisticalMetric(0.0, 0.0);
        double sum = 0.0;
        for (double v : values) sum += v;
        double mean = sum / values.length;

        double sumSqDiff = 0.0;
        for (double v : values) {
            double diff = v - mean;
            sumSqDiff += diff * diff;
        }
        double stdDev = values.length > 1 ? Math.sqrt(sumSqDiff / (values.length - 1)) : 0.0;
        return new StatisticalMetric(mean, stdDev);
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
}
