# CloudSim Plus 8.0.0 Experimental Validation & Execution Report

**Date:** October 6, 2026  
**Project:** Deadline-Aware Priority-Based Task Scheduling in Cloud Computing  
**Simulation Engine:** CloudSim Plus 8.0.0 (Discrete-Event Cloud Simulation)  
**Status:** Architecture Validated, 100% Tests Passing, Benchmark Rerun  

---

## 1. Executive Summary & Root Cause Analysis

### A. Root Cause
During the architectural review, it was identified that although CloudSim Plus 8.0.0 was included as a project dependency in `pom.xml` and validated via standalone unit tests, the main research experimental benchmark pipeline was bypassing CloudSim Plus:

1. **Analytic Timing Bypass**:
   `ExperimentRunner` and `CampaignRunner` invoked the scheduling algorithms (`BaselinePriorityScheduler` and `ProposedPriorityScheduler`), which maintained internal arrays (`vmAvailableTime[vmId]`) to predict task start and finish times.
2. **Metrics Harvested from Scheduler Estimates**:
   Instead of converting tasks to Cloudlets and running discrete-event simulation, `ExperimentRunner` constructed `TaskExecutionRecord` objects directly from the schedulers' internal mathematical predictions and fed them into `MetricsCalculator`.
3. **Web API Disconnect**:
   `SimulationService.java` contained the identical analytic shortcut.

As a result, CloudSim Plus was not serving as the execution engine for the 24 research experiments.

---

## 2. Architecture & Call Graph Modifications

### B. List of Modified and Created Files

- **`src/main/java/scheduler/cloudsim/TaskCloudletAdapter.java`** *(Created)*
  Converts domain `Task` models to CloudSim `CloudletSimple` entities. Maps execution requirements using reference MIPS ($\text{length MI} = \text{executionTime} \times \text{referenceMIPS}$) and translates arrival times directly into CloudSim submission delays ($\text{delay} = \text{arrivalTime}$). Extracts real execution timestamps back into `TaskExecutionRecord`.
- **`src/main/java/scheduler/cloudsim/CloudSimEnvironment.java`** *(Modified)*
  Equipped with `CloudletSchedulerSpaceShared` on every VM to enforce true non-preemptive queuing. Added `execute(tasks, assignments, refMips)` which constructs the infrastructure, binds Cloudlets to scheduler-assigned VMs via `broker.bindCloudletToVm(cloudlet, vm)`, executes `simulation.start()`, and harvests execution results.
- **`src/main/java/scheduler/cloudsim/SimulationScenario.java`** *(Modified)*
  Added the `forVmSpecs(List<VmSpecification> vmSpecs)` factory to provision matching physical hosts and datacenters for any experimental VM configuration.
- **`src/main/java/scheduler/model/Task.java`** *(Modified)*
  Added `resetRuntimeState()` to ensure tasks cleanly transition from scheduler ranking to discrete-event execution.
- **`src/main/java/scheduler/experiment/ExperimentRunner.java`** *(Modified)*
  Refactored `runOnWorkload(...)` to enforce a strict boundary: scheduler algorithms only perform prioritization and VM assignment, while discrete-event execution is delegated to `CloudSimEnvironment`. Benchmark metrics are calculated exclusively from harvested CloudSim execution records. Added `[EXPERIMENT]` tracing logs.
- **`src/main/java/scheduler/experiment/CampaignRunner.java`** *(Modified)*
  Updated the 24-experiment benchmark suite to execute strictly via CloudSim Plus and export output files to `results/cloudsim/` as well as `results/`.
- **`src/main/java/web/service/SimulationService.java`** *(Modified)*
  Wired the production REST API simulation endpoint directly to `CloudSimEnvironment.execute(...)`.
- **New Tests & Runners**:
  - `src/test/java/scheduler/cloudsim/CloudletBindingTest.java`
  - `src/test/java/scheduler/cloudsim/CloudSimEndToEndTest.java`
  - `src/test/java/scheduler/experiment/ExperimentRunnerCloudSimIntegrationTest.java`
  - `src/test/java/scheduler/experiment/SmallEndToEndValidationTest.java`
  - `src/test/java/scheduler/experiment/FullBenchmarkExecutionTest.java`
  - `src/main/java/scheduler/experiment/MultiSeedExperimentRunner.java`

