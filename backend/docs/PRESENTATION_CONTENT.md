# Final Project Presentation Content: Slide-by-Slide Outline

**Project Title:** Deadline-Aware Priority-Based Task Scheduling in Cloud Computing  
**Domain:** Cloud Computing, Heuristic Task Scheduling, Discrete-Event Simulation  
**Target Audience:** Mini-Project Evaluation Panel, Faculty Evaluators, Academic Reviewers  

---

### Slide 1: Title & Overview
- **Title:** Deadline-Aware Priority-Based Task Scheduling in Cloud Computing
- **Subtitle:** Enhancing Heuristic Cloud Task Scheduling via Dynamic Urgency and Discrete-Event Simulation
- **Presented by:** B.Tech Mini-Project Team
- **Key Focus:** Extending the IEEE Access 2023 priority scheduling approach (*"Task Scheduling in Cloud Computing: A Priority-Based Heuristic Approach"* by Lipsa et al.) with dynamic deadline awareness and an interactive simulation testbed.

---

### Slide 2: Problem Statement
- Cloud datacenters receive high-velocity, heterogeneous workloads from competing tenants.
- Tasks differ widely in nominal execution times, base priorities, and service-level agreement (SLA) deadlines.
- **The Dilemma:**
  - Traditional static priority algorithms favor high-priority jobs, starving urgent lower-priority tasks and driving up deadline miss rates.
  - Pure Earliest Deadline First (EDF) ignores priority hierarchies, leading to priority inversion under overload.
- **Core Challenge:** Designing a multi-criteria heuristic that respects base priorities, penalizes impending deadline misses, and achieves balanced cluster resource utilization.

---

### Slide 3: Motivation & Need
- Modern cloud computing architectures (IaaS, FaaS) bind revenue and reputation to SLA compliance.
- Unnecessary deadline misses lead to financial penalties, SLA violations, and degraded QoS.
- Existing priority algorithms often neglect time-decaying deadline slack.
- Need for a rigorous, reproducible, discrete-event simulation framework to evaluate whether dynamic deadline awareness improves deadline compliance without degrading cluster throughput or makespan.

---

### Slide 4: Existing Approach (IEEE Access 2023)
- Reference work: Swati Lipsa, Ranjan Kumar Dash, Nikola Ivkovic, Korhan Cengiz (IEEE Access 2023).
- **Core Techniques:**
  1. **Priority Assignment to Tasks (PAT):** Calculates nominal priority vectors.
  2. **Waiting Time Matrix (WTM):** Tracks expected queuing delays across virtual machines.
  3. **Priority Queueing via Fibonacci Heap:** Employs an $M/M/n$ queuing framework with $\mathcal{O}(1)$ amortized insertion and $\mathcal{O}(\log n)$ extraction.
  4. **Task-to-VM Dispatch:** Allocates extracted tasks to VMs minimizing completion times.
- **Limitation:** The baseline heuristic relies solely on nominal priority and waiting time, making it blind to task deadlines.

---

### Slide 5: Research Gap
- **Lack of Time-Varying Urgency:** In the baseline, a task with 1 second of remaining slack receives the same priority as an identical task with 50 seconds of slack.
- **Overdue Task Handling:** Fixed-priority heuristics lack mechanisms to elevate aging tasks before their deadlines expire.
- **Need for Balanced Objectives:** Cloud providers cannot sacrifice overall resource utilization or makespan merely to satisfy deadlines. A multi-objective formulation is essential.

---

### Slide 6: Proposed Approach (Deadline-Aware Priority Scheduler)
- Introduces a dynamic, multi-objective composite priority scoring function evaluated at each scheduling event.
- **Scoring Factors:**
  1. Base Task Priority ($P_{\text{norm}}$)
  2. Dynamic Deadline Urgency ($U_{\text{deadline}}(t)$)
  3. Normalized Waiting Time ($W_{\text{wait}}(t)$)
- **Urgency Modeling:** Exponential decay function based on remaining task slack relative to execution duration.
- **Persistent Fibonacci Min-Heap:** Maintains a single persistent Min-Heap with $O(1)$ amortized `decreaseKey()` updates as simulation time advances, eliminating redundant heap reallocations.

