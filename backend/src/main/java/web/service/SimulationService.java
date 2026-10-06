package web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import scheduler.baseline.BaselinePriorityScheduler;
import scheduler.baseline.SchedulingResult;
import scheduler.experiment.ExperimentConfig;
import scheduler.experiment.MetricsCalculator;
import scheduler.experiment.TaskExecutionRecord;
import scheduler.experiment.WorkloadGenerator;
import scheduler.model.CloudVmSpec;
import scheduler.model.Task;
import scheduler.proposed.ProposedPriorityScheduler;
import scheduler.proposed.ProposedSchedulingResult;
import web.dto.*;
import web.exception.CloudWorkerException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service orchestrating simulations and comparisons.
 *
 * Supports two execution environments:
 * 1. SIMULATION (Default): Pure in-memory discrete-event simulation via the existing
 *    frozen Baseline and Proposed scheduling engines and CloudSim Plus specifications.
 * 2. REAL_WORKER: Real execution where the existing scheduler determines task execution
 *    sequence, and CloudWorkerService dispatches the ordered tasks to an active real worker node.
 */
@Service
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    public static final int DEFAULT_VM_COUNT = ExperimentConfig.DEFAULT_VM_COUNT; // 4 VMs
    public static final double DEFAULT_REFERENCE_MIPS = ExperimentConfig.DEFAULT_REFERENCE_MIPS; // 1000.0 MIPS

    private final List<CloudVmSpec> vms;
    private final CloudWorkerService cloudWorkerService;

    @Autowired
    public SimulationService(CloudWorkerService cloudWorkerService) {
        this.vms = ExperimentConfig.createHomogeneousVms(DEFAULT_VM_COUNT, DEFAULT_REFERENCE_MIPS);
        this.cloudWorkerService = cloudWorkerService;
    }

    public SimulationService() {
        this(null);
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
     * Executes single algorithm simulation on the provided task specifications.
     * Defaults to SIMULATION execution target.
     */
    public SimulateResponse simulate(String algorithm, List<TaskDto> taskDtos) {
        return simulate(algorithm, taskDtos, ExecutionTarget.SIMULATION);
    }

    /**
     * Executes single algorithm simulation on the specified execution target (SIMULATION or REAL_WORKER).
     */
    public SimulateResponse simulate(String algorithm, List<TaskDto> taskDtos, ExecutionTarget target) {
        Objects.requireNonNull(algorithm, "Algorithm cannot be null");
        Objects.requireNonNull(taskDtos, "Task list cannot be null");
        ExecutionTarget resolvedTarget = target != null ? target : ExecutionTarget.SIMULATION;

        if (resolvedTarget == ExecutionTarget.REAL_WORKER) {
            return executeOnRealWorker(algorithm, taskDtos);
        } else {
            return executeSimulated(algorithm, taskDtos);
        }
    }

    /**
     * Standard discrete-event in-memory simulation execution path.
     */
    private SimulateResponse executeSimulated(String algorithm, List<TaskDto> taskDtos) {
        String algoUpper = algorithm.trim().toUpperCase();
        List<Task> domainTasks = toDomainTasks(taskDtos);

        if ("BASELINE".equals(algoUpper)) {
            BaselinePriorityScheduler scheduler = new BaselinePriorityScheduler(vms, DEFAULT_REFERENCE_MIPS);
            scheduler.schedule(domainTasks);

            scheduler.cloudsim.CloudSimEnvironment env = new scheduler.cloudsim.CloudSimEnvironment(vms);
            scheduler.cloudsim.CloudSimEnvironment.CloudSimExecutionResult execResult =
                    env.execute(domainTasks, scheduler.getAssignments(), DEFAULT_REFERENCE_MIPS);

            MetricsCalculator.MetricsSummary summary = MetricsCalculator.calculate(execResult.records(), vms.size());

            List<TaskResultDto> taskResults = execResult.records().stream().map(r -> new TaskResultDto(
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

            return new SimulateResponse("BASELINE", taskResults, MetricsDto.from(summary), ExecutionTarget.SIMULATION);

        } else if ("PROPOSED".equals(algoUpper)) {
            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(vms, DEFAULT_REFERENCE_MIPS);
            scheduler.schedule(domainTasks);

            scheduler.cloudsim.CloudSimEnvironment env = new scheduler.cloudsim.CloudSimEnvironment(vms);
            scheduler.cloudsim.CloudSimEnvironment.CloudSimExecutionResult execResult =
                    env.execute(domainTasks, scheduler.getAssignments(), DEFAULT_REFERENCE_MIPS);

            MetricsCalculator.MetricsSummary summary = MetricsCalculator.calculate(execResult.records(), vms.size());

            List<TaskResultDto> taskResults = execResult.records().stream().map(r -> new TaskResultDto(
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

            return new SimulateResponse("PROPOSED", taskResults, MetricsDto.from(summary), ExecutionTarget.SIMULATION);

        } else {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm + ". Expected 'BASELINE' or 'PROPOSED'.");
        }
    }

    /**
     * Real Worker execution path:
     * 1. Validates worker reachability (throws CloudWorkerException if unreachable).
     * 2. Runs the existing scheduler to determine task execution order.
     * 3. Dispatches the ordered tasks sequentially to the worker agent.
     * 4. Maps real wall-clock telemetry into TaskResultDto and MetricsDto.
     */
    private SimulateResponse executeOnRealWorker(String algorithm, List<TaskDto> taskDtos) {
        if (cloudWorkerService == null || !cloudWorkerService.isWorkerReachable()) {
            String url = cloudWorkerService != null ? cloudWorkerService.getWorkerUrl() : "unknown";
            throw new CloudWorkerException("Worker is unreachable at " + url + ". Please ensure the real worker agent is running.");
        }

        String algoUpper = algorithm.trim().toUpperCase();
        List<Task> domainTasks = toDomainTasks(taskDtos);
        Map<Long, TaskDto> originalDtoMap = taskDtos.stream()
                .collect(Collectors.toMap(TaskDto::taskId, t -> t, (a, b) -> a));

        // Use the existing scheduling algorithms to determine task ordering
        List<Long> scheduledTaskIdOrder = new ArrayList<>();
        if ("PROPOSED".equals(algoUpper)) {
            ProposedPriorityScheduler scheduler = new ProposedPriorityScheduler(vms, DEFAULT_REFERENCE_MIPS);
            List<ProposedSchedulingResult> proposedResults = scheduler.schedule(domainTasks);
            for (ProposedSchedulingResult r : proposedResults) {
                scheduledTaskIdOrder.add(r.taskId());
            }
        } else if ("BASELINE".equals(algoUpper)) {
            BaselinePriorityScheduler scheduler = new BaselinePriorityScheduler(vms, DEFAULT_REFERENCE_MIPS);
            List<SchedulingResult> baselineResults = scheduler.schedule(domainTasks);
            for (SchedulingResult r : baselineResults) {
                scheduledTaskIdOrder.add(r.taskId());
            }
        } else {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm + ". Expected 'BASELINE' or 'PROPOSED'.");
        }

        log.info("Executing on real worker in [{}] scheduled order: {}", algoUpper, scheduledTaskIdOrder);

        List<TaskResultDto> taskResults = new ArrayList<>();
        List<TaskExecutionRecord> executionRecords = new ArrayList<>();

        long batchStartNs = System.nanoTime();

        for (Long taskId : scheduledTaskIdOrder) {
            TaskDto orig = originalDtoMap.get(taskId);
            if (orig == null) continue;

            double arrivalTime = orig.arrivalTime();
            double nominalExecutionTime = orig.executionTime();
            int priority = orig.priority();
            double deadline = orig.deadline();

            CloudTaskExecutionRequest request = new CloudTaskExecutionRequest(
                    String.valueOf(taskId),
                    nominalExecutionTime,
                    priority,
                    deadline
            );

            // Wall-clock start time relative to batch start (in seconds)
            double taskStartRel = (System.nanoTime() - batchStartNs) / 1_000_000_000.0;

            // Execute genuine CPU workload on real worker
            CloudTaskExecutionResponse workerResp = cloudWorkerService.executeTask(request);

            // Wall-clock completion time relative to batch start (in seconds)
            double taskCompletionRel = (System.nanoTime() - batchStartNs) / 1_000_000_000.0;
            double actualDuration = workerResp.actualExecutionTimeSeconds();

            double waitingTime = Math.max(0.0, taskStartRel - arrivalTime);
            double turnaroundTime = Math.max(actualDuration, taskCompletionRel - arrivalTime);
            boolean deadlineMissed = taskCompletionRel > deadline;

            String status = deadlineMissed ? "MISSED_DEADLINE" : "COMPLETED";

            TaskResultDto resultDto = new TaskResultDto(
                    taskId,
                    priority,
                    round4(arrivalTime),
                    round4(actualDuration),
                    round4(deadline),
                    0L, // Worker Node ID
                    round4(taskStartRel),
                    round4(taskCompletionRel),
                    round4(waitingTime),
                    round4(turnaroundTime),
                    status,
                    deadlineMissed
            );
            taskResults.add(resultDto);

            TaskExecutionRecord record = new TaskExecutionRecord(
                    taskId,
                    0L,
                    arrivalTime,
                    taskStartRel,
                    taskCompletionRel,
                    actualDuration,
                    waitingTime,
                    turnaroundTime,
                    priority,
                    0.0,
                    deadline,
                    deadlineMissed
            );
            executionRecords.add(record);
        }

        MetricsCalculator.MetricsSummary summary = MetricsCalculator.calculate(executionRecords, 1);
        return new SimulateResponse(algoUpper, taskResults, MetricsDto.from(summary), ExecutionTarget.REAL_WORKER);
    }

    /**
     * Executes both Baseline and Proposed schedulers on identical clones of the workload.
     * Defaults to SIMULATION execution target.
     */
    public CompareResponse compare(List<TaskDto> taskDtos) {
        return compare(taskDtos, ExecutionTarget.SIMULATION);
    }

    /**
     * Executes both Baseline and Proposed schedulers on identical clones of the workload
     * using the specified execution target.
     */
    public CompareResponse compare(List<TaskDto> taskDtos, ExecutionTarget target) {
        Objects.requireNonNull(taskDtos, "Task list cannot be null");
        ExecutionTarget resolvedTarget = target != null ? target : ExecutionTarget.SIMULATION;

        SimulateResponse baseResp = simulate("BASELINE", taskDtos, resolvedTarget);
        SimulateResponse propResp = simulate("PROPOSED", taskDtos, resolvedTarget);

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

    private static double round4(double val) {
        return Math.round(val * 10000.0) / 10000.0;
    }
}
