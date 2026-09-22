# PROJECT PLAN: Deadline-Aware Priority-Based Task Scheduling in Cloud Computing

## 1. Project Overview & Objective
This project is an academic research-oriented mini-project that extends the priority-based heuristic task scheduling approach from the IEEE Access 2023 paper:
> **Swati Lipsa, Ranjan Kumar Dash, Nikola Ivkovic, Korhan Cengiz (IEEE Access 2023)**:
> *"Task Scheduling in Cloud Computing: A Priority-Based Heuristic Approach"*

### Core Objective
The existing baseline system schedules tasks based on the **Priority Assignment to Tasks (PAT)** algorithm, utilizing a **Waiting Time Matrix (WTM)** and an $M/M/n$ queuing framework with priority queueing implemented via a **Fibonacci Heap**.

Our proposed system enhances the scheduling process by introducing **deadline awareness**.

The proposed scheduler evaluates:
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
- **Web Platform:** Spring Boot 3.3.4 (REST API) + React 18 / Vite 5 / TypeScript 5 (Frontend)
- **Priority Queue Implementation:** Dedicated standalone Fibonacci Heap (`FibonacciHeap<T>`)

---

## 3. Development Phases

| Phase | Description | Status | Deliverables |
|---|---|---|---|
| **Phase 0** | Workspace inspection, requirements analysis, architecture, project plan, and design decision identification | **COMPLETED** | `PROJECT_PLAN.md`, `ARCHITECTURE.md`, `DESIGN_DECISIONS.md` |
| **Phase 1** | Maven project setup, directory scaffolding, `.gitignore`, initial Git commit | **COMPLETED** | `pom.xml`, project skeleton, Git initialized |
| **Phase 2** | CloudSim Plus 8.0.0 dependency setup and verification on JDK 21 | **COMPLETED** | Verified `pom.xml` dependencies, `CloudSimCompatibilityTest.java`, `docs/CLOUDSIM_PLUS_API_VERIFICATION.md` |
| **Phase 3** | CloudSim Plus simulation environment configuration (Datacenter, Hosts, VMs) | **COMPLETED** | `CloudSimEnvironment.java`, `SimulationScenario.java`, `CloudVmSpec.java`, `docs/SIMULATION_ENVIRONMENT.md` |
| **Phase 4** | Common Task Data Model (shared identically by Baseline & Proposed) | **COMPLETED** | `Task.java`, `TaskStatus.java`, `TaskModelTest.java`, `docs/TASK_MODEL.md` |
| **Phase 5** | Standalone Fibonacci Heap implementation & rigorous unit tests | **COMPLETED** | `FibonacciHeap.java`, `FibonacciNode.java`, `HeapKey.java`, `docs/FIBONACCI_HEAP.md` |
| **Phase 6** | Baseline Priority Scheduler reproducing IEEE 2023 PAT algorithm & Waiting Time Matrix | **COMPLETED** | `WaitingTimeMatrix.java`, `PriorityAssignment.java`, `BaselinePriorityScheduler.java`, `VmAssignment.java`, `SchedulingResult.java`, `docs/BASELINE_SCHEDULER.md` |
| **Phase 7** | Proposed Deadline-Aware Priority Scheduler & scoring engine | **COMPLETED** | `DeadlineUrgencyCalculator.java`, `DeadlineAwarePriorityCalculator.java`, `ProposedPriorityScheduler.java`, `ProposedSchedulingResult.java`, `docs/PROPOSED_DEADLINE_AWARE_SCHEDULER.md` |
| **Phase 8** | Controlled Experimental Framework, Workload Manifest, and Smoke Experiment | **COMPLETED** | `WorkloadScenarioType.java`, `ExperimentConfig.java`, `WorkloadGenerator.java`, `WorkloadSerializer.java`, `TaskExecutionRecord.java`, `MetricsCalculator.java`, `docs/EXPERIMENT_FRAMEWORK.md` |
| **Phase 9** | Full Evaluation Matrix Execution & Results Generation (24 runs: 4 scenarios x 3 task counts) | **COMPLETED** | `CampaignRunner.java`, `raw_experiment_results.csv`, `final_experiment_manifest.csv`, 24 task-detail CSVs |
| **Phase 10** | Result Analysis, Pairwise Comparisons, & Metric Tabulation | **COMPLETED** | `ResultLoader.java`, `PairwiseComparison.java`, `ResultAggregator.java`, `aggregated_results.csv`, `pairwise_comparison.csv`, `deadline_analysis.csv`, `workload_analysis.csv` |
| **Phase 11** | Result Tables, Graphs, and Research Artifacts | **COMPLETED** | 6 Publication PNG charts (`results/charts/`), `final_metric_table.csv`, `final_pairwise_table.csv`, `RESULTS_SUMMARY.md`, `FINAL_RESULTS_MANIFEST.md` |
| **Phase 12** | Interactive Web Application Foundation | **COMPLETED** | Spring Boot REST API (`web.*`), React + Vite TypeScript Dashboard (`frontend/`), Gantt VM Timeline, Controlled Comparison View, `docs/WEB_APPLICATION_*.md` |
| **Phase 13** | CSV Workload Import & Result Export | **COMPLETED** | `WorkloadCsvService.java`, `WorkloadController.java`, `WorkloadValidationResponse.java`, `CsvUploadModal.tsx`, `ExportButtons.tsx`, `WorkloadImportExportTest.java`, `docs/CSV_WORKLOAD_GUIDE.md` |
| **Phase 14** | Advanced Visualization & Analytics Dashboard | **COMPLETED** | `WorkloadSummaryCards.tsx`, `MetricsCharts.tsx`, `DeadlineAnalysisView.tsx`, `VmUtilizationCards.tsx`, `TaskFilters.tsx`, Enhanced `VmTimeline.tsx` with ticks, tooltips & scroll |
| **Phase 15** | Final Demo Mode & Project Packaging | **COMPLETED** | Deterministic `demo-workload.csv`, Simulation Reset button, Live backend health probe, `docs/USER_GUIDE.md`, Updated `README.md`, `WEB_APPLICATION_ARCHITECTURE.md` |
| **Phase 16** | Final System Validation | **COMPLETED** | Clean build (`mvn clean test`), frontend production build (`npm run build`), API endpoint checks, invalid input verification |
| **Phase 17** | Final Research Verification | **COMPLETED** | Metric consistency checks, strict difference definition ($\text{Proposed} - \text{Baseline}$), limitation documentation, research freeze validation |
| **Phase 18** | Final Project Documentation | **COMPLETED** | Comprehensive updates to `README.md`, `ARCHITECTURE.md`, `PROJECT_PLAN.md`, `USER_GUIDE.md`, `API_DOCUMENTATION.md`, `WEB_APPLICATION_SETUP.md` |
| **Phase 19** | Final Presentation Material | **COMPLETED** | `docs/PRESENTATION_CONTENT.md`, `docs/PRESENTATION_SPEECH.md`, `docs/VIVA_QUESTIONS_AND_ANSWERS.md` (40+ questions) |
| **Phase 20** | Final Live Demo Preparation | **COMPLETED** | `docs/DEMO_SCRIPT.md` (Step-by-step 3–5 minute presentation demonstration guide) |
| **Phase 21** | Final Cleanup & Delivery Check | **COMPLETED** | Clean workspace audit, run commands verification, research integrity check, no accidental changes |
| **Phase 22** | Final Submission Checklist | **COMPLETED** | `docs/FINAL_SUBMISSION_CHECKLIST.md` (Exhaustive verification of code, research, web app, documentation, presentation, and git status) |