---

### C. Production Call Graph

```
Workload Generator / Workload Matrix
                 │
                 ▼
          ExperimentRunner
                 │
    ┌────────────┴────────────┐
    ▼                         ▼
Baseline Scheduler     Proposed Scheduler
(PAT + Fibonacci Heap) (PAT + Deadline Urgency + Heap)
    │                         │
    └────────────┬────────────┘
                 ▼
       Target VM Assignments
                 │
                 ▼
        TaskCloudletAdapter
        (Task → CloudletSimple)
                 │
                 ▼
        CloudSimEnvironment
        (1 Host per VM, SpaceShared)
                 │
                 ▼
      broker.bindCloudletToVm()
                 │
                 ▼
        simulation.start()
     [CloudSim Plus 8.0.0 Engine]
                 │
                 ▼
  Harvest Real Execution Records
  (getExecStartTime, getFinishTime, getVm)
                 │
                 ▼
         MetricsCalculator
 (Makespan = max(finish) - min(arrival),
  Miss Rate, Turnaround, Waiting, Throughput)
                 │
                 ▼
   results/cloudsim/ CSV Outputs
```

---

## 3. Small End-to-End Validation Experiment

Prior to running the 24-benchmark suite, a validation experiment was conducted using 4 tasks and 2 VMs (VM 0: 1000 MIPS, VM 1: 1000 MIPS):

### Baseline (IEEE PAT) Execution
| Task | Scheduler VM | CloudSim VM | Arrival | Start | Finish | Deadline | Miss? |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **T1** | VM 0 | VM 0 | 0.00 | 0.10 | 3.10 | 8.00 | false |
| **T2** | VM 1 | VM 1 | 1.00 | 1.10 | 5.21 | 12.00 | false |
| **T3** | VM 0 | VM 0 | 2.00 | 3.21 | 5.32 | 6.00 | false |
| **T4** | VM 0 | VM 0 | 3.00 | 5.43 | 10.54 | 10.00 | true |

### Proposed (Deadline-Aware) Execution
| Task | Scheduler VM | CloudSim VM | Arrival | Start | Finish | Deadline | Miss? |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **T1** | VM 0 | VM 0 | 0.00 | 0.10 | 3.10 | 8.00 | false |
| **T2** | VM 1 | VM 1 | 1.00 | 1.10 | 5.21 | 12.00 | false |
| **T3** | VM 0 | VM 0 | 2.00 | 3.21 | 5.32 | 6.00 | false |
| **T4** | VM 0 | VM 0 | 3.00 | 5.43 | 10.54 | 10.00 | true |

**Result**: 100% agreement between scheduler-selected VM and CloudSim execution VM. Timestamps satisfy `Finish >= Start >= Arrival` and `Miss == (Finish > Deadline)`.

---

## 4. Test Suite Execution Summary

