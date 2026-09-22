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

## 1.1 Architectural Layering & Separation of Concerns

The project strictly segregates into two decoupled layers:

1. **Research Experiment Layer (Frozen, Phases 0–11)**:
   - Contains the core scheduling heuristics: `BaselinePriorityScheduler` and `ProposedPriorityScheduler`.
   - Contains the controlled experimental framework, 24-run campaign, raw CSV results, and publication charts under `results/`.
   - All logic, formulas, seeds, weights, and raw datasets in this layer are permanently frozen.

2. **Interactive Web Application Layer (Phases 12–15)**:
   - Built on Spring Boot 3.3.4 (REST API) and React 18 + Vite + TypeScript (Dashboard).
   - Translates user-provided synthetic tasks into domain `Task` models and invokes the scheduling engine in memory.
   - Provides CSV upload/validation, result export, dynamic charts, deadline analysis, and VM Gantt visualizations.
   - Does not modify or overwrite research experiment files in `results/`.

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

## 3. Common Task Model Specification (Phase 4 Verified)

Both schedulers consume the exact same Java class instances: `scheduler.model.Task`.

```java
public class Task {
    // Immutable Definition Properties
    private final long taskId;            // Unique identifier (taskId >= 0)
    private final int priority;           // Base priority in [1, 10] (higher = more important)
    private final double arrivalTime;     // Simulation time when task arrives (seconds)
    private final double executionTime;   // Nominal execution duration requirement (seconds)
    private final double deadline;        // Absolute simulation timestamp for completion (seconds)
    
    // Mutable Runtime State Properties (Transitioned via safe lifecycle methods)
    private TaskStatus status;            // SUBMITTED -> READY -> RUNNING -> COMPLETED / MISSED_DEADLINE
    private double startTime;            // Actual start time on allocated VM (>= arrivalTime)
    private double completionTime;       // Actual completion time on VM (>= startTime)
    private long allocatedVmId;           // Assigned VM ID (>= 0)
    private double dynamicPriority;      // Heuristic score calculated by PAT or Deadline Calculator
}
```

- **Lifecycle Enum**: `scheduler.model.TaskStatus` (`SUBMITTED`, `READY`, `RUNNING`, `COMPLETED`, `MISSED_DEADLINE`).
- **Deadline Semantics**: A deadline miss occurs strictly when `completionTime > deadline`. Completion at `completionTime <= deadline` transitions to `COMPLETED`.
- **Decoupling**: `Task.java` contains zero dependencies on `org.cloudsimplus.*`.
- Full specification documented in `docs/TASK_MODEL.md`.

---

## 4. CloudSim Plus Integration Details
- **Engine**: CloudSim Plus `8.0.0`
- **Compiler/Target**: Java 21 LTS (`maven.compiler.release=21`)
- **Infrastructure Layer** (Phase 3 Verified):
  - `CloudSimEnvironment`: Encapsulates `CloudSimPlus`, `DatacenterSimple`, `HostSimple`, `DatacenterBrokerSimple`, and `VmSimple` provisioning.
  - `SimulationScenario`: Centralized hardware scenario configuration (default: 2 physical hosts @ 12,000 MIPS each, 5 heterogeneous VMs: 500 - 2500 MIPS).
  - `CloudVmSpec`: Immutable resource specification record validating positive MIPS, RAM, BW, and Storage.
  - Full details documented in `docs/SIMULATION_ENVIRONMENT.md`.
- **Adapter** (Planned Phase 7): `TaskCloudletAdapter` bridges the common `Task` model with CloudSim `CloudletSimple` instances.
- **Event Listeners**: On-finish listeners record clock times, compute wait/turnaround, determine deadline hit/miss, and record VM busy duration.

---

## 5. Standalone Fibonacci Heap (Phase 5 Verified)
- **Generic Data Structure**: `scheduler.heap.FibonacciHeap<K, V>` and `scheduler.heap.FibonacciNode<K, V>`.
- **Ordering Convention**: Min-oriented heap; supports custom `Comparator` (e.g. `Comparator.reverseOrder()` for max-priority scheduling).
- **Composite Deterministic Ordering**: `scheduler.heap.HeapKey` provides composite primary score, secondary tie-breaking, and sequence ID.
- **Asymptotic Bounds**: Insert $\mathcal{O}(1)$, Extract-Min $\mathcal{O}(\log n)$, Decrease-Key $\mathcal{O}(1)$, Union $\mathcal{O}(1)$.
- **Decoupling**: Zero dependencies on CloudSim Plus, `Task`, or scheduling algorithms. Verified by 18 exhaustive tests including differential testing against a `PriorityQueue` oracle.
- Full details documented in `docs/FIBONACCI_HEAP.md`.
