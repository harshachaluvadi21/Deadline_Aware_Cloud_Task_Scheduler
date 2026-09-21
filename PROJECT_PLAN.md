# PROJECT PLAN: Deadline-Aware Priority-Based Task Scheduling in Cloud Computing

## 1. Project Overview & Objective
This project is an academic research-oriented mini-project that extends the priority-based heuristic task scheduling approach from the IEEE Access 2023 paper:
> **Swati Lipsa, Ranjan Kumar Dash, Nikola Ivkovic, Korhan Cengiz (IEEE Access 2023)**:
> *"Task Scheduling in Cloud Computing: A Priority-Based Heuristic Approach"*

### Core Objective
The existing baseline system schedules tasks based on the **Priority Assignment to Tasks (PAT)** algorithm, utilizing a **Waiting Time Matrix (WTM)** and an $M/M/n$ queuing framework with priority queueing implemented via a **Fibonacci Heap**.

Our proposed system enhances the scheduling process by introducing **deadline awareness**.

The proposed scheduler must evaluate:
- Task Priority
- Task Deadline
- Task Arrival Time
- Task Execution Time
- Waiting Time
- VM workload / resource availability

The objective is to investigate whether deadline-aware scheduling can reduce deadline misses while maintaining effective resource utilization and scheduling performance.

---

## 2. Environment & Tooling Verification
- **Operating System:** Windows 11 (amd64)
- **Java Platform:** JDK 21 LTS (`maven.compiler.source=21`, `maven.compiler.target=21`)
- **Build Tool:** Apache Maven 3.9.12
- **Version Control:** Git 2.48.1.windows.1
- **Simulation Engine:** CloudSim Plus `8.0.0` (`org.cloudsimplus:cloudsimplus`)
- **Priority Queue Implementation:** Dedicated standalone Fibonacci Heap (`FibonacciHeap<T>`)

---

## 3. Development Phases

| Phase | Description | Status | Deliverables |
|---|---|---|---|
| **Phase 0** | Workspace inspection, requirements analysis, architecture, project plan, and design decision identification | **COMPLETED** | `PROJECT_PLAN.md`, `ARCHITECTURE.md`, `DESIGN_DECISIONS.md` |
| **Phase 1** | Maven project setup, directory scaffolding, `.gitignore`, initial Git commit | **COMPLETED** | `pom.xml`, project skeleton, Git initialized |
| **Phase 2** | CloudSim Plus 8.0.0 dependency setup and verification on JDK 21 | **COMPLETED** | Verified `pom.xml` dependencies, `CloudSimCompatibilityTest.java`, `docs/CLOUDSIM_PLUS_API_VERIFICATION.md` |
| **Phase 3** | CloudSim Plus simulation environment configuration (Datacenter, Hosts, VMs) | **COMPLETED** | `CloudSimEnvironment.java`, `SimulationScenario.java`, `CloudVmSpec.java`, `docs/SIMULATION_ENVIRONMENT.md` |
| **Phase 4** | Common Task Data Model (shared identically by Baseline & Proposed) | PENDING | `Task.java`, `TaskStatus.java` |
| **Phase 5** | Standalone Fibonacci Heap implementation & rigorous unit tests | PENDING | `FibonacciHeap.java`, `FibonacciNode.java`, heap tests |
| **Phase 6** | Baseline Priority Scheduler reproducing IEEE 2023 PAT algorithm & Waiting Time Matrix | PENDING | `WaitingTimeControlMatrix.java`, `PriorityAssignmentToTasks.java`, `BaselineScheduler.java` |
| **Phase 7** | Baseline simulation execution & validation with controlled task sets | PENDING | Baseline execution harness, baseline unit tests |
| **Phase 8** | Design and document the Deadline-Aware Priority Formula & Tie-Breaking rules | **STOP FOR HUMAN REVIEW** | Formula specification in documentation (no coding yet) |
| **Phase 9** | Implement Proposed Deadline-Aware Scheduler (accounting for 6 mandatory factors) | PENDING | `DeadlineAwareScheduler.java`, `DeadlinePriorityCalculator.java` |
| **Phase 10** | Metrics Engine implementation (Makespan, Waiting Time, Turnaround, DMR, Throughput, bounded VM Utilization) | PENDING | `PerformanceMetrics.java`, `MetricsCalculator.java` |
| **Phase 11** | Controlled Experiment Scenarios (Normal, High, Deadline-Sensitive, Mixed-Priority, High Load; 20, 50, 100 tasks) | PENDING | `WorkloadGenerator.java`, fixed benchmark datasets |
| **Phase 12** | Experimental evaluation: Identical workload execution on Baseline vs Proposed | PENDING | Automated benchmark runner |
| **Phase 13** | Raw simulation results generation (CSV) | PENDING | CSV files in `results/baseline/`, `results/proposed/`, `results/comparison/` |
| **Phase 14** | Comparative tables and graph data generation | PENDING | `comparison.csv`, markdown summary tables, chart data |
| **Phase 15** | Comprehensive test suite execution (Edge cases, consistency, reproducibility) | PENDING | Full JUnit test suite covering all cases |
| **Phase 16** | Verification against Project Abstract and presentation goals | PENDING | Compliance verification checklist |
| **Phase 17** | Final documentation and academic presentation/viva preparation guide | PENDING | `README.md`, `docs/`, viva Q&A guide |

---

## 4. Requirements Traceability

### 4.1 Clearly Defined Requirements
1. **Common Task Model:** Both baseline and proposed must schedule identical task objects containing at minimum: `taskId`, `priority`, `arrivalTime`, `executionTime`, `deadline`, and `status`.
2. **Deterministic & Identical Workloads:** Baseline and Proposed schedulers must be tested against the exact same VM configurations and task sets (20, 50, 100 tasks).
3. **Dedicated Fibonacci Heap:** Priority queue must be a custom, isolated Fibonacci Heap data structure rather than relying solely on `java.util.PriorityQueue`.
4. **No Artificial Results:** Every number in `results/` must be generated through reproducible CloudSim Plus simulation runs.
5. **No Technology Bloat:** Pure Java + Maven + CloudSim Plus + JUnit. No Spring Boot, Python, web frameworks, or external databases.
6. **Isolated Algorithms:** Baseline logic (`scheduler.baseline.*`) and Proposed logic (`scheduler.deadline.*`) must remain strictly decoupled.
7. **VM Utilization Metric Definition:** Strictly bounded:
   $$\text{Utilization} = \frac{\sum_{j=1}^M \text{BusyTime}_j}{M \times \text{Makespan}} \times 100\% \le 100\%$$

### 4.2 Ambiguous Requirements & Design Decisions Requiring Human Approval
1. **Deadline-Aware Priority Calculation Formula:** Must combine: base priority, deadline, arrival time, execution time, waiting time, and VM resource availability. Currently **PENDING HUMAN REVIEW** (Phase 8).
2. **Tie-Breaking Strategy:** Priority order: (1) Earlier Deadline, (2) Longer Waiting Time, (3) Lower Task ID.
3. **Task Scheduling Paradigm:** Non-preemptive event-driven dynamic dispatching.
4. **Admission/Drop Policy for Overdue Tasks:** Overdue tasks continue execution to preserve workload throughput, but are strictly counted as missed deadlines.
