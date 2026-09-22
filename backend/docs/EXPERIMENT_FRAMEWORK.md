# Controlled Experimental Framework

## 1. Overview and Core Experimental Principle

The **Controlled Experimental Framework** (Phase 8) provides an objective, reproducible evaluation harness to compare the **Baseline IEEE Priority Scheduler** (Phase 6) and the **Proposed Deadline-Aware Priority Scheduler** (Phase 7).

### The Controlled-Variable Principle
To guarantee scientific validity and eliminate confounding factors:
- **Identical Workload Stream:** Both schedulers receive the exact same task set:
  - Identical Task IDs
  - Identical Arrival timestamps ($t_a$)
  - Identical Base priorities ($p_i \in [1, 10]$)
  - Identical Nominal execution durations ($E_i$)
  - Identical Absolute deadlines ($D_i$)
- **Zero Mutable State Leakage:** Workloads are generated once using deterministic pseudorandom seeds, saved to CSV, and deep-cloned into fresh, unmutated `Task` instances prior to each scheduler run.
- **Identical Infrastructure:** Both schedulers execute against identical Virtual Machine topologies (e.g. 4 homogeneous VMs at 1000 MIPS) and reference MIPS scaling factors.
- **Algorithmic Freezing:** The mathematical formulas and scheduling mechanics of both Phase 6 and Phase 7 remain 100% frozen:
  - Baseline: IEEE PAT Algorithm 1, Power Iteration, Waiting Time Matrix, Fibonacci Heap extraction.
  - Proposed: Dynamic scoring ($0.35 P_{\text{norm}} + 0.50 U + 0.15 W_{\text{wait}}$), $k = 2.0$, $\tau = 50.0$, dynamic heap rebuilding at dispatch events.
- **Neutrality:** The framework measures and logs performance without computing rankings, scores, or drawing subjective conclusions.

---

## 2. Primary & Secondary Metrics

### Primary Metric: Time-based VM Resource Utilization
The primary system efficiency metric is explicitly designated as **"Time-based VM Resource Utilization"**:

$$\text{ResourceUtilization} = \frac{\sum_{j=1}^M \text{VM busy time}_j}{M \times \text{experiment makespan}} \times 100\%$$

Where:
- $\text{VM busy time}_j = \sum_{i \in \text{tasks on VM } j} (\text{completionTime}_i - \text{startTime}_i)$
- $\sum_{j=1}^M \text{VM busy time}_j = \sum_{i=1}^N (\text{completionTime}_i - \text{startTime}_i)$ (non-preemptive, non-overlapping task processing)
- $M$ = Number of Virtual Machines
- $\text{experiment makespan} = \max_{i}(\text{completionTime}_i) - \min_{i}(\text{arrivalTime}_i)$
- **Bounds:** Strictly bounded within $[0.0, 100.0]\%$.
- **Constraint:** MIPS-weighted utilization is *not* used.

### Additional Standard Metrics
1. **Makespan:**
   $$\text{Makespan} = \max_{i}(\text{completionTime}_i) - \min_{i}(\text{arrivalTime}_i)$$
2. **Average Waiting Time:**
   $$\text{Average Waiting Time} = \frac{1}{N} \sum_{i=1}^N (\text{startTime}_i - \text{arrivalTime}_i)$$
3. **Average Turnaround Time:**
   $$\text{Average Turnaround Time} = \frac{1}{N} \sum_{i=1}^N (\text{completionTime}_i - \text{arrivalTime}_i)$$
4. **Throughput:**
   $$\text{Throughput} = \frac{N_{\text{completed}}}{\text{Makespan}} \quad (\text{tasks/second})$$
5. **Deadline Miss Rate (DMR):**
   $$\text{DMR} = \frac{N_{\text{missed}}}{N_{\text{total}}} \quad (\text{ratio} \in [0.0, 1.0] \text{ and percentage})$$
   A task is observed as missed if and only if $\text{completionTime} > \text{deadline}$.

---

## 3. Workload Manifest & Traceability

To ensure full auditability, every generated workload is tracked in a central manifest:
- **Location:** `results/workload_manifest.csv`
- **Schema:**
  ```csv
  scenario,taskCount,seed,workloadFile
  ```
- **Example Record:**
  ```csv
  NORMAL_LOAD,20,1001,normal_load_20_seed1001.csv
  ```
This guarantees that any experimental result can be directly traced back to the exact synthetic workload file from which it originated.

