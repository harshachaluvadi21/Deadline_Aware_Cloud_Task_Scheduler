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
- **Dynamic Heap Rebuilding:** Recomputes composite scores for all ready tasks at decision points to reflect clock progression accurately.

---

### Slide 7: System Architecture
- **Two-Tier Decoupled Architecture:**
  1. **Research Experiment Layer (Frozen, Reproducible):**
     - CloudSim Plus 8.0.0 simulation engine.
     - Standalone min-oriented `FibonacciHeap<T>`.
     - Controlled experimental framework with deep workload cloning.
  2. **Interactive Web Application Layer:**
     - Spring Boot 3.3.4 REST API backend.
     - React 18 + Vite + TypeScript interactive analytics dashboard.
     - In-memory execution without touching frozen research campaign artifacts.

---

### Slide 8: Scheduling Workflow & Mathematical Model
- **Dynamic Priority Score Formula:**
  $$S(t) = W_p \cdot P_{\text{norm}} + W_d \cdot U_{\text{deadline}}(t) + W_w \cdot W_{\text{wait}}(t)$$
  - Initial Design Weights: $W_p = 0.35$, $W_d = 0.50$, $W_w = 0.15$
- **Deadline Urgency:**
  $$U_{\text{deadline}}(t) = \exp\left( -k \cdot \frac{\text{Slack}(t)}{\text{ExecutionTime}} \right)$$
  where $\text{Slack}(t) = \text{Deadline} - (t + \text{ExecutionTime})$, with initial design parameter $k = 2.0$.
- **Critical Urgency Clamp:** When $\text{Slack}(t) \le 0$, $U_{\text{deadline}}(t) = 1.0$.

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
