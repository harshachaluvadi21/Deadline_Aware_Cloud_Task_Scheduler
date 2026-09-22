# Final Results and Artifact Manifest

This manifest documents all data files, analytical tables, publication charts, and reports generated during Phases 9, 10, and 11.

---

## 1. Workload Datasets (`results/workloads/`)

Contains the exact synthetic task sets generated deterministically and replayed identically to both the Baseline and Proposed schedulers:

| Filename | Scenario | Task Count | Seed | Purpose |
|---|---|:---:|:---:|---|
| `normal_load_5_seed1001.csv` | NORMAL_LOAD | 5 | 1001 | 5-task smoke validation workload |
| `normal_load_20_seed1001.csv` | NORMAL_LOAD | 20 | 1001 | Primary benchmark workload for Normal Load (20 tasks) |
| `normal_load_50_seed1001.csv` | NORMAL_LOAD | 50 | 1001 | Primary benchmark workload for Normal Load (50 tasks) |
| `normal_load_100_seed1001.csv` | NORMAL_LOAD | 100 | 1001 | Primary benchmark workload for Normal Load (100 tasks) |
| `high_load_20_seed2001.csv` | HIGH_LOAD | 20 | 2001 | Primary benchmark workload for High Load (20 tasks) |
| `high_load_50_seed2001.csv` | HIGH_LOAD | 50 | 2001 | Primary benchmark workload for High Load (50 tasks) |
| `high_load_100_seed2001.csv` | HIGH_LOAD | 100 | 2001 | Primary benchmark workload for High Load (100 tasks) |
| `deadline_sensitive_20_seed3001.csv` | DEADLINE_SENSITIVE | 20 | 3001 | Primary benchmark workload for Deadline-Sensitive (20 tasks) |
| `deadline_sensitive_50_seed3001.csv` | DEADLINE_SENSITIVE | 50 | 3001 | Primary benchmark workload for Deadline-Sensitive (50 tasks) |
| `deadline_sensitive_100_seed3001.csv` | DEADLINE_SENSITIVE | 100 | 3001 | Primary benchmark workload for Deadline-Sensitive (100 tasks) |
| `mixed_20_seed4001.csv` | MIXED | 20 | 4001 | Primary benchmark workload for Mixed Scenario (20 tasks) |
| `mixed_50_seed4001.csv` | MIXED | 50 | 4001 | Primary benchmark workload for Mixed Scenario (50 tasks) |
| `mixed_100_seed4001.csv` | MIXED | 100 | 4001 | Primary benchmark workload for Mixed Scenario (100 tasks) |

---

## 2. Manifests & Traceability (`results/manifests/` & root)

| Artifact | Schema / Content | Purpose |
|---|---|---|
| `results/manifests/final_experiment_manifest.csv` | `scenario,taskCount,seed,workloadFile,baselineResultFile,proposedResultFile,status` | Maps each of the 12 evaluation workloads to its specific baseline and proposed task-level output files |
| `results/workload_manifest.csv` | `scenario,taskCount,seed,workloadFile` | Central audit trail tracing all generated workloads back to scenario, task count, and seed |

---

## 3. Raw Simulation Outcomes (`results/experiments/`)

### Summary Results (`results/experiments/summary/`)
- `raw_experiment_results.csv`: Complete table of 24 scheduler runs (12 Baseline, 12 Proposed). Contains makespan, average waiting time, average turnaround time, throughput, deadline miss count, deadline miss rate, completed task count, total tasks, and Time-based VM Resource Utilization.

### Task-Level Execution Records (`results/experiments/task_details/`)
Contains per-task dispatch, start, completion, VM allocation, and dynamic score breakdowns:
- `all_tasks_experiment_details.csv`: Single master table containing all 2,040 task execution events across the 24 runs.
- 24 individual CSV files (e.g. `baseline_normal_load_20_seed1001.csv`, `proposed_normal_load_20_seed1001.csv`, etc.).

### Validation Logs (`results/experiments/validation/`)
- `workload_integrity_report.md`: Verifies that all 12 workloads passed all 8 parameter integrity checks, 100% pairwise input identity, numerical bounds checks, and reproducibility checks.

---

## 4. Analytical Datasets (`results/analysis/`)

| Artifact | Columns / Focus | Purpose |
|---|---|---|
| `aggregated_results.csv` | `scenario,taskCount,scheduler,meanMakespan,...` | Aggregated view where mean = observed value for single deterministic runs |
| `pairwise_comparison.csv` | Baseline vs Proposed side-by-side with $\Delta = \text{Proposed} - \text{Baseline}$ and relative % change | Exact mathematical differences without value judgments |
| `deadline_analysis.csv` | `totalTasks,deadlineMissedTasks,deadlineMetTasks,deadlineMissRate` | Focused deadline compliance comparison |
| `workload_analysis.csv` | Workload characteristics (`meanPriority`, `meanExecutionTime`, `meanDeadlineSlackAtArrival`) joined with metrics | Correlation analysis between workload attributes and performance |
| `final_metric_table.csv` | Presentation-ready summary table for reports and thesis | Formatted numeric values with standard decimal precision |
| `final_pairwise_table.csv` | Side-by-side presentation table for reports and slides | Direct side-by-side comparison across all metrics |
| `RESULTS_SUMMARY.md` | Comprehensive research findings, tables, differences, and limitations | Academic report document |

---

## 5. Publication Visualizations (`results/charts/`)

High-resolution 300 DPI PNG figures generated directly from the raw simulation data:

| Chart Filename | Metric Visualized | Format |
|---|---|:---:|
| `makespan_comparison.png` | Makespan (seconds) across 20, 50, 100 tasks for all 4 scenarios | 2x2 Bar Chart Grid |
| `waiting_time_comparison.png` | Average Waiting Time (seconds) across 20, 50, 100 tasks | 2x2 Bar Chart Grid |
| `turnaround_time_comparison.png` | Average Turnaround Time (seconds) across 20, 50, 100 tasks | 2x2 Bar Chart Grid |
| `throughput_comparison.png` | System Throughput (tasks/second) across 20, 50, 100 tasks | 2x2 Bar Chart Grid |
| `deadline_miss_rate_comparison.png` | Deadline Miss Rate (%) across 20, 50, 100 tasks | 2x2 Bar Chart Grid |
| `resource_utilization_comparison.png` | Time-based VM Resource Utilization (%) across 20, 50, 100 tasks | 2x2 Bar Chart Grid |