---

## 4. Workload Scenarios & Configurations

| Scenario | Default Seed | Arrival Interval (s) | Execution Time (s) | Slack Multiplier | Purpose |
| :--- | :---: | :---: | :---: | :---: | :--- |
| `NORMAL_LOAD` | 1001 | $[0.5, 2.5]$ | $[5.0, 20.0]$ | $[1.5, 3.5]$ | Standard cloud operating conditions with comfortable deadline margins |
| `HIGH_LOAD` | 2001 | $[0.1, 0.8]$ | $[15.0, 40.0]$ | $[0.8, 2.0]$ | Stress condition with rapid arrivals, long tasks, and high VM queue contention |
| `DEADLINE_SENSITIVE` | 3001 | $[0.4, 2.0]$ | $[5.0, 30.0]$ | $[0.2, 1.2]$ | Stringent deadline pressure testing dynamic urgency prioritization |
| `MIXED` | 4001 | $[0.2, 3.0]$ | $[3.0, 45.0]$ | $[0.2, 4.0]$ | Heterogeneous workload combining quick/long tasks and tight/loose deadlines |

### Deadline Formulation
Workload generation assigns deadlines independently of scheduler scoring:
$$\text{deadline} = \text{arrivalTime} + \text{executionTime} + (\text{executionTime} \times \text{slackMultiplier})$$
Because $\text{slackMultiplier} \ge 0.2 > 0$ across all scenarios, initial slack at arrival time is always positive and feasible.

---

## 5. CSV Output Schemas

### 1. Workload Task File (`results/workloads/<scenario>_<count>_seed<seed>.csv`)
```csv
taskId,priority,arrivalTime,executionTime,deadline
0,5,0.00,15.65,47.31
1,4,1.99,10.56,30.90
```

### 2. Summary CSV (`results/smoke_summary.csv` or `results/experiment_summary.csv`)
```csv
scenario,taskCount,seed,schedulerName,vmCount,makespan,averageWaitingTime,averageTurnaroundTime,throughput,deadlineMissRate,deadlineMissedCount,completedTaskCount,resourceUtilization
NORMAL_LOAD,5,1001,BASELINE_IEEE_PRIORITY,4,20.2000,1.2260,12.5700,0.2475,0.0000,0,5,70.1980
NORMAL_LOAD,5,1001,PROPOSED_DEADLINE_AWARE,4,20.2000,1.2260,12.5700,0.2475,0.0000,0,5,70.1980
```

### 3. Detailed Per-Task CSV (`results/smoke_task_details.csv` or `results/experiment_task_details.csv`)
```csv
scenario,taskCount,seed,schedulerName,taskId,assignedVmId,arrivalTime,startTime,completionTime,executionTime,waitingTime,turnaroundTime,basePriority,dynamicScore,deadline,deadlineMissed
NORMAL_LOAD,5,1001,BASELINE_IEEE_PRIORITY,0,0,0.00,0.00,15.65,15.65,0.00,15.65,5,0.5000,47.31,false
NORMAL_LOAD,5,1001,PROPOSED_DEADLINE_AWARE,0,0,0.00,0.00,15.65,15.65,0.00,15.65,5,0.3568,47.31,false
```

---

## 6. Smoke Experiment Validation

The 5-task smoke experiment was executed using:
- **Scenario:** `NORMAL_LOAD`
- **Task Count:** 5
- **Seed:** 1001
- **VMs:** 4 homogeneous VMs (1000 MIPS each)

### Observed Output
- **Baseline Scheduler:**
  - Makespan: 20.2000 s
  - Average Waiting Time: 1.2260 s
  - Average Turnaround Time: 12.5700 s
  - Throughput: 0.2475 tasks/s
  - Time-based VM Resource Utilization: 70.1980%
  - Deadline Miss Rate: 0.0% (0 / 5)
- **Proposed Scheduler:**
  - Makespan: 20.2000 s
  - Average Waiting Time: 1.2260 s
  - Average Turnaround Time: 12.5700 s
  - Throughput: 0.2475 tasks/s
  - Time-based VM Resource Utilization: 70.1980%
  - Deadline Miss Rate: 0.0% (0 / 5)
- **Files Verified in `results/`:**
  - `results/workload_manifest.csv`
  - `results/workloads/normal_load_5_seed1001.csv`
  - `results/smoke_summary.csv`
  - `results/smoke_task_details.csv`