Execution command: `mvn test`
```text
[INFO] Results:
[INFO] 
[INFO] Tests run: 143, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

- **Total Tests Run:** 143
- **Passed:** 143
- **Failed:** 0
- **Errors:** 0
- **Skipped:** 0

Targeted suites verified:
- `CloudSimCompatibilityTest`: PASSED
- `CloudletBindingTest`: PASSED (ensures Cloudlet VM binding matches scheduler VM target)
- `CloudSimEndToEndTest`: PASSED
- `ExperimentRunnerCloudSimIntegrationTest`: PASSED (verifies production path runs CloudSim)
- `SmallEndToEndValidationTest`: PASSED
- `FullBenchmarkExecutionTest`: PASSED

---

## 5. Final Research Benchmark Campaign (24 Experiments)

All 24 experimental configurations (4 workload profiles $\times$ 3 scales $\{20, 50, 100\}$ tasks $\times$ 2 schedulers) were rerun through the CloudSim Plus engine and persisted to `results/cloudsim/experiments/summary/cloudsim_benchmark_results.csv`.

### Verified Comparative Results Table

| Workload Profile | Scale | Metric | Baseline (PAT) | Proposed (Deadline-Aware) | Raw Difference / Percentage |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **NORMAL_LOAD** | 20 | Makespan (s) | 72.19 | 68.73 | **+4.79% faster makespan** |
| | | Deadline Miss Rate | 15.0% (0.1500) | 20.0% (0.2000) | +5.0% higher miss rate |
| | | Deadline Success Rate | 85.0% (0.8500) | 80.0% (0.8000) | -5.0% lower success rate |
| | | Avg Turnaround (s) | 25.62 | 25.89 | -1.02% turnaround time |
| | 50 | Makespan (s) | 164.91 | 164.98 | -0.04% makespan |
| | | Deadline Miss Rate | 54.0% (0.5400) | 58.0% (0.5800) | +4.0% higher miss rate |
| | | Deadline Success Rate | 46.0% (0.4600) | 42.0% (0.4200) | -4.0% lower success rate |
| | | Avg Turnaround (s) | 50.13 | 50.26 | -0.27% turnaround time |
| | 100 | Makespan (s) | 319.95 | 319.08 | **+0.27% faster makespan** |
| | | Deadline Miss Rate | 77.0% (0.7700) | 80.0% (0.8000) | +3.0% higher miss rate |
| | | Deadline Success Rate | 23.0% (0.2300) | 20.0% (0.2000) | -3.0% lower success rate |
| | | Avg Turnaround (s) | 93.30 | 93.55 | -0.27% turnaround time |
| **HIGH_LOAD** | 20 | Makespan (s) | 152.80 | 153.83 | -0.67% makespan |
| | | Deadline Miss Rate | 60.0% (0.6000) | 65.0% (0.6500) | +5.0% higher miss rate |
| | | Deadline Success Rate | 40.0% (0.4000) | 35.0% (0.3500) | -5.0% lower success rate |
| | | Avg Turnaround (s) | 82.27 | 82.22 | **+0.06% faster turnaround** |
| | 50 | Makespan (s) | 363.55 | 362.75 | **+0.22% faster makespan** |
| | | Deadline Miss Rate | 82.0% (0.8200) | 86.0% (0.8600) | +4.0% higher miss rate |
| | | Deadline Success Rate | 18.0% (0.1800) | 14.0% (0.1400) | -4.0% lower success rate |
| | | Avg Turnaround (s) | 184.63 | 186.33 | -0.92% turnaround time |
| | 100 | Makespan (s) | 703.13 | 697.19 | **+0.84% faster makespan** |
| | | Deadline Miss Rate | 93.0% (0.9300) | 93.0% (0.9300) | **0.0% (Identical miss rate)** |
| | | Deadline Success Rate | 7.0% (0.0700) | 7.0% (0.0700) | **0.0% (Identical success rate)** |
| | | Avg Turnaround (s) | 340.47 | 339.79 | **+0.20% faster turnaround** |
| **DEADLINE_SENSITIVE** | 20 | Makespan (s) | 100.54 | 98.47 | **+2.06% faster makespan** |
| | | Deadline Miss Rate | 65.0% (0.6500) | 75.0% (0.7500) | +10.0% higher miss rate |
| | | Deadline Success Rate | 35.0% (0.3500) | 25.0% (0.2500) | -10.0% lower success rate |
| | | Avg Turnaround (s) | 45.64 | 44.95 | **+1.52% faster turnaround** |
| | 50 | Makespan (s) | 228.77 | 233.86 | -2.22% makespan |
| | | Deadline Miss Rate | 86.0% (0.8600) | 86.0% (0.8600) | **0.0% (Identical miss rate)** |
| | | Deadline Success Rate | 14.0% (0.1400) | 14.0% (0.1400) | **0.0% (Identical success rate)** |
| | | Avg Turnaround (s) | 96.84 | 96.57 | **+0.28% faster turnaround** |
| | 100 | Makespan (s) | 440.65 | 438.20 | **+0.56% faster makespan** |
| | | Deadline Miss Rate | 95.0% (0.9500) | 94.0% (0.9400) | **-1.0% lower miss rate (Better)** |
| | | Deadline Success Rate | 5.0% (0.0500) | 6.0% (0.0600) | **+1.0% higher success rate** |
| | | Avg Turnaround (s) | 173.54 | 174.66 | -0.65% turnaround time |
| **MIXED** | 20 | Makespan (s) | 129.81 | 147.94 | -13.97% makespan |
| | | Deadline Miss Rate | 40.0% (0.4000) | 35.0% (0.3500) | **-5.0% lower miss rate (Better)** |
| | | Deadline Success Rate | 60.0% (0.6000) | 65.0% (0.6500) | **+5.0% higher success rate** |
| | | Avg Turnaround (s) | 62.48 | 59.81 | **+4.27% faster turnaround** |
| | 50 | Makespan (s) | 331.36 | 336.41 | -1.52% makespan |
| | | Deadline Miss Rate | 76.0% (0.7600) | 72.0% (0.7200) | **-4.0% lower miss rate (Better)** |
| | | Deadline Success Rate | 24.0% (0.2400) | 28.0% (0.2800) | **+4.0% higher success rate** |
| | | Avg Turnaround (s) | 136.32 | 134.93 | **+1.02% faster turnaround** |
| | 100 | Makespan (s) | 593.54 | 596.07 | -0.43% makespan |
| | | Deadline Miss Rate | 89.0% (0.8900) | 86.0% (0.8600) | **-3.0% lower miss rate (Better)** |
| | | Deadline Success Rate | 11.0% (0.1100) | 14.0% (0.1400) | **+3.0% higher success rate** |
| | | Avg Turnaround (s) | 242.13 | 236.13 | **+2.48% faster turnaround** |

*Energy consumption note: In accordance with research protocol requirements, energy consumption is not evaluated synthetically and remains "Not evaluated".*

---

## 6. Multi-Seed Statistical Evaluation (`DEADLINE_SENSITIVE`)

Across 5 controlled random seeds (101, 202, 303, 404, 505), empirical statistics are reported as **$\text{mean} \pm \text{standard deviation}$**:

| Metric | Task Scale | Baseline (PAT) ($\text{mean} \pm \text{std}$) | Proposed (Deadline-Aware) ($\text{mean} \pm \text{std}$) |
| :--- | :--- | :--- | :--- |
| **Makespan (s)** | 20 | $105.3780 \pm 7.4786$ | $108.0140 \pm 5.6186$ |
| | 50 | $237.0060 \pm 12.0455$ | $237.7500 \pm 14.2786$ |
| | 100 | $468.5040 \pm 19.2560$ | $465.0520 \pm 17.2276$ |
| **Deadline Miss Rate** | 20 | $0.6900 \pm 0.0822$ | $0.6600 \pm 0.0894$ |
| | 50 | $0.8600 \pm 0.0316$ | $0.8640 \pm 0.0456$ |
| | 100 | $0.9240 \pm 0.0207$ | $0.9180 \pm 0.0239$ |
| **Average Turnaround (s)** | 20 | $48.5967 \pm 5.8151$ | $48.8047 \pm 5.3545$ |
| | 50 | $98.5924 \pm 7.4113$ | $99.6019 \pm 7.1511$ |
| | 100 | $182.6689 \pm 11.3588$ | $180.3058 \pm 10.6716$ |
| **Resource Utilization (%)** | 20 | $90.3763 \pm 2.0166$ | $88.1291 \pm 2.8117$ |
| | 50 | $95.0205 \pm 1.5970$ | $94.7768 \pm 2.2085$ |
| | 100 | $96.3409 \pm 0.4950$ | $97.0392 \pm 1.0827$ |

---

## 7. Direct Assessment

> **Are the final research benchmark results now generated by CloudSim Plus?**

**YES.**  
Every benchmark experiment executes through CloudSim Plus 8.0.0. The scheduler algorithms determine task ordering and VM assignments; CloudSim Plus provisions datacenters, hosts, and VMs, manages simulation event queues, calculates start/finish timestamps, and returns execution records that directly feed `MetricsCalculator`. No predicted timestamps are used for benchmark metrics.
