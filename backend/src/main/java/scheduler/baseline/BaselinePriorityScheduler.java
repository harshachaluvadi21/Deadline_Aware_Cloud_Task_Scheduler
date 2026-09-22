package scheduler.baseline;

import scheduler.heap.FibonacciHeap;
import scheduler.heap.HeapKey;
import scheduler.model.CloudVmSpec;
import scheduler.model.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Standalone Baseline Priority Scheduler reproducing the IEEE Access 2023 priority scheduling approach:
 * <em>"Task Scheduling in Cloud Computing: A Priority-Based Heuristic Approach"</em>
 * (Lipsa, Dash, Ivkovic, Cengiz, DOI: 10.1109/ACCESS.2023.3255781).
 *
 * <p><b>Architecture &amp; Algorithm:</b>
 * <ol>
 *   <li><b>Priority Assignment to Tasks (PAT):</b>
 *       Constructs the Waiting Time Matrix (\(WM\)) and calculates baseline task priorities in \((0, 1)\)
 *       using {@link PriorityAssignment} [Directly specified by IEEE paper].</li>
 *   <li><b>Fibonacci Heap Priority Queue:</b>
 *       Tasks are populated into a standalone {@link FibonacciHeap} [Directly specified by IEEE paper].
 *       Ordering is strictly governed by {@link HeapKey} without using Java {@code PriorityQueue}.</li>
 *   <li><b>VM Selection &amp; Dispatching:</b>
 *       Extracts the highest-priority task from the heap and assigns it to the earliest available VM
 *       in a non-preemptive multi-server queuing model (\(M/M/n\)) [Clearly implied by paper].</li>
 *   <li><b>Strict Baseline Separation:</b>
 *       Deadline has <em>zero influence</em> on task priorities, heap ordering, or VM selection.
 *       Deadline miss is solely recorded post-execution as an evaluation metric in {@link SchedulingResult}.</li>
 *   <li><b>Deterministic Tie-Breaking:</b>
 *       When priorities are identical, tie-breaking evaluates earlier arrival time, then lowest Task ID.</li>
 * </ol>
 */
public class BaselinePriorityScheduler {

    /** Default reference MIPS for nominal task execution duration scaling. */
    public static final double DEFAULT_REFERENCE_MIPS = 1000.0;

    private final List<CloudVmSpec> vms;
    private final double referenceMips;
    private final List<VmAssignment> assignments;
    private final List<SchedulingResult> results;

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
     * Constructs a baseline scheduler with the specified VM specifications and default reference MIPS.
     *
     * @param vms list of VM specifications
     * @throws IllegalArgumentException if vms is null or empty
     */
    public BaselinePriorityScheduler(List<CloudVmSpec> vms) {
        this(vms, DEFAULT_REFERENCE_MIPS);
    }

    /**
     * Constructs a baseline scheduler with the specified VM specifications and custom reference MIPS.
     *
     * @param vms           list of VM specifications
     * @param referenceMips reference MIPS baseline against which nominal execution times are scaled
     * @throws IllegalArgumentException if vms is null/empty or referenceMips <= 0
     */
    public BaselinePriorityScheduler(List<CloudVmSpec> vms, double referenceMips) {
        if (vms == null || vms.isEmpty()) {
            throw new IllegalArgumentException("VM specifications cannot be null or empty");
        }
        if (referenceMips <= 0.0) {
            throw new IllegalArgumentException("Reference MIPS must be positive, got: " + referenceMips);
        }

        this.vms = Collections.unmodifiableList(new ArrayList<>(vms));
        this.referenceMips = referenceMips;
        this.assignments = new ArrayList<>();
        this.results = new ArrayList<>();
    }

