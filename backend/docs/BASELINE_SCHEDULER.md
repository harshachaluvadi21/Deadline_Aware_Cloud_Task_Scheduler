# Baseline IEEE Priority-Based Task Scheduler

## 1. Purpose

This document provides a comprehensive technical reference for the **Baseline IEEE Priority-Based Scheduler** implemented in Phase 6. The scheduler faithfully reproduces the existing priority-driven heuristic approach described in the IEEE Access 2023 research paper:

> **Swati Lipsa, Ranjan Kumar Dash, Nikola Ivković, and Korhan Cengiz (2023)**  
> *"Task Scheduling in Cloud Computing: A Priority-Based Heuristic Approach"*  
> **Journal:** *IEEE Access*, Volume 11, pp. 27111–27126  
> **DOI:** [10.1109/ACCESS.2023.3255781](https://doi.org/10.1109/ACCESS.2023.3255781)

The baseline scheduler serves as the rigorous empirical control group for this mini-project. It is strictly isolated from any proposed deadline-aware scheduling mechanisms so that downstream experimental evaluations (Phases 11–14) can definitively measure the benefits of the proposed multi-factor deadline-aware enhancement over the published IEEE reference heuristic.

---

## 2. IEEE Paper Reference & Conceptual Mapping

The reference paper models cloud scheduling using an $M/M/n$ multi-server queuing framework and addresses scheduling latency using three core components:
1. **Waiting Time Matrix ($WM$ / $WTM$):** A square comparison matrix modeling cumulative sequential delay.
2. **Priority Assignment to Tasks (PAT - Algorithm 1):** Derives normalized task priorities $\in (0, 1)$ using dominant eigenvalue and eigenvector analysis based on Analytic Hierarchy Process (AHP) consistency principles.
3. **Fibonacci Heap Task Queue:** Stores queued tasks ordered by dynamic priority to support $O(1)$ amortized insertion and $O(\log m)$ extraction.

### Concept-to-Class Mapping Table

| IEEE Paper Concept | Source Location / Section | Implementation Class | Traceability Category | Role & Description |
|---|---|---|---|---|
| Sequential Waiting Time $WT(T_k)$ | Section III | [`WaitingTimeMatrix`](file:///c:/4-1%20Mini%20Project/mini_project/src/main/java/scheduler/baseline/WaitingTimeMatrix.java) | **A. Directly Specified** | Computes sequential waiting times: $WT(T_1)=0$, $WT(T_k)=\sum_{j=1}^{k-1} PT(T_j)$. |
| Waiting Matrix $WM$ | Section III & Alg. 1 | [`WaitingTimeMatrix`](file:///c:/4-1%20Mini%20Project/mini_project/src/main/java/scheduler/baseline/WaitingTimeMatrix.java) | **A. Directly Specified** | $m \times m$ matrix: diagonal $= 1.0$, upper triangle $= WT(T_j)$, lower triangle $= 1.0 / WT(T_i)$. |
| PAT (Algorithm 1) | Section III, Alg. 1 | [`PriorityAssignment`](file:///c:/4-1%20Mini%20Project/mini_project/src/main/java/scheduler/baseline/PriorityAssignment.java) | **A. Directly Specified** | Power Iteration eigenvector calculation, Consistency Index ($CI$), Consistency Ratio ($CR < 0.1$). |
| Priority Score $Priority(T_k)$ | Lemma 1 & Alg. 1 | [`PriorityAssignment`](file:///c:/4-1%20Mini%20Project/mini_project/src/main/java/scheduler/baseline/PriorityAssignment.java) | **A. Directly Specified** | $Priority(T_k) = 1.0 - egvt[k] \in (0, 1)$, assigning higher priority to tasks facing longer sequential wait. |
| Priority Queue Structure | Section III, Alg. 2 | [`FibonacciHeap`](file:///c:/4-1%20Mini%20Project/mini_project/src/main/java/scheduler/heap/FibonacciHeap.java) | **A. Directly Specified** | Dedicated standalone Min-Oriented Fibonacci Heap with composite `HeapKey`. |
| Dispatching & VM Assignment | Section III, Alg. 2 | [`BaselinePriorityScheduler`](file:///c:/4-1%20Mini%20Project/mini_project/src/main/java/scheduler/baseline/BaselinePriorityScheduler.java) | **B. Clearly Implied** | Extracts tasks by priority and dispatches to the earliest available VM in the $M/M/n$ pool. |
| Deterministic Tie-Breaking | Not explicitly defined | [`HeapKey`](file:///c:/4-1%20Mini%20Project/mini_project/src/main/java/scheduler/heap/HeapKey.java) | **C. Implementation Assumption** | Breaks priority ties first by earlier arrival time, then deterministically by lowest Task ID. |
| VM Speed Scaling ($E_{ij}$) | Section IV ($M/M/n$) | [`BaselinePriorityScheduler`](file:///c:/4-1%20Mini%20Project/mini_project/src/main/java/scheduler/baseline/BaselinePriorityScheduler.java) | **C. Implementation Assumption** | Scales nominal duration by VM MIPS: $E_{ij} = \text{nominalDuration} \times \frac{\text{RefMIPS}}{\text{VmMIPS}_j}$. |
| Post-Execution Outcome | Performance metrics | [`SchedulingResult`](file:///c:/4-1%20Mini%20Project/mini_project/src/main/java/scheduler/baseline/SchedulingResult.java) | **C. Implementation Assumption** | Immutable container recording timestamps and metrics. Deadline miss is strictly a post-hoc observation. |

---

## 3. Waiting-Time Mechanism

### Mathematical Formulation
For a sequence of $m$ tasks $\{T_1, T_2, \dots, T_m\}$ arriving in sequence $T_1 \succ T_2 \succ \dots \succ T_m$:

$$WT(T_1) = 0$$

$$WT(T_k) = \sum_{j=1}^{k-1} PT(T_j) \quad \text{for } k \ge 2$$

where $PT(T_j)$ is the nominal processing/execution duration of task $T_j$.

### Waiting Time Matrix Structure
The square matrix $WM \in \mathbb{R}^{m \times m}$ is populated as:

$$WM = \begin{bmatrix}
1 & WT(T_2) & WT(T_3) & \dots & WT(T_m) \\
\frac{1}{WT(T_2)} & 1 & WT(T_3) & \dots & WT(T_m) \\
\frac{1}{WT(T_3)} & \frac{1}{WT(T_3)} & 1 & \dots & WT(T_m) \\
\vdots & \vdots & \vdots & \ddots & \vdots \\
\frac{1}{WT(T_m)} & \frac{1}{WT(T_m)} & \frac{1}{WT(T_m)} & \dots & 1
\end{bmatrix}$$

- **Diagonal:** $WM[i][i] = 1.0$.
- **Upper Triangle ($i < j$):** $WM[i][j] = WT(T_j)$ (sequential waiting delay of task $j$).
- **Lower Triangle ($i > j$):** $WM[i][j] = \frac{1.0}{WM[j][i]} = \frac{1.0}{WT(T_i)}$ (reciprocal pairwise comparison).
- **Single-task edge case ($m=1$):** Yields $[[1.0]]$.

---

## 4. Priority Assignment Mechanism (PAT - Algorithm 1)

Algorithm 1 applies Analytic Hierarchy Process (AHP) eigen-decomposition to the Waiting Matrix:

1. **Power Iteration Method:** Computes the principal eigenvector $egvt$ corresponding to the dominant eigenvalue $\lambda_{max}$:
   $$v^{(t+1)} = \frac{WM \cdot v^{(t)}}{\|WM \cdot v^{(t)}\|_1}$$
   Iterated until $\|v^{(t+1)} - v^{(t)}\|_\infty < 10^{-9}$.
   
2. **Dominant Eigenvalue:**
   $$\lambda_{max} = \frac{1}{m} \sum_{i=1}^m \frac{(WM \cdot v)_i}{v_i}$$

3. **Consistency Verification:**
   - **Consistency Index (CI):**
     $$CI = \frac{\lambda_{max} - m}{m - 1} \quad (m > 1)$$
   - **Consistency Ratio (CR):**
     $$CR = \frac{CI}{RI_m}$$
     where $RI_m$ is Saaty's standard Random Index:
     $[0, 0, 0.58, 0.90, 1.12, 1.24, 1.32, 1.41, 1.45, 1.49]$.
   - **Convergence Check:** If $CR < 0.1$, the pairwise comparisons are consistent. If $CR \ge 0.1$, the maximum waiting time entry is iteratively adjusted by scaling down by $0.90$ until convergence.

4. **Task Priority Derivation (Lemma 1):**
   $$Priority(T_k) = 1.0 - egvt[k]$$
   Because $\sum egvt = 1.0$ and each $egvt[k] \in (0, 1)$, $Priority(T_k)$ is strictly bounded in $(0, 1)$.
   Tasks facing longer sequential waiting delays receive smaller eigenvector components, and therefore receive higher priority scores ($1 - egvt_k$) to mitigate starvation and minimize total waiting time.

---

## 5. Fibonacci Heap Integration

The scheduler utilizes the custom, generic **Min-Oriented Fibonacci Heap** (`scheduler.heap.FibonacciHeap<HeapKey, Task>`) implemented and verified in Phase 5.

### Key Mapping & Deterministic Tie-Breaking
The composite key `HeapKey` guarantees a strict total order:

```java
double primaryScore = 1.0 - task.getDynamicPriority();
double secondaryScore = task.getArrivalTime();
long sequenceId = task.getTaskId();

HeapKey key = HeapKey.of(primaryScore, secondaryScore, sequenceId);
```

- **`primaryScore` ($1.0 - \text{dynamicPriority}$):** Maps the maximum-priority requirement to the min-heap root. A task with high priority (e.g. 0.95) has a small primary score (0.05) and is extracted first.
- **`secondaryScore` ($\text{arrivalTime}$):** Breaks priority ties by earlier arrival time.
- **`sequenceId` ($\text{taskId}$):** Final, strict deterministic tie-breaker by ascending Task ID.

---

## 6. VM Selection & Dispatching Flow

The scheduler models a dynamic $M/M/n$ heterogeneous cloud datacenter:
1. **Dynamic Task Admission:** Tasks are admitted at discrete event timestamps as simulation time advances.
2. **Batch Prioritization:** Admitted tasks are prioritized via PAT (Algorithm 1) and inserted into the `FibonacciHeap`.
3. **Earliest Available VM Allocation:**
   When extracting the next highest-priority task from the heap:
   - Evaluates the ready time of all VMs: $\text{effectiveReady}_j = \max(\text{readyTime}_j, \text{arrivalTime})$.
   - Selects the VM that becomes available earliest.
   - **Tie-Breaking among idle VMs:** Selects the faster VM (higher MIPS), followed by lowest VM ID.
4. **Execution Duration:**
   $$\text{actualDuration} = \text{executionTime} \times \frac{\text{Reference MIPS}}{\text{VM MIPS}_j}$$
5. **Non-Preemptive Completion:** The task runs continuously until completion. The VM ready time is updated to $startTime + actualDuration$.

---

## 7. Strict Baseline Separation & Zero Deadline Awareness

In strict compliance with project specifications:
- **No Deadline in Priority:** Task deadlines ($D_i$) are never passed into `PriorityAssignment`, `WaitingTimeMatrix`, or `HeapKey`.
- **No Earliest Deadline First (EDF):** Urgent deadlines cannot leapfrog higher-priority tasks.
- **No Deadline-Based VM Selection:** VMs are chosen solely based on earliest availability and computing capacity.
- **Post-Hoc Metric Only:** The `deadlineMissed` field in `SchedulingResult` is strictly an observational measurement ($completionTime > deadline$) computed after task execution terminates.

---

## 8. Assumptions & Source Ambiguities

| Component | IEEE Paper Specification Status | Ambiguity / Gap | Implementation Decision & Rationale |
|---|---|---|---|
| **Tie-Breaking** | Underspecified | Paper does not define behavior when two tasks have identical priorities. | Strict hierarchical tie-breaking: earlier arrival time first, lowest Task ID second. |
| **Physical VM Specifications** | Abstract queuing ($M/M/n$) | Paper parameterizes servers by service rate $\mu$, with no Host RAM, storage, or MIPS definitions. | Integrated with CloudSim Plus Heterogeneous VM environment (500 to 2500 MIPS) via standard reference MIPS scaling. |
| **Large Workload RI Table** | Table ends at $m=10$ | Paper tests up to 50 tasks but only quotes standard AHP Random Index values up to $m=10$. | Extended using the standard Golden-Wang AHP approximation formula: $RI_m \approx 1.98 \times \frac{m - 2}{m}$ for $m > 10$. |
| **Preemption Execution** | Mentioned abstractly | Mentions preemptive mode conditionally but does not specify preemption checkpointing, overhead, or resume penalties. | Implemented pure non-preemptive event-driven scheduling as approved in Decision 3 of `DESIGN_DECISIONS.md`. |

---

## 9. Computational Complexity

- **Matrix Construction:** $O(m^2)$ where $m$ is the number of admitted tasks.
- **Eigenvector Calculation (Power Iteration):** $O(k \cdot m^2)$ where $k \le 1000$ (typically converges in $< 30$ iterations).
- **PAT Algorithm 1 Worst-Case:** $O(m^3)$ as reported by the authors.
- **Fibonacci Heap Operations:**
  - Insert: $O(1)$ amortized.
  - Extract-Min: $O(\log m)$ amortized.
- **VM Selection:** $O(n)$ where $n$ is the number of VMs.
- **Overall Scheduler Runtime:** $O(m^3 + m \log m + m \cdot n)$, highly scalable for benchmark evaluations (20, 50, 100 tasks).

---

## 10. Test Coverage & Verification

The baseline implementation is verified by a dedicated test suite with **29 tests** (69 total workspace tests):
1. **[`WaitingTimeMatrixTest`](file:///c:/4-1%20Mini%20Project/mini_project/src/test/java/scheduler/baseline/WaitingTimeMatrixTest.java) (8 tests):**
   - Sequential waiting times: $WT(T_1)=0$, $WT(T_2)=PT(T_1)$, etc.
   - Diagonal elements equal $1.0$.
   - Upper triangle values and lower triangle reciprocals.
   - Single task matrix ($m=1$).
   - Defensive copies, out-of-bounds guards, and alias compatibility.
2. **[`PriorityAssignmentTest`](file:///c:/4-1%20Mini%20Project/mini_project/src/test/java/scheduler/baseline/PriorityAssignmentTest.java) (10 tests):**
   - Lemma 1 verification: priority values strictly within $(0, 1)$.
   - Ordering verification: tasks facing longer waiting delays receive higher priority scores.
   - Principal eigenvector normalization ($\sum v_i = 1.0$).
   - Consistency Ratio convergence ($CR < 0.1$).
   - Deterministic reproducibility across consecutive runs.
   - Equal execution times and single task handling.
3. **[`BaselinePrioritySchedulerTest`](file:///c:/4-1%20Mini%20Project/mini_project/src/test/java/scheduler/baseline/BaselinePrioritySchedulerTest.java) (11 tests):**
   - Fibonacci Heap insertion and priority extraction ordering.
   - Deterministic tie-breaking by Task ID.
   - Event-driven timing: staggered arrival times and queuing delays while busy.
   - Heterogeneous VM scaling and earliest available VM selection.
   - Verification that deadline has zero influence on baseline priority ordering.
   - Post-hoc deadline miss observation in `SchedulingResult`.
   - End-to-end 10-task heterogeneous cluster execution.
   - 100% deterministic reproducibility across multiple runs.
