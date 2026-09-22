package scheduler.proposed;

import scheduler.baseline.VmAssignment;
import scheduler.heap.FibonacciHeap;
import scheduler.heap.HeapKey;
import scheduler.model.CloudVmSpec;
import scheduler.model.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Proposed Deadline-Aware Priority-Based Task Scheduler.
 *
 * <p><b>[Project Design Decision]</b>
 * Extends heuristic task scheduling by dynamically synthesizing:
 * <ul>
 *   <li>Base Task Priority (\(P_{\text{norm}}\))</li>
 *   <li>Remaining Slack &amp; Deadline Urgency (\(U_{\text{deadline}}(t)\))</li>
 *   <li>Elapsed Waiting Time (\(W_{\text{wait}}(t)\))</li>
 * </ul>
 *
 * <p><b>Dynamic Heap Rebuilding:</b>
 * Because a task's urgency and slack change continuously as simulation time \(t\) advances,
 * task scores fluctuate non-monotonically. At each scheduling decision event, the scheduler:
 * <ol>
 *   <li>Collects all unallocated tasks that are READY at current simulation time \(t\).</li>
 *   <li>Recalculates dynamic scores using the current timestamp \(t\).</li>
 *   <li>Rebuilds a fresh standalone {@link FibonacciHeap}.</li>
 *   <li>Inserts nodes with {@code primaryScore = -finalScore} (mapping max score to min-heap root).</li>
 *   <li>Extracts the highest-priority task and assigns it to the earliest available VM.</li>
 * </ol>
 *
 * <p><b>Fair Experimental Comparison:</b>
 * Uses the exact same heterogeneous VM infrastructure, reference MIPS duration scaling,
 * and Earliest-Available VM allocation policy as the Phase 6 Baseline Scheduler.
 */
public class ProposedPriorityScheduler {

    public static final double DEFAULT_REFERENCE_MIPS = 1000.0;

    private final List<CloudVmSpec> vms;
    private final double referenceMips;
    private final DeadlineAwarePriorityCalculator priorityCalculator;
    private final List<VmAssignment> assignments;
    private final List<ProposedSchedulingResult> results;

    /**
     * Internal mutable tracker for VM availability in simulation time.
     */
    private static class VmTracker {
        final CloudVmSpec spec;
        double readyTime;

        VmTracker(CloudVmSpec spec) {
            this.spec = spec;
            this.readyTime = 0.0;
        }
    }

    /**
     * Constructs a proposed scheduler with VM specifications, default reference MIPS,
     * and default priority calculator.
     *
     * @param vms list of VM specifications
     */
    public ProposedPriorityScheduler(List<CloudVmSpec> vms) {
        this(vms, DEFAULT_REFERENCE_MIPS, new DeadlineAwarePriorityCalculator());
    }

    /**
     * Constructs a proposed scheduler with VM specifications, custom reference MIPS,
     * and default priority calculator.
     *
     * @param vms           list of VM specifications
     * @param referenceMips reference MIPS for execution duration scaling
     */
    public ProposedPriorityScheduler(List<CloudVmSpec> vms, double referenceMips) {
        this(vms, referenceMips, new DeadlineAwarePriorityCalculator());
    }

    /**
     * Constructs a proposed scheduler with a fixed number of homogeneous VMs.
     *
     * @param vmCount number of homogeneous VMs (1000 MIPS each)
     */
    public ProposedPriorityScheduler(int vmCount) {
        this(createHomogeneousVms(vmCount), DEFAULT_REFERENCE_MIPS, new DeadlineAwarePriorityCalculator());
    }

    /**
     * Full constructor supporting custom VMs, reference MIPS, and custom priority calculator.
     *
     * @param vms                list of VM specifications
     * @param referenceMips      reference MIPS
     * @param priorityCalculator priority calculator instance
     */
    public ProposedPriorityScheduler(
            List<CloudVmSpec> vms,
            double referenceMips,
            DeadlineAwarePriorityCalculator priorityCalculator) {

        if (vms == null || vms.isEmpty()) {
            throw new IllegalArgumentException("VM specifications cannot be null or empty");
        }
        if (!Double.isFinite(referenceMips) || referenceMips <= 0.0) {
            throw new IllegalArgumentException("Reference MIPS must be positive, got: " + referenceMips);
        }
        if (priorityCalculator == null) {
            throw new IllegalArgumentException("Priority calculator cannot be null");
        }

        this.vms = Collections.unmodifiableList(new ArrayList<>(vms));
        this.referenceMips = referenceMips;
        this.priorityCalculator = priorityCalculator;
        this.assignments = new ArrayList<>();
        this.results = new ArrayList<>();
    }

    private static List<CloudVmSpec> createHomogeneousVms(int vmCount) {
        if (vmCount <= 0) {
            throw new IllegalArgumentException("VM count must be positive: " + vmCount);
        }
        List<CloudVmSpec> list = new ArrayList<>(vmCount);
        for (int i = 0; i < vmCount; i++) {
            list.add(CloudVmSpec.of(i, DEFAULT_REFERENCE_MIPS, 2048, 1000, 10000));
        }
        return list;
    }