    /**
     * Convenience constructor for a fixed number of homogeneous VMs (1000 MIPS each).
     *
     * @param vmCount number of homogeneous VMs
     */
    public BaselinePriorityScheduler(int vmCount) {
        this(createHomogeneousVms(vmCount), DEFAULT_REFERENCE_MIPS);
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
     * Executes the complete baseline scheduling pipeline:
     * 1. Admits tasks dynamically based on arrival timestamps.
     * 2. Derives dynamic priorities using IEEE PAT Algorithm 1 for admitted tasks.
     * 3. Enqueues tasks into the Fibonacci Heap.
     * 4. Dispatches tasks to VMs based on earliest availability.
     * 5. Transitions task lifecycle states and records results.
     *
     * @param inputTasks list of tasks to schedule
     * @return unmodifiable list of scheduling results
     * @throws IllegalArgumentException if inputTasks is null or empty
     */
    public List<SchedulingResult> schedule(List<Task> inputTasks) {
        if (inputTasks == null || inputTasks.isEmpty()) {
            throw new IllegalArgumentException("Input tasks cannot be null or empty");
        }

        this.assignments.clear();
        this.results.clear();

        // Sort incoming tasks by arrival time, then task ID
        List<Task> remainingTasks = new ArrayList<>(inputTasks);
        remainingTasks.sort(Comparator.comparingDouble(Task::getArrivalTime)
            .thenComparingLong(Task::getTaskId));

        // Initialize VM trackers
        List<VmTracker> vmTrackers = new ArrayList<>(vms.size());
        for (CloudVmSpec spec : vms) {
            vmTrackers.add(new VmTracker(spec));
        }

        FibonacciHeap<HeapKey, Task> readyQueue = new FibonacciHeap<>();
        int nextTaskIdx = 0;
        int totalTasks = remainingTasks.size();

        while (nextTaskIdx < totalTasks || !readyQueue.isEmpty()) {
            // If ready queue is empty, advance simulation time to next arriving task
            if (readyQueue.isEmpty()) {
                double nextArrival = remainingTasks.get(nextTaskIdx).getArrivalTime();
                List<Task> arrivalBatch = new ArrayList<>();
                while (nextTaskIdx < totalTasks && remainingTasks.get(nextTaskIdx).getArrivalTime() <= nextArrival + 1e-9) {
                    arrivalBatch.add(remainingTasks.get(nextTaskIdx));
                    nextTaskIdx++;
                }

                // Compute priorities using IEEE PAT Algorithm 1 if not already pre-assigned
                boolean hasPreassigned = arrivalBatch.stream().allMatch(t -> t.getDynamicPriority() > 0.0);
                if (!hasPreassigned) {
                    new PriorityAssignment(arrivalBatch);
                }

                // Insert into Fibonacci Heap
                for (Task t : arrivalBatch) {
                    t.markReady();
                    double primaryScore = 1.0 - t.getDynamicPriority();
                    double secondaryScore = t.getArrivalTime();
                    HeapKey key = HeapKey.of(primaryScore, secondaryScore, t.getTaskId());
                    readyQueue.insert(key, t);
                }
            }

            // Extract highest priority task from ready queue
            Task task = readyQueue.extractMin();

            // Select earliest available VM
            VmTracker selectedVm = selectEarliestAvailableVm(vmTrackers, task.getArrivalTime());
            double startTime = Math.max(task.getArrivalTime(), selectedVm.readyTime);

            // Admit any tasks that arrived while waiting for VM
            List<Task> newlyArrived = new ArrayList<>();
            while (nextTaskIdx < totalTasks && remainingTasks.get(nextTaskIdx).getArrivalTime() <= startTime + 1e-9) {
                newlyArrived.add(remainingTasks.get(nextTaskIdx));
                nextTaskIdx++;
            }
            if (!newlyArrived.isEmpty()) {
                boolean hasPreassigned = newlyArrived.stream().allMatch(t -> t.getDynamicPriority() > 0.0);
                if (!hasPreassigned) {
                    new PriorityAssignment(newlyArrived);
                }
                for (Task t : newlyArrived) {
                    t.markReady();
                    double primaryScore = 1.0 - t.getDynamicPriority();
                    double secondaryScore = t.getArrivalTime();
                    HeapKey key = HeapKey.of(primaryScore, secondaryScore, t.getTaskId());
                    readyQueue.insert(key, t);
                }
            }

            // Execute task on selected VM
            double speedRatio = this.referenceMips / selectedVm.spec.mips();
            double actualDuration = task.getExecutionTime() * speedRatio;
            double completionTime = startTime + actualDuration;

            selectedVm.readyTime = completionTime;

            // Transition Task lifecycle
            task.markRunning(startTime, selectedVm.spec.id());
            task.markFinished(completionTime);

            // Record VmAssignment and SchedulingResult
            VmAssignment assignment = new VmAssignment(
                task.getTaskId(),
                selectedVm.spec.id(),
                task.getArrivalTime(),
                startTime,
                completionTime,
                actualDuration
            );
            this.assignments.add(assignment);

            SchedulingResult result = SchedulingResult.fromTask(task);
            this.results.add(result);
        }

        return Collections.unmodifiableList(new ArrayList<>(this.results));
    }

    /**
     * Selects the earliest available VM.
     * Deterministic tie-breaking:
     * - If multiple VMs have the same earliest ready time, prefer the faster VM (higher MIPS).
     * - If MIPS are also equal, prefer the lower VM ID.
     */
    private VmTracker selectEarliestAvailableVm(List<VmTracker> trackers, double arrivalTime) {
        return trackers.stream().min((v1, v2) -> {
            double effectiveReady1 = Math.max(v1.readyTime, arrivalTime);
            double effectiveReady2 = Math.max(v2.readyTime, arrivalTime);

            int cmpReady = Double.compare(effectiveReady1, effectiveReady2);
            if (cmpReady != 0) {
                return cmpReady;
            }

            // Tie-breaker 1: Higher MIPS (faster processing)
            int cmpMips = Double.compare(v2.spec.mips(), v1.spec.mips());
            if (cmpMips != 0) {
                return cmpMips;
            }

            // Tie-breaker 2: Lower VM ID
            return Long.compare(v1.spec.id(), v2.spec.id());
        }).orElseThrow(() -> new IllegalStateException("No VMs available"));
    }

    // ==========================================
    // Query Methods
    // ==========================================

    public List<CloudVmSpec> getVms() {
        return vms;
    }

    public double getReferenceMips() {
        return referenceMips;
    }

    public List<VmAssignment> getAssignments() {
        return Collections.unmodifiableList(assignments);
    }

    public List<SchedulingResult> getResults() {
        return Collections.unmodifiableList(results);
    }
}
