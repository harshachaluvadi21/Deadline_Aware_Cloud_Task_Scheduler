package web.service;

import org.springframework.stereotype.Service;
import scheduler.baseline.BaselinePriorityScheduler;
import scheduler.baseline.SchedulingResult;
import scheduler.experiment.ExperimentConfig;
import scheduler.experiment.MetricsCalculator;
import scheduler.experiment.WorkloadGenerator;
import scheduler.model.CloudVmSpec;
import scheduler.model.Task;
import scheduler.proposed.ProposedPriorityScheduler;
import scheduler.proposed.ProposedSchedulingResult;
import web.dto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service orchestrating in-memory simulations and comparisons using the existing
 * frozen Baseline and Proposed scheduling engines.
 */
@Service
public class SimulationService {

    public static final int DEFAULT_VM_COUNT = ExperimentConfig.DEFAULT_VM_COUNT; // 4 VMs
    public static final double DEFAULT_REFERENCE_MIPS = ExperimentConfig.DEFAULT_REFERENCE_MIPS; // 1000.0 MIPS

    private final List<CloudVmSpec> vms;

    public SimulationService() {
        this.vms = ExperimentConfig.createHomogeneousVms(DEFAULT_VM_COUNT, DEFAULT_REFERENCE_MIPS);
    }

    /**
     * Returns a fixed, deterministic sample workload for UI demonstration.
     */
    public List<TaskDto> getSampleWorkload() {
        return List.of(
                new TaskDto(0L, 5, 0.00, 15.65, 47.31),
                new TaskDto(1L, 4, 1.99, 10.56, 30.90),
                new TaskDto(2L, 6, 2.85, 12.58, 56.86),
                new TaskDto(3L, 5, 4.68, 10.28, 40.74),
                new TaskDto(4L, 3, 6.42, 7.65, 30.40)
        );
    }

    /**
     * Executes a single algorithm simulation on the provided task specifications.
     */
    public SimulateResponse simulate(String algorithm, List<TaskDto> taskDtos) {
        Objects.requireNonNull(algorithm, "Algorithm cannot be null");
        Objects.requireNonNull(taskDtos, "Task list cannot be null");

        String algoUpper = algorithm.trim().toUpperCase();
        List<Task> domainTasks = toDomainTasks(taskDtos);

        if ("BASELINE".equals(algoUpper)) {
            BaselinePriorityScheduler scheduler = new BaselinePriorityScheduler(vms, DEFAULT_REFERENCE_MIPS);
            List<SchedulingResult> results = scheduler.schedule(domainTasks);
            MetricsCalculator.MetricsSummary summary = MetricsCalculator.calculateFromBaseline(results, vms.size());

            List<TaskResultDto> taskResults = results.stream().map(r -> new TaskResultDto(
                    r.taskId(),
                    r.basePriority(),
                    r.arrivalTime(),
                    r.executionTime(),
                    r.deadline(),
                    r.assignedVmId(),
                    r.startTime(),
                    r.completionTime(),
                    r.waitingTime(),
                    r.turnaroundTime(),
                    r.deadlineMissed() ? "MISSED_DEADLINE" : "COMPLETED",
                    r.deadlineMissed()
            )).toList();

            return new SimulateResponse("BASELINE", taskResults, MetricsDto.from(summary));

        } else if ("PROPOSED".equals(algoUpper)) {
            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(vms, DEFAULT_REFERENCE_MIPS);
            List<ProposedSchedulingResult> results = scheduler.schedule(domainTasks);
            MetricsCalculator.MetricsSummary summary = MetricsCalculator.calculateFromProposed(results, vms.size());

            List<TaskResultDto> taskResults = results.stream().map(r -> new TaskResultDto(
                    r.taskId(),
                    r.basePriority(),
                    r.arrivalTime(),
                    r.executionTime(),
                    r.deadline(),
                    r.assignedVmId(),
                    r.startTime(),
                    r.completionTime(),
                    r.waitingTime(),
                    r.turnaroundTime(),
                    r.deadlineMissed() ? "MISSED_DEADLINE" : "COMPLETED",
                    r.deadlineMissed()
            )).toList();

            return new SimulateResponse("PROPOSED", taskResults, MetricsDto.from(summary));

        } else {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm + ". Expected 'BASELINE' or 'PROPOSED'.");
        }
    }

    /**
     * Executes both Baseline and Proposed schedulers on identical clones of the workload,
     * and computes pairwise numerical differences (Proposed - Baseline).
     */
    public CompareResponse compare(List<TaskDto> taskDtos) {
        Objects.requireNonNull(taskDtos, "Task list cannot be null");

        List<Task> baseDomain = toDomainTasks(taskDtos);
        List<Task> baselineTasks = WorkloadGenerator.cloneWorkload(baseDomain);
        List<Task> proposedTasks = WorkloadGenerator.cloneWorkload(baseDomain);

        // Run Baseline
        BaselinePriorityScheduler baselineScheduler = new BaselinePriorityScheduler(vms, DEFAULT_REFERENCE_MIPS);
        List<SchedulingResult> baseResults = baselineScheduler.schedule(baselineTasks);
        MetricsCalculator.MetricsSummary baseSummary = MetricsCalculator.calculateFromBaseline(baseResults, vms.size());
        List<TaskResultDto> baseTaskResults = baseResults.stream().map(r -> new TaskResultDto(
                r.taskId(),
                r.basePriority(),
                r.arrivalTime(),
                r.executionTime(),
                r.deadline(),
                r.assignedVmId(),
                r.startTime(),
                r.completionTime(),
                r.waitingTime(),
                r.turnaroundTime(),
                r.deadlineMissed() ? "MISSED_DEADLINE" : "COMPLETED",
                r.deadlineMissed()
        )).toList();
        SimulateResponse baseResp = new SimulateResponse("BASELINE", baseTaskResults, MetricsDto.from(baseSummary));

        // Run Proposed
        ProposedPriorityScheduler proposedScheduler = new ProposedPriorityScheduler(vms, DEFAULT_REFERENCE_MIPS);
        List<ProposedSchedulingResult> propResults = proposedScheduler.schedule(proposedTasks);
        MetricsCalculator.MetricsSummary propSummary = MetricsCalculator.calculateFromProposed(propResults, vms.size());
        List<TaskResultDto> propTaskResults = propResults.stream().map(r -> new TaskResultDto(
                r.taskId(),
                r.basePriority(),
                r.arrivalTime(),
                r.executionTime(),
                r.deadline(),
                r.assignedVmId(),
                r.startTime(),
                r.completionTime(),
                r.waitingTime(),
                r.turnaroundTime(),
                r.deadlineMissed() ? "MISSED_DEADLINE" : "COMPLETED",
                r.deadlineMissed()
        )).toList();
        SimulateResponse propResp = new SimulateResponse("PROPOSED", propTaskResults, MetricsDto.from(propSummary));

        PairwiseComparisonDto comparison = PairwiseComparisonDto.of(baseResp.metrics(), propResp.metrics());
        return new CompareResponse(baseResp, propResp, comparison);
    }

    private List<Task> toDomainTasks(List<TaskDto> dtos) {
        List<Task> list = new ArrayList<>(dtos.size());
        for (TaskDto dto : dtos) {
            list.add(new Task(
                    dto.taskId(),
                    dto.priority(),
                    dto.arrivalTime(),
                    dto.executionTime(),
                    dto.deadline()
            ));
        }
        return list;
    }
}