---

### Slide 7: System Architecture
- **Two-Tier Decoupled Architecture:**
  1. **Simulation Kernel Layer:**
     - CloudSim Plus 8.0.0 discrete-event cloud simulation engine.
     - Standalone persistent min-oriented `FibonacciHeap<HeapKey, Task>`.
     - Controlled experimental framework with identical workload dispatch to baseline and proposed schedulers.
  2. **Interactive Web Application Layer:**
     - Spring Boot 3.3.4 REST API backend.
     - React 18 + Vite + TypeScript interactive analytics dashboard.
     - Optional live worker agent for real physical CPU cryptographic hashing.

---

### Slide 8: Proposed Algorithm Workflow & Formulations

#### 1. Proposed Algorithm Flowchart
```
Cloud Tasks
    ↓
Calculate Dynamic Deadline Urgency Ω(t)
    ↓
Calculate Dynamic Priority P(t)
    ↓
Insert into Persistent Fibonacci Heap
    ↓
Select Highest-Priority Task
    ↓
Execute Cloud Task
    ↓
Advance Simulation Time
    ↓
Recalculate P(t) for Waiting Tasks
    ↓
decreaseKey()
    ↓
Select Next Task
    ↓
Repeat until all tasks are completed
```

#### 2. Representative Cloud Workload Types
*Standardized benchmark profiles (Task_ID, Task Type, Deadline, Burst Time, Priority, Waiting Time):*

| Task ID | Task Type | Burst Time | Deadline | Base Priority |
|:-------:|:----------|:----------:|:--------:|:-------------:|
| **T1** | Image Processing | 8 s | 20 s | 3 |
| **T2** | Video Transcoding | 15 s | 25 s | 5 |
| **T3** | Database Query | 4 s | 12 s | 4 |
| **T4** | ML Model Inference | 10 s | 18 s | 5 |
| **T5** | Log Analysis | 6 s | 30 s | 2 |
| **T6** | File Compression | 7 s | 22 s | 3 |
| **T7** | Data Analytics | 12 s | 28 s | 4 |
| **T8** | Backup Processing | 20 s | 45 s | 1 |

*(Distinction: Representative cloud workload types provide reproducible benchmark scenarios; Real cloud execution refers to the Render-deployed live CPU worker agent).*

#### 3. Mathematical Formulations
- **Component 1 (Dynamic Deadline Urgency Algorithm):**
  $$\Omega(t) = \left[ \frac{1000}{\text{EffectiveSlack}(t) + 1} \right] \times \left[ 1 + \frac{B}{D_{\text{effective}}(t) + 1} \right]$$
  where $\text{Slack}(t) = (\text{deadline} - t) - B$, $\text{EffectiveSlack}(t) = \max(0, \text{Slack}(t))$, and $D_{\text{effective}}(t) = \max(0, \text{deadline} - t)$.
- **Component 2 (Dynamic Priority Algorithm):**
  $$P(t) = 100 \times \text{basePriority} + \Omega(t) + 10 \times W(t)$$
  where $\text{basePriority}$ is immutable, and $W(t) = \max(0, t - \text{arrivalTime})$.
- **Component 3 (Persistent Fibonacci-Heap Scheduling Algorithm):**
  Persistent Min-Heap with key $(-P(t), \text{deadline}, \text{taskId})$, updated via `decreaseKey()` ($O(1)$ amortized) and dispatched via `extractMin()` ($O(\log n)$ amortized).

---

### Slide 9: Technology Stack
- **Backend Core:** Java 21 LTS, Spring Boot 3.3.4, Apache Maven
- **Discrete-Event Simulation:** CloudSim Plus 8.0.0
- **Priority Queue:** Custom generic `FibonacciHeap<K, V>`
- **Frontend Framework:** React 18, TypeScript 5.6, Vite 5.4, Vanilla CSS
- **Testing & Quality:** JUnit Jupiter 5, MockMvc, Spring Boot Test (135 passing tests, 0 failures)