    /**
     * Executes dynamic, deadline-aware priority scheduling on the supplied workload.
     *
     * @param inputTasks list of tasks to schedule
     * @return unmodifiable list of scheduling results
     * @throws IllegalArgumentException if inputTasks is null or empty
     */
    public List<ProposedSchedulingResult> schedule(List<Task> inputTasks) {
        if (inputTasks == null || inputTasks.isEmpty()) {
            throw new IllegalArgumentException("Input tasks cannot be null or empty");
        }

        this.assignments.clear();
        this.results.clear();

        // Workload sorted initially by arrival time, then Task ID
        List<Task> pendingTasks = new ArrayList<>(inputTasks);
        pendingTasks.sort(Comparator.comparingDouble(Task::getArrivalTime)
            .thenComparingLong(Task::getTaskId));

        // Initialize VM trackers
        List<VmTracker> vmTrackers = new ArrayList<>(vms.size());
        for (CloudVmSpec spec : vms) {
            vmTrackers.add(new VmTracker(spec));
        }

        // List of currently arrived, unallocated tasks
        List<Task> readyPool = new ArrayList<>();
        int pendingIdx = 0;
        int totalTasks = pendingTasks.size();
        double currentClock = 0.0;

        while (pendingIdx < totalTasks || !readyPool.isEmpty()) {
            // If ready pool is empty, advance clock to the arrival time of the next pending task
            if (readyPool.isEmpty()) {
                currentClock = Math.max(currentClock, pendingTasks.get(pendingIdx).getArrivalTime());
            } else {
                // Otherwise, evaluate current earliest VM availability
                double earliestVmReady = vmTrackers.stream()
                    .mapToDouble(v -> v.readyTime)
                    .min()
                    .orElse(0.0);
                currentClock = Math.max(currentClock, earliestVmReady);
            }

            // Admit all tasks that have arrived up to currentClock
            while (pendingIdx < totalTasks && pendingTasks.get(pendingIdx).getArrivalTime() <= currentClock + 1e-9) {
                Task arriving = pendingTasks.get(pendingIdx);
                arriving.markReady();
                readyPool.add(arriving);
                pendingIdx++;
            }

            if (readyPool.isEmpty()) {
                continue;
            }

            // [Project Design Decision]: Rebuild a fresh Fibonacci Heap with recalculated scores at currentClock
            FibonacciHeap<HeapKey, Task> heap = new FibonacciHeap<>();
            for (Task readyTask : readyPool) {
                DeadlineAwarePriorityCalculator.ScoreBreakdown breakdown =
                    priorityCalculator.calculateScoreBreakdown(readyTask, currentClock);

                // Set dynamic priority on task for inspection
                readyTask.setDynamicPriority(breakdown.finalScore());

                // Min-heap ordering:
                // primaryScore: -finalScore (highest score extracts first)
                // secondaryScore: deadline (earlier deadline tie-breaker)
                // sequenceId: taskId (deterministic tie-breaker)
                HeapKey key = HeapKey.of(-breakdown.finalScore(), readyTask.getDeadline(), readyTask.getTaskId());
                heap.insert(key, readyTask);
            }

            // Extract the highest-priority task from heap
            Task selectedTask = heap.extractMin();
            readyPool.remove(selectedTask);

            // Select earliest available VM using the baseline-identical multi-server policy
            VmTracker selectedVm = selectEarliestAvailableVm(vmTrackers, selectedTask.getArrivalTime());
            double startTime = Math.max(selectedTask.getArrivalTime(), selectedVm.readyTime);

            // Calculate actual execution duration on selected VM
            double speedRatio = this.referenceMips / selectedVm.spec.mips();
            double actualDuration = selectedTask.getExecutionTime() * speedRatio;
            double completionTime = startTime + actualDuration;

            // Update VM state
            selectedVm.readyTime = completionTime;

            // Transition task lifecycle
            selectedTask.markRunning(startTime, selectedVm.spec.id());
            selectedTask.markFinished(completionTime);

            // Record assignment and proposed result
            VmAssignment assignment = new VmAssignment(
                selectedTask.getTaskId(),
                selectedVm.spec.id(),
                selectedTask.getArrivalTime(),
                startTime,
                completionTime,
                actualDuration
            );
            this.assignments.add(assignment);

            DeadlineAwarePriorityCalculator.ScoreBreakdown dispatchBreakdown =
                priorityCalculator.calculateScoreBreakdown(selectedTask, startTime);

            ProposedSchedulingResult result = ProposedSchedulingResult.fromTask(
                selectedTask,
                dispatchBreakdown.finalScore(),
                dispatchBreakdown.deadlineUrgency()
            );
            this.results.add(result);

            // Advance currentClock to at least the dispatched task's start time
            currentClock = Math.max(currentClock, startTime);
        }

        return Collections.unmodifiableList(new ArrayList<>(this.results));
    }

    /**
     * Selects the earliest available VM using identical logic to the IEEE Baseline Scheduler.
     */
    private VmTracker selectEarliestAvailableVm(List<VmTracker> trackers, double arrivalTime) {
        return trackers.stream().min((v1, v2) -> {
            double effectiveReady1 = Math.max(v1.readyTime, arrivalTime);
            double effectiveReady2 = Math.max(v2.readyTime, arrivalTime);

            int cmpReady = Double.compare(effectiveReady1, effectiveReady2);
            if (cmpReady != 0) {
                return cmpReady;
            }

            // Tie-breaker 1: Higher MIPS (faster VM)
            int cmpMips = Double.compare(v2.spec.mips(), v1.spec.mips());
            if (cmpMips != 0) {
                return cmpMips;
            }

            // Tie-breaker 2: Lower VM ID
            return Long.compare(v1.spec.id(), v2.spec.id());
        }).orElseThrow(() -> new IllegalStateException("No VMs available"));
    }

    // Getters
    public List<CloudVmSpec> getVms() { return vms; }
    public double getReferenceMips() { return referenceMips; }
    public DeadlineAwarePriorityCalculator getPriorityCalculator() { return priorityCalculator; }
    public List<VmAssignment> getAssignments() { return Collections.unmodifiableList(assignments); }
    public List<ProposedSchedulingResult> getResults() { return Collections.unmodifiableList(results); }
}