> [!NOTE]
> **Separation of Concerns:**
> - **Phases 0–11:** Research and experimental framework completed and permanently frozen.
> - **Phases 12–15:** Interactive web application presentation and simulation layer built around the frozen core.
> - **Phases 16–22:** Final system validation, research verification, presentation/viva preparation, and delivery packaging.

---

## 4. Requirements Traceability

### 4.1 Clearly Defined Requirements
1. **Common Task Model:** Both baseline and proposed schedule identical task objects containing: `taskId`, `priority`, `arrivalTime`, `executionTime`, `deadline`, and `status`.
2. **Deterministic & Identical Workloads:** Baseline and Proposed schedulers are tested against the exact same VM configurations and task sets.
3. **Dedicated Fibonacci Heap:** Priority queue is a custom, isolated Fibonacci Heap data structure rather than relying on `java.util.PriorityQueue`.
4. **No Artificial Results:** Every number in `results/` is generated through reproducible CloudSim Plus simulation runs.
5. **Isolated Algorithms:** Baseline logic (`scheduler.baseline.*`) and Proposed logic (`scheduler.proposed.*`) remain strictly decoupled and frozen.
6. **VM Utilization Metric Definition:** Strictly bounded:
   $$\text{Utilization} = \frac{\sum_{j=1}^M \text{BusyTime}_j}{M \times \text{Makespan}} \times 100\% \le 100\%$$
7. **Descriptive Metric Comparison:** In all web comparisons and exports, difference is strictly computed as:
   $$\text{Difference} = \text{Proposed} - \text{Baseline}$$
   No "winner", "superior", or "ranking" terminology is introduced.