---

### Slide 10: Experimental Methodology
- **Controlled Comparison Principle:** Identical task sets, arrival times, durations, base priorities, deadlines, and VM environments dispatched to both schedulers.
- **Experimental Matrix (24 Runs):**
  - 4 Workload Scenarios: Normal Load, High Load, Tight Deadlines, Mixed Load.
  - 3 Task Scales: 20, 50, and 100 tasks.
  - 2 Schedulers: IEEE Baseline vs. Proposed Deadline-Aware.
  - Deterministic seeds (1001 to 4003) guaranteeing 100% reproducible execution traces.

---

### Slide 11: Evaluation Metrics
- **Makespan ($s$):** Total schedule completion time ($\max(C_i) - \min(A_i)$).
- **Average Waiting Time ($s$):** Mean delay between task arrival and execution start.
- **Average Turnaround Time ($s$):** Mean duration from arrival to completion.
- **Throughput ($\text{tasks}/s$):** Completed tasks per unit of simulation time.
- **Deadline Miss Rate (DMR, $\%$):** Percentage of tasks whose completion exceeded deadline.
- **Time-based VM Resource Utilization ($\%$):**
  $$\text{Utilization} = \frac{\sum_{j=1}^M \text{BusyTime}_j}{M \times \text{Makespan}} \times 100\%$$
- **Comparative Difference:** Strictly reported as $\Delta = \text{Proposed} - \text{Baseline}$.

---

### Slide 12: Experimental Results & Observations
- **Deadline Compliance:** Under tight deadline and high queuing conditions, the proposed scheduler substantially curtailed deadline miss rates (e.g. from 40% down to 5% in representative tight workloads).
- **Resource Utilization:** VM resource utilization remained tightly comparable between baseline and proposed algorithms, showing that deadline awareness does not cause resource underutilization.
- **Makespan Trade-off:** Makespan differences were minimal and workload-dependent, reflecting that the scheduler reorders tasks for urgency rather than introducing idle gaps.
- **Scientific Objectivity:** No algorithm is declared universally superior; results reflect observed trade-offs across synthetic scenarios.

---

### Slide 13: Interactive Web Dashboard
- Translates research algorithms into an interactive demonstration testbed:
  - **Workload Summary Cards:** Instant overview of workload volume, VM counts, and priority distribution.
  - **Side-by-Side Comparison:** Comparative tables with exact numerical differences.
  - **Multi-Metric Charts:** Comparative bars for all 6 metrics.
  - **Deadline Adherence Analysis:** Segmented classification of tasks completed before deadline, at deadline, or missed deadline.
  - **VM Utilization Cards:** Per-VM busy times and load percentages for VMs 0–3.

---

### Slide 14: Demonstration & Workload Capabilities
- **Deterministic Demo Workload:** Instant 1-click loading of a 10-task benchmark demonstrating deadline sensitivity.
- **CSV Workload Ingestion:** Drag-and-drop file upload with all-or-nothing validation (rejects invalid headers, non-numeric values, duplicate IDs, and inverted deadlines).
- **Interactive Gantt Timeline:** Real simulation timestamps mapped across VM rows with hover tooltips and time intervals.
- **Display Filters:** Filter by VM, completion status, priority, and deadline category.
- **Research CSV Export:** Download execution logs (`tasks-result.csv`) and metrics summaries (`metrics-summary.csv`).

---

### Slide 15: Conclusion & Future Scope
- **Conclusions:**
  - Successfully reproduced and validated the IEEE Access 2023 priority scheduling approach.
  - Demonstrated that incorporating dynamic deadline urgency into Fibonacci-heap priority queues significantly improves deadline adherence under queuing pressure.
  - Validated 100% reproducibility across a 24-run experimental campaign and built a complete interactive web dashboard.
- **Future Scope:**
  - Evaluation against real-world production cloud traces (e.g., Google Cluster Traces).
  - Investigating preemptive scheduling mechanisms for safety-critical tasks.
  - Incorporating energy-aware dynamic VM voltage and frequency scaling (DVFS).
