# DESIGN DECISIONS & REQUIREMENT AMBIGUITY REGISTER

This document categorizes requirements into those that are **Strictly Defined** and those that represent **Design Decisions Requiring Explicit Project Approval**.

---

## 1. Strictly Defined Requirements (Non-Negotiable)

1. **Common Task Model**: Single `Task` entity used without variation across both Baseline and Proposed algorithms (`taskId`, `priority`, `arrivalTime`, `executionTime`, `deadline`, `status`).
2. **Dedicated Fibonacci Heap**: Must use a dedicated, standalone Fibonacci Heap implementation for priority queueing, not standard Java `PriorityQueue`.
3. **Reproducible Experimental Workloads**: Exact same workloads (20, 50, 100 tasks) and identical VM configurations must be fed into both algorithms.
4. **Isolated Modular Architecture**: Baseline code (`scheduler.baseline.*`) and Proposed code (`scheduler.deadline.*`) must remain strictly isolated. No deadline logic may leak into the baseline.
5. **No Synthetic Results**: All reported metrics must emerge directly from simulated execution inside CloudSim Plus.
6. **Core Metrics & Revised Mathematical Definitions**:
   - **Makespan**: $\max_{i}(C_i) - \min_{i}(A_i)$
   - **Waiting Time**: $W_i = S_i - A_i$ (where $S_i$ is execution start time, $A_i$ is arrival time)
   - **Turnaround Time**: $T_i = C_i - A_i$ (where $C_i$ is completion time)
   - **Deadline Miss Rate (DMR)**: $\text{DMR} = \frac{\sum_{i=1}^N \mathbb{I}(C_i > D_i)}{N} \times 100\%$
   - **Throughput**: $\frac{N}{\text{Makespan}}$ (tasks completed per unit time)
   - **VM / Resource Utilization (Bounded)**:
     $$\text{Utilization} = \frac{\sum_{j=1}^M \text{BusyTime}_j}{M \times \text{Makespan}} \times 100\%$$
     where $\text{BusyTime}_j$ is the total active execution time of VM $j$, $M$ is total number of VMs, and the measurement interval is the makespan. This definition guarantees that utilization is strictly bounded in $[0\%, 100\%]$ and cannot exceed $100\%$.
7. **Java Version**: JDK 21 (LTS) for compilation and target bytecode (`maven.compiler.source=21`, `maven.compiler.target=21`).
8. **CloudSim Plus Framework**: `org.cloudsimplus:cloudsimplus:8.0.0` (active modern release compatible with JDK 21; note that the legacy artifact `cloudsim-plus` is deprecated).

---

## 2. Explicit Mapping of the Baseline Algorithm (IEEE Access 2023 Paper)

The baseline scheduler reproduces the priority-based heuristic from:
> **Lipsa, Dash, Ivkovic, and Cengiz (IEEE Access 2023)**: *"Task Scheduling in Cloud Computing: A Priority-Based Heuristic Approach"*

### 2.1 Paper-Derived Logic
1. **Queuing Model Framework**: The cloud task arrival and execution flow is modeled using an $M/M/n$ queuing framework with multiple heterogeneous VMs ($n$ processing servers).
2. **Waiting Time Matrix (WTM / WM)**:
   - When a set of tasks arrives, a Waiting Time Matrix $\mathbf{W}$ is constructed where each element $W_{ik}$ reflects the waiting time / queuing delay for task $i$ considering task execution attributes and server capabilities.
   - The matrix captures the relative waiting delay among queued tasks.
3. **Priority Assignment to Tasks (PAT - Algorithm 1 in Paper)**:
   - From the Waiting Time Matrix $\mathbf{W}$, the priority vector is derived mathematically (using the principal eigenvector / normalized row-weight aggregation).
   - Priority values are strictly bounded in the range $[0, 1]$.
4. **Fibonacci Heap Ordering**:
   - The computed PAT priority values serve as keys to populate a Fibonacci Heap.
   - Highest-priority tasks are extracted in $O(1)$ amortized time for assignment.
5. **No Deadline Factors**: The paper's PAT algorithm contains zero deadline awareness.

### 2.2 Implementation Adaptations in CloudSim Plus
- CloudSim Plus provides a discrete-event simulation engine rather than continuous-time queuing approximations.
- In our simulation, the **Waiting Time Matrix (WTM)** is instantiated for active tasks arriving at simulation time $t$, capturing their accumulated waiting time and projected wait across VM queues.
- The PAT procedure normalizes these delays to produce the baseline dynamic priority score $\in [0, 1]$.
- A custom, standalone `FibonacciHeap` manages the dispatching queue without external third-party queue libraries.

### 2.3 Proposed Modifications (Strictly in Proposed Algorithm Only)
- In the proposed algorithm, the baseline priority is augmented with multi-factor deadline awareness (slack time, remaining execution time, deadline urgency, VM capacity).
- **The baseline remains completely untouched by deadline logic.**

---

## 3. Ambiguities & Design Decisions Requiring Human Review

### Decision 1: Proposed Deadline-Aware Priority Calculation Formula
> [!IMPORTANT]
> **Status:** PENDING HUMAN REVIEW (Must NOT be implemented until explicitly approved prior to Phase 9)
> 
> The proposed scheduler must account for the 6 mandatory factors:
> 1. **Base Priority** ($P_i$)
> 2. **Task Deadline** ($D_i$)
> 3. **Task Arrival Time** ($A_i$)
> 4. **Task Execution Time** ($E_i$)
> 5. **Waiting Time** ($W_i(t) = t - A_i$)
> 6. **VM / Resource Availability & Capacity** (e.g., VM MIPS, queue delay)

#### Open Status:
- The exact combination function (linear combination, exponential urgency, or ratio-based) is **NOT yet finalized**.
- Any suggested weights (e.g., $0.4 / 0.4 / 0.2$) are **hypothetical illustrative candidates and are NOT approved**.
- Formal candidate formulas with tunable parameters will be submitted during **Phase 8** for human review and sign-off before Phase 9 coding begins.

---

### Decision 2: Deterministic Tie-Breaking Rule
> [!NOTE]
> When two tasks compute identical scheduling scores in the heap:
> - **Primary Rule**: Earlier Deadline ($D_i < D_j$)
> - **Secondary Rule**: Longer Waiting Time ($W_i > W_j$)
> - **Tertiary Rule**: Deterministic Task ID order ($taskId_i < taskId_j$)

---

### Decision 3: Task Scheduling Paradigm (Non-Preemptive Event-Driven)
> [!NOTE]
> Scheduling assignments occur at discrete event boundaries (e.g., task arrival, VM completion). Non-preemptive execution ensures deterministic makespan without unmodeled preemption penalties.

---

### Decision 4: Overdue Task Handling
> [!NOTE]
> Tasks whose deadlines expire prior to or during execution are allowed to complete execution to preserve total workload accounting, but are strictly flagged and tallied in the **Deadline Miss Rate (DMR)**.
