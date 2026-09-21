# ARCHITECTURE: Deadline-Aware Priority-Based Task Scheduling in Cloud Computing

## 1. System Conceptual Architecture

The high-level data and control flow follows the specified pipeline:

```
[User / Task Workload Input]
            │
            ▼
     [Task Manager]
            │
            ▼
   [Priority Assignment] 
   (IEEE 2023 PAT Heuristic: Waiting Time Matrix & Normalized Priority)
            │
            ▼
   [Deadline-Aware Calculation] (Proposed Path Only)
   (Incorporates: Priority, Deadline, Arrival, Execution, Waiting, VM Availability)
            │
            ▼
[Fibonacci Heap Priority Queue]
   (Amortized O(1) insert/findMin, O(log N) extractMin)
            │
            ▼
        [Scheduler]
   (Baseline vs. Deadline-Aware)
            │
            ▼
   [VM Selection / Allocation]
   (Best-fit / Resource-aware matching)
            │
            ▼
[CloudSim Plus Engine Execution]
   (Datacenter, Hosts, VMs, Cloudlets)
            │
            ▼
       [Monitoring]
   (Execution events, clock, completions)
            │
            ▼
   [Performance Analyzer]
   (Makespan, Turnaround, Waiting, DMR, Bounded VM Utilization, Throughput)
            │
            ▼
  [Results / CSV / Graph Data]
```

---

## 2. Component Design & Directory Structure

```
deadline-aware-scheduler/
├── pom.xml
├── README.md
├── PROJECT_PLAN.md
├── ARCHITECTURE.md
├── DESIGN_DECISIONS.md
├── docs/
│   ├── architecture.md
│   ├── algorithm.md
│   ├── assumptions.md
│   └── experiments.md
├── data/
│   ├── tasks_20.csv
│   ├── tasks_50.csv
│   └── tasks_100.csv
├── src/
│   ├── main/
│   │   └── java/
│   │       └── scheduler/
│   │           ├── model/
│   │           │   ├── Task.java                        # Common task entity
│   │           │   ├── TaskStatus.java                  # SUBMITTED, READY, RUNNING, COMPLETED, MISSED_DEADLINE
│   │           │   └── CloudVmSpec.java                 # VM specifications (MIPS, RAM, BW, Cores)
│   │           ├── heap/
│   │           │   ├── FibonacciHeap.java               # Generic Fibonacci Heap
│   │           │   ├── FibonacciNode.java               # Heap Node (Key, Value, Degree, Mark, Pointers)
│   │           │   └── HeapKey.java                     # Comparable key supporting score + tie-breaking
│   │           ├── baseline/
│   │           │   ├── WaitingTimeControlMatrix.java    # Paper-derived Waiting Time Matrix (WTM)
│   │           │   ├── PriorityAssignmentToTasks.java   # Paper-derived PAT (Algorithm 1) priority vector
│   │           │   └── BaselineScheduler.java          # Baseline queue & VM dispatch logic
│   │           ├── deadline/
│   │           │   ├── DeadlinePriorityCalculator.java  # Proposed dynamic score accounting for all 6 factors
│   │           │   ├── DeadlineAwareScheduler.java      # Proposed scheduling logic with deadline checks
│   │           │   └── DeadlineState.java               # FEASIBLE, URGENT, CRITICAL, EXPIRED
│   │           ├── cloudsim/
│   │           │   ├── CloudSimEnvironment.java         # Datacenter, Host, VM, Broker setup
│   │           │   ├── TaskCloudletAdapter.java         # Bridges Task model with CloudSim Cloudlet
│   │           │   └── SimulationScenario.java          # Encapsulates datacenter & workload specs
│   │           ├── metrics/
│   │           │   ├── PerformanceMetrics.java          # Metric container (Makespan, DMR, etc.)
│   │           │   ├── MetricsCalculator.java           # Mathematical formulas (strictly bounded utilization)
│   │           │   └── SimulationReport.java            # Formatted reporting helper
│   │           ├── simulation/
│   │           │   ├── WorkloadGenerator.java           # Generates reproducible synthetic workloads
│   │           │   ├── ExperimentRunner.java            # Automated comparative benchmark runner
│   │           │   └── WorkloadScenarioType.java        # NORMAL, HIGH, DEADLINE_SENSITIVE, etc.
│   │           └── utils/
│   │               ├── CsvExporter.java                 # Raw data export to CSV
│   │               └── Constants.java                   # System constants & default weights
│   └── test/
│       └── java/
│           └── scheduler/
│               ├── heap/
│               │   └── FibonacciHeapTest.java           # Exhaustive heap operation tests
│               ├── model/
│               │   └── TaskModelTest.java               # Task state transitions and validation
│               ├── baseline/
│               │   └── BaselineSchedulerTest.java       # Baseline correctness tests
│               ├── deadline/
│               │   └── DeadlineAwareSchedulerTest.java  # Proposed scheduler unit tests
│               ├── metrics/
│               │   └── MetricsCalculationTest.java      # Mathematical correctness tests
│               └── integration/
│                   └── ComparativeSimulationTest.java   # End-to-end multi-task execution test
└── results/
    ├── baseline/
    ├── proposed/
    └── comparison/
```

---

## 3. Common Task Model Specification

Both schedulers consume the exact same Java class instances:

```java
public class Task {
    private final long taskId;
    private final int priority;           // Base priority (e.g. 1 - 5, where higher = more important)
    private final double arrivalTime;     // Simulation time when task arrives (seconds)
    private final double executionTime;   // Estimated / Required execution duration (seconds) or length in MI
    private final double deadline;        // Simulation time by which completion is expected (seconds)
    
    // Runtime execution tracking
    private TaskStatus status;            // PENDING, READY, RUNNING, COMPLETED, MISSED_DEADLINE
    private double startTime;            // Actual start time on allocated VM
    private double completionTime;       // Actual completion time on VM
    private int allocatedVmId;           // Assigned VM ID
    
    // Dynamic scheduling calculation fields
    private double dynamicPriority;      // Score calculated by baseline PAT or proposed deadline calculator
}
```

---

## 4. CloudSim Plus Integration Details
- **Engine**: CloudSim Plus `8.0.0`
- **Compiler/Target**: Java 21 LTS
- **Adapter**: `TaskCloudletAdapter` creates CloudSim `CloudletSimple` instances preserving mapping to `Task`.
- **Event Listeners**: On-finish listeners record clock times, compute wait/turnaround, determine deadline hit/miss, and record VM busy duration.
