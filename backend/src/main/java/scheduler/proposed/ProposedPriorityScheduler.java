package scheduler.proposed;

import scheduler.baseline.VmAssignment;
import scheduler.heap.FibonacciHeap;
import scheduler.heap.FibonacciNode;
import scheduler.heap.HeapKey;
import scheduler.model.CloudVmSpec;
import scheduler.model.Task;
import scheduler.model.TaskStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the Persistent Fibonacci-Heap Scheduling Algorithm
 * for the proposed deadline-aware cloud task scheduling method.
 *
 * <p><b>Algorithm Name:</b>
 * Persistent Fibonacci-Heap Scheduling Algorithm
 *
 * <p><b>Execution Modes:</b>
 * Supports both:
 * <ul>
 *   <li>{@link SchedulingMode#NON_PREEMPTIVE}: Selected task runs to completion without interruption.</li>
 *   <li>{@link SchedulingMode#PREEMPTIVE}: Running tasks may be preempted at discrete scheduling events
 *       (e.g., higher-priority task arrival). The interrupted task preserves its remaining execution time
 *       and is reinserted into the persistent Fibonacci heap.</li>
 * </ul>
 *
 * <p><b>Persistent Heap Guarantee:</b>
 * Both modes utilize a single persistent Min-Oriented Fibonacci Heap across the entire simulation run.
 * Waiting task priorities are updated in-place via {@code decreaseKey()} in \(O(1)\) amortized time.
 * The heap is never discarded or rebuilt.
 */
public class ProposedPriorityScheduler {

    public static final double DEFAULT_REFERENCE_MIPS = 1000.0;

    private final List<CloudVmSpec> vms;
    private final double referenceMips;
    private final DeadlineAwarePriorityCalculator priorityCalculator;
    private final SchedulingMode schedulingMode;
    private final List<VmAssignment> assignments;
    private final List<ProposedSchedulingResult> results;
    private final Map<Long, TaskContext> taskContexts;
    private int totalPreemptions;

    /**
     * Internal execution tracker maintaining complete lifecycle information for a task.
     */
    public static class TaskContext {
        private final Task task;
        private final double originalExecutionTime;
        private double remainingExecutionTime;
        private TaskState state;
        private double firstStartTime;
        private double completionTime;
        private double accumulatedCpuTime;
        private int preemptionCount;
        private double lastDispatchedScore;
        private double lastDispatchedUrgency;
        private long allocatedVmId;

        public TaskContext(Task task) {
            this.task = task;
            this.originalExecutionTime = task.getExecutionTime();
            this.remainingExecutionTime = task.getExecutionTime();
            this.state = TaskState.WAITING;
            this.firstStartTime = -1.0;
            this.completionTime = -1.0;
            this.accumulatedCpuTime = 0.0;
            this.preemptionCount = 0;
            this.lastDispatchedScore = 0.0;
            this.lastDispatchedUrgency = 0.0;
            this.allocatedVmId = -1L;
        }

        public Task getTask() { return task; }
        public long getTaskId() { return task.getTaskId(); }
        public int getBasePriority() { return task.getPriority(); }
        public double getArrivalTime() { return task.getArrivalTime(); }
        public double getDeadline() { return task.getDeadline(); }
        public double getOriginalExecutionTime() { return originalExecutionTime; }
        public double getRemainingExecutionTime() { return remainingExecutionTime; }
        public TaskState getState() { return state; }
        public double getStartTime() { return firstStartTime; }
        public double getCompletionTime() { return completionTime; }
        public double getCpuExecutionTimeAccumulated() { return accumulatedCpuTime; }
        public int getPreemptionCount() { return preemptionCount; }
        public double getLastDispatchedScore() { return lastDispatchedScore; }
        public double getLastDispatchedUrgency() { return lastDispatchedUrgency; }
        public long getAllocatedVmId() { return allocatedVmId; }

        public double getWaitingTime() {
            if (completionTime >= 0.0) {
                return Math.max(0.0, (completionTime - task.getArrivalTime()) - accumulatedCpuTime);
            }
            return (firstStartTime >= 0.0) ? Math.max(0.0, firstStartTime - task.getArrivalTime()) : 0.0;
        }

        public double getTurnaroundTime() {
            return (completionTime >= 0.0) ? Math.max(0.0, completionTime - task.getArrivalTime()) : 0.0;
        }
    }

    /**
     * Internal mutable tracker for VM availability in non-preemptive simulation.
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
     * Internal tracker for VM state during preemptive discrete-event simulation.
     */
    private static class PreemptiveVmTracker {
        final CloudVmSpec spec;
        TaskContext runningContext;
        double expectedFinishTime;

        PreemptiveVmTracker(CloudVmSpec spec) {
            this.spec = spec;
            this.runningContext = null;
            this.expectedFinishTime = 0.0;
        }

        boolean isBusy() {
            return runningContext != null;
        }
    }

    public ProposedPriorityScheduler(List<CloudVmSpec> vms) {
        this(vms, DEFAULT_REFERENCE_MIPS, new DeadlineAwarePriorityCalculator(), SchedulingMode.NON_PREEMPTIVE);
    }

    public ProposedPriorityScheduler(List<CloudVmSpec> vms, SchedulingMode mode) {
        this(vms, DEFAULT_REFERENCE_MIPS, new DeadlineAwarePriorityCalculator(), mode);
    }

    public ProposedPriorityScheduler(List<CloudVmSpec> vms, double referenceMips) {
        this(vms, referenceMips, new DeadlineAwarePriorityCalculator(), SchedulingMode.NON_PREEMPTIVE);
    }

    public ProposedPriorityScheduler(List<CloudVmSpec> vms, double referenceMips, SchedulingMode mode) {
        this(vms, referenceMips, new DeadlineAwarePriorityCalculator(), mode);
    }

    public ProposedPriorityScheduler(int vmCount) {
        this(createHomogeneousVms(vmCount), DEFAULT_REFERENCE_MIPS, new DeadlineAwarePriorityCalculator(), SchedulingMode.NON_PREEMPTIVE);
    }

    public ProposedPriorityScheduler(int vmCount, SchedulingMode mode) {
        this(createHomogeneousVms(vmCount), DEFAULT_REFERENCE_MIPS, new DeadlineAwarePriorityCalculator(), mode);
    }

    public ProposedPriorityScheduler(
            List<CloudVmSpec> vms,
            double referenceMips,
            DeadlineAwarePriorityCalculator priorityCalculator) {
        this(vms, referenceMips, priorityCalculator, SchedulingMode.NON_PREEMPTIVE);
    }

    public ProposedPriorityScheduler(
            List<CloudVmSpec> vms,
            double referenceMips,
            DeadlineAwarePriorityCalculator priorityCalculator,
            SchedulingMode mode) {

        if (vms == null || vms.isEmpty()) {
            throw new IllegalArgumentException("VM specifications cannot be null or empty");
        }
        if (!Double.isFinite(referenceMips) || referenceMips <= 0.0) {
            throw new IllegalArgumentException("Reference MIPS must be positive, got: " + referenceMips);
        }
        if (priorityCalculator == null) {
            throw new IllegalArgumentException("Priority calculator cannot be null");
        }
        if (mode == null) {
            throw new IllegalArgumentException("Scheduling mode cannot be null");
        }

        this.vms = Collections.unmodifiableList(new ArrayList<>(vms));
        this.referenceMips = referenceMips;
        this.priorityCalculator = priorityCalculator;
        this.schedulingMode = mode;
        this.assignments = new ArrayList<>();
        this.results = new ArrayList<>();
        this.taskContexts = new HashMap<>();
        this.totalPreemptions = 0;
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
     * Executes dynamic, deadline-aware priority scheduling using a persistent Fibonacci Min-Heap
     * according to the configured {@link SchedulingMode}.
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
        this.taskContexts.clear();
        this.totalPreemptions = 0;

        for (Task task : inputTasks) {
            taskContexts.put(task.getTaskId(), new TaskContext(task));
        }

        if (this.schedulingMode == SchedulingMode.NON_PREEMPTIVE) {
            return scheduleNonPreemptive(inputTasks);
        } else {
            return schedulePreemptive(inputTasks);
        }
    }

    // ============================================================
    // MODE 1: NON-PREEMPTIVE PERSISTENT FIBONACCI HEAP SCHEDULER
    // ============================================================
    private List<ProposedSchedulingResult> scheduleNonPreemptive(List<Task> inputTasks) {
        List<Task> pendingTasks = new ArrayList<>(inputTasks);
        pendingTasks.sort(Comparator.comparingDouble(Task::getArrivalTime)
            .thenComparingLong(Task::getTaskId));

        List<VmTracker> vmTrackers = new ArrayList<>(vms.size());
        for (CloudVmSpec spec : vms) {
            vmTrackers.add(new VmTracker(spec));
        }

        FibonacciHeap<HeapKey, Task> readyQueue = new FibonacciHeap<>();
        Map<Long, FibonacciNode<HeapKey, Task>> activeNodes = new HashMap<>();

        int pendingIdx = 0;
        int totalTasks = pendingTasks.size();
        double currentClock = 0.0;

        while (pendingIdx < totalTasks || !readyQueue.isEmpty()) {
            if (readyQueue.isEmpty()) {
                currentClock = Math.max(currentClock, pendingTasks.get(pendingIdx).getArrivalTime());
            } else {
                double earliestVmReady = vmTrackers.stream()
                    .mapToDouble(v -> v.readyTime)
                    .min()
                    .orElse(0.0);
                currentClock = Math.max(currentClock, earliestVmReady);
            }

            // Admit arriving tasks
            while (pendingIdx < totalTasks && pendingTasks.get(pendingIdx).getArrivalTime() <= currentClock + 1e-9) {
                Task arriving = pendingTasks.get(pendingIdx);
                TaskContext ctx = taskContexts.get(arriving.getTaskId());
                ctx.state = TaskState.WAITING;

                if (arriving.getStatus() == TaskStatus.SUBMITTED) {
                    arriving.markReady();
                }

                double initialPriority = priorityCalculator.calculateDynamicPriority(arriving, currentClock);
                arriving.setDynamicPriority(initialPriority);

                HeapKey key = HeapKey.of(-initialPriority, arriving.getDeadline(), arriving.getTaskId());
                FibonacciNode<HeapKey, Task> node = readyQueue.insert(key, arriving);
                activeNodes.put(arriving.getTaskId(), node);
                pendingIdx++;
            }

            if (readyQueue.isEmpty()) {
                continue;
            }

            // Recalculate dynamic priority P(t) for active waiting tasks in O(1) amortized decreaseKey()
            for (Map.Entry<Long, FibonacciNode<HeapKey, Task>> entry : activeNodes.entrySet()) {
                FibonacciNode<HeapKey, Task> node = entry.getValue();
                Task waitingTask = node.getValue();
                double updatedPriority = priorityCalculator.calculateDynamicPriority(waitingTask, currentClock);
                waitingTask.setDynamicPriority(updatedPriority);

                HeapKey updatedKey = HeapKey.of(-updatedPriority, waitingTask.getDeadline(), waitingTask.getTaskId());
                if (updatedKey.compareTo(node.getKey()) < 0) {
                    readyQueue.decreaseKey(node, updatedKey);
                }
            }

            // Extract highest dynamic priority task
            Task selectedTask = readyQueue.extractMin();
            activeNodes.remove(selectedTask.getTaskId());

            TaskContext ctx = taskContexts.get(selectedTask.getTaskId());
            VmTracker selectedVm = selectEarliestAvailableVm(vmTrackers, selectedTask.getArrivalTime());
            double startTime = Math.max(selectedTask.getArrivalTime(), selectedVm.readyTime);

            double speedRatio = this.referenceMips / selectedVm.spec.mips();
            double actualDuration = selectedTask.getExecutionTime() * speedRatio;
            double completionTime = startTime + actualDuration;

            selectedVm.readyTime = completionTime;

            ctx.state = TaskState.RUNNING;
            ctx.firstStartTime = startTime;
            ctx.allocatedVmId = selectedVm.spec.id();
            ctx.accumulatedCpuTime = actualDuration;
            ctx.remainingExecutionTime = 0.0;
            ctx.completionTime = completionTime;
            ctx.state = TaskState.COMPLETED;

            if (selectedTask.getStatus() == TaskStatus.READY) {
                selectedTask.markRunning(startTime, selectedVm.spec.id());
            }
            selectedTask.markFinished(completionTime);

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

            ctx.lastDispatchedScore = dispatchBreakdown.finalScore();
            ctx.lastDispatchedUrgency = dispatchBreakdown.deadlineUrgency();

            ProposedSchedulingResult result = new ProposedSchedulingResult(
                selectedTask.getTaskId(),
                selectedVm.spec.id(),
                selectedTask.getArrivalTime(),
                startTime,
                completionTime,
                selectedTask.getExecutionTime(),
                ctx.getWaitingTime(),
                ctx.getTurnaroundTime(),
                selectedTask.getPriority(),
                dispatchBreakdown.finalScore(),
                dispatchBreakdown.deadlineUrgency(),
                selectedTask.getDeadline(),
                completionTime > selectedTask.getDeadline(),
                0
            );
            this.results.add(result);

            currentClock = Math.max(currentClock, startTime);
        }

        return Collections.unmodifiableList(new ArrayList<>(this.results));
    }

    // ============================================================
    // MODE 2: PREEMPTIVE PERSISTENT FIBONACCI HEAP SCHEDULER
    // ============================================================
    private List<ProposedSchedulingResult> schedulePreemptive(List<Task> inputTasks) {
        List<Task> pendingTasks = new ArrayList<>(inputTasks);
        pendingTasks.sort(Comparator.comparingDouble(Task::getArrivalTime)
            .thenComparingLong(Task::getTaskId));

        List<PreemptiveVmTracker> vmTrackers = new ArrayList<>(vms.size());
        for (CloudVmSpec spec : vms) {
            vmTrackers.add(new PreemptiveVmTracker(spec));
        }

        FibonacciHeap<HeapKey, Task> readyQueue = new FibonacciHeap<>();
        Map<Long, FibonacciNode<HeapKey, Task>> activeNodes = new HashMap<>();

        int pendingIdx = 0;
        int totalTasks = pendingTasks.size();
        double currentClock = 0.0;

        while (pendingIdx < totalTasks || !readyQueue.isEmpty() || hasBusyVm(vmTrackers)) {
            // Synchronize clock when heap is empty and no VM is busy
            if (readyQueue.isEmpty() && !hasBusyVm(vmTrackers)) {
                currentClock = Math.max(currentClock, pendingTasks.get(pendingIdx).getArrivalTime());
            }

            // Step 1: Admit newly arrived tasks at currentClock
            while (pendingIdx < totalTasks && pendingTasks.get(pendingIdx).getArrivalTime() <= currentClock + 1e-9) {
                Task arriving = pendingTasks.get(pendingIdx);
                TaskContext ctx = taskContexts.get(arriving.getTaskId());
                ctx.state = TaskState.WAITING;

                if (arriving.getStatus() == TaskStatus.SUBMITTED) {
                    arriving.markReady();
                }

                double initialPriority = priorityCalculator.calculateDynamicPriority(
                    arriving, currentClock, ctx.remainingExecutionTime
                );
                arriving.setDynamicPriority(initialPriority);

                HeapKey key = HeapKey.of(-initialPriority, arriving.getDeadline(), arriving.getTaskId());
                FibonacciNode<HeapKey, Task> node = readyQueue.insert(key, arriving);
                activeNodes.put(arriving.getTaskId(), node);
                pendingIdx++;
            }

            // Step 2: Recalculate dynamic priority P(t) for all active waiting tasks in O(1) decreaseKey()
            for (Map.Entry<Long, FibonacciNode<HeapKey, Task>> entry : activeNodes.entrySet()) {
                FibonacciNode<HeapKey, Task> node = entry.getValue();
                Task waitingTask = node.getValue();
                TaskContext ctx = taskContexts.get(waitingTask.getTaskId());

                double updatedPriority = priorityCalculator.calculateDynamicPriority(
                    waitingTask, currentClock, ctx.remainingExecutionTime
                );
                waitingTask.setDynamicPriority(updatedPriority);

                HeapKey updatedKey = HeapKey.of(-updatedPriority, waitingTask.getDeadline(), waitingTask.getTaskId());
                if (updatedKey.compareTo(node.getKey()) < 0) {
                    readyQueue.decreaseKey(node, updatedKey);
                }
            }

            // Step 3: First dispatch waiting tasks to any currently idle VMs
            while (!readyQueue.isEmpty() && hasIdleVm(vmTrackers)) {
                PreemptiveVmTracker idleVm = selectBestIdleVm(vmTrackers);
                Task selectedTask = readyQueue.extractMin();
                activeNodes.remove(selectedTask.getTaskId());

                dispatchToVm(idleVm, selectedTask, currentClock);
            }

            // Step 4: Check for preemption if all VMs are busy and a waiting task has higher dynamic priority
            while (!readyQueue.isEmpty() && allVmsBusy(vmTrackers)) {
                FibonacciNode<HeapKey, Task> topWaitingNode = readyQueue.peekNode();
                Task topWaiting = topWaitingNode.getValue();
                TaskContext topWaitingCtx = taskContexts.get(topWaiting.getTaskId());
                double topWaitingP = priorityCalculator.calculateDynamicPriority(
                    topWaiting, currentClock, topWaitingCtx.remainingExecutionTime
                );

                // Find running task with lowest dynamic priority among busy VMs
                PreemptiveVmTracker lowestRunningVm = null;
                double lowestRunningP = Double.MAX_VALUE;
                for (PreemptiveVmTracker vm : vmTrackers) {
                    Task runningTask = vm.runningContext.task;
                    TaskContext runningCtx = vm.runningContext;
                    double runP = priorityCalculator.calculateDynamicPriority(
                        runningTask, currentClock, runningCtx.remainingExecutionTime
                    );
                    if (runP < lowestRunningP) {
                        lowestRunningP = runP;
                        lowestRunningVm = vm;
                    }
                }

                // Preempt if waiting task priority strictly exceeds lowest running task priority
                if (lowestRunningVm != null && topWaitingP > lowestRunningP + 1e-9) {
                    this.totalPreemptions++;
                    TaskContext preemptedCtx = lowestRunningVm.runningContext;
                    preemptedCtx.preemptionCount++;
                    preemptedCtx.state = TaskState.PREEMPTED;

                    // Reinsert preempted task into persistent Fibonacci heap with preserved remaining burst
                    double preemptedP = priorityCalculator.calculateDynamicPriority(
                        preemptedCtx.task, currentClock, preemptedCtx.remainingExecutionTime
                    );
                    preemptedCtx.task.setDynamicPriority(preemptedP);

                    HeapKey preemptKey = HeapKey.of(-preemptedP, preemptedCtx.task.getDeadline(), preemptedCtx.task.getTaskId());
                    FibonacciNode<HeapKey, Task> pNode = readyQueue.insert(preemptKey, preemptedCtx.task);
                    activeNodes.put(preemptedCtx.task.getTaskId(), pNode);

                    // Free VM and immediately dispatch the new highest priority task to it
                    lowestRunningVm.runningContext = null;
                    Task nextTask = readyQueue.extractMin();
                    activeNodes.remove(nextTask.getTaskId());
                    dispatchToVm(lowestRunningVm, nextTask, currentClock);
                } else {
                    break;
                }
            }

            // Step 5: Advance simulation clock to next meaningful discrete event
            double nextArrival = (pendingIdx < totalTasks)
                ? pendingTasks.get(pendingIdx).getArrivalTime()
                : Double.POSITIVE_INFINITY;

            double earliestCompletion = Double.POSITIVE_INFINITY;
            for (PreemptiveVmTracker vm : vmTrackers) {
                if (vm.isBusy() && vm.expectedFinishTime < earliestCompletion) {
                    earliestCompletion = vm.expectedFinishTime;
                }
            }

            if (Double.isInfinite(nextArrival) && Double.isInfinite(earliestCompletion)) {
                break;
            }

            if (!hasBusyVm(vmTrackers)) {
                currentClock = nextArrival;
                continue;
            }

            double nextEventTime = Math.min(nextArrival, earliestCompletion);
            double dt = Math.max(0.0, nextEventTime - currentClock);

            // Execute on all busy VMs during dt
            for (PreemptiveVmTracker vm : vmTrackers) {
                if (vm.isBusy()) {
                    double speedRatio = this.referenceMips / vm.spec.mips();
                    double nominalConsumed = dt / speedRatio;
                    vm.runningContext.remainingExecutionTime = Math.max(0.0, vm.runningContext.remainingExecutionTime - nominalConsumed);
                    vm.runningContext.accumulatedCpuTime += nominalConsumed;
                }
            }

            currentClock = nextEventTime;

            // Handle task completions at currentClock
            for (PreemptiveVmTracker vm : vmTrackers) {
                if (vm.isBusy() && (vm.runningContext.remainingExecutionTime <= 1e-9 || currentClock >= vm.expectedFinishTime - 1e-9)) {
                    TaskContext finishedCtx = vm.runningContext;
                    finishedCtx.state = TaskState.COMPLETED;
                    finishedCtx.completionTime = currentClock;
                    finishedCtx.remainingExecutionTime = 0.0;

                    Task t = finishedCtx.task;
                    if (t.getStatus() == TaskStatus.READY) {
                        t.markRunning(finishedCtx.firstStartTime, finishedCtx.allocatedVmId);
                    }
                    t.markFinished(currentClock);

                    VmAssignment assignment = new VmAssignment(
                        t.getTaskId(),
                        finishedCtx.allocatedVmId,
                        t.getArrivalTime(),
                        finishedCtx.firstStartTime,
                        currentClock,
                        finishedCtx.accumulatedCpuTime
                    );
                    this.assignments.add(assignment);

                    ProposedSchedulingResult result = new ProposedSchedulingResult(
                        t.getTaskId(),
                        finishedCtx.allocatedVmId,
                        t.getArrivalTime(),
                        finishedCtx.firstStartTime,
                        currentClock,
                        finishedCtx.originalExecutionTime,
                        finishedCtx.getWaitingTime(),
                        finishedCtx.getTurnaroundTime(),
                        t.getPriority(),
                        finishedCtx.lastDispatchedScore,
                        finishedCtx.lastDispatchedUrgency,
                        t.getDeadline(),
                        currentClock > t.getDeadline(),
                        finishedCtx.preemptionCount
                    );
                    this.results.add(result);

                    vm.runningContext = null;
                }
            }
        }

        return Collections.unmodifiableList(new ArrayList<>(this.results));
    }

    private void dispatchToVm(PreemptiveVmTracker vm, Task task, double currentClock) {
        TaskContext ctx = taskContexts.get(task.getTaskId());
        if (ctx.firstStartTime < 0.0) {
            ctx.firstStartTime = currentClock;
        }
        ctx.allocatedVmId = vm.spec.id();
        ctx.state = TaskState.RUNNING;

        double speedRatio = this.referenceMips / vm.spec.mips();
        double remainingDuration = ctx.remainingExecutionTime * speedRatio;
        vm.expectedFinishTime = currentClock + remainingDuration;
        vm.runningContext = ctx;

        DeadlineAwarePriorityCalculator.ScoreBreakdown dispatchBreakdown =
            priorityCalculator.calculateScoreBreakdown(task, currentClock, ctx.remainingExecutionTime);
        ctx.lastDispatchedScore = dispatchBreakdown.finalScore();
        ctx.lastDispatchedUrgency = dispatchBreakdown.deadlineUrgency();
    }

    private boolean hasBusyVm(List<PreemptiveVmTracker> trackers) {
        for (PreemptiveVmTracker vm : trackers) {
            if (vm.isBusy()) return true;
        }
        return false;
    }

    private boolean hasIdleVm(List<PreemptiveVmTracker> trackers) {
        for (PreemptiveVmTracker vm : trackers) {
            if (!vm.isBusy()) return true;
        }
        return false;
    }

    private boolean allVmsBusy(List<PreemptiveVmTracker> trackers) {
        for (PreemptiveVmTracker vm : trackers) {
            if (!vm.isBusy()) return false;
        }
        return true;
    }

    private PreemptiveVmTracker selectBestIdleVm(List<PreemptiveVmTracker> trackers) {
        return trackers.stream()
            .filter(v -> !v.isBusy())
            .min((v1, v2) -> {
                int cmpMips = Double.compare(v2.spec.mips(), v1.spec.mips());
                if (cmpMips != 0) return cmpMips;
                return Long.compare(v1.spec.id(), v2.spec.id());
            })
            .orElseThrow(() -> new IllegalStateException("No idle VMs available"));
    }

    private VmTracker selectEarliestAvailableVm(List<VmTracker> trackers, double arrivalTime) {
        return trackers.stream().min((v1, v2) -> {
            double effectiveReady1 = Math.max(v1.readyTime, arrivalTime);
            double effectiveReady2 = Math.max(v2.readyTime, arrivalTime);

            int cmpReady = Double.compare(effectiveReady1, effectiveReady2);
            if (cmpReady != 0) {
                return cmpReady;
            }

            int cmpMips = Double.compare(v2.spec.mips(), v1.spec.mips());
            if (cmpMips != 0) {
                return cmpMips;
            }

            return Long.compare(v1.spec.id(), v2.spec.id());
        }).orElseThrow(() -> new IllegalStateException("No VMs available"));
    }

    public List<CloudVmSpec> getVms() { return vms; }
    public double getReferenceMips() { return referenceMips; }
    public DeadlineAwarePriorityCalculator getPriorityCalculator() { return priorityCalculator; }
    public SchedulingMode getSchedulingMode() { return schedulingMode; }
    public int getTotalPreemptions() { return totalPreemptions; }
    public int getPreemptionCount() { return totalPreemptions; }
    public Map<Long, TaskContext> getTaskContexts() { return Collections.unmodifiableMap(taskContexts); }
    public TaskContext getTaskContext(long taskId) { return taskContexts.get(taskId); }
    public List<VmAssignment> getAssignments() { return Collections.unmodifiableList(assignments); }
    public List<ProposedSchedulingResult> getResults() { return Collections.unmodifiableList(results); }
}
