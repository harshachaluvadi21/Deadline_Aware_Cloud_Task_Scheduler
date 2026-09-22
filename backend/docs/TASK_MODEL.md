# Common Task Data Model Specification

## 1. Architectural Role & Shared Model Principle

In this research project, **both** the baseline algorithm (IEEE Access 2023 PAT + WTM) and the proposed deadline-aware algorithm consume and schedule the **exact same** Java class instances: `scheduler.model.Task`.

### Why the Same Task Model is Mandatory for Both Algorithms:
1. **Scientific Equivalence & Controlled Comparison:** In experimental computer science, comparing two scheduling heuristics requires strict input invariance. If baseline tasks and proposed tasks were represented by differing data structures, subtle differences in memory layouts, field representations, or initialization semantics could introduce experimental bias or confounding variables.
2. **Fairness in Evaluation:** Both schedulers receive identical task instances (same IDs, arrival times, execution requirements, and deadlines). The baseline simply evaluates its priority vector without reading the deadline field, while the proposed scheduler actively incorporates deadline slack into its heuristic.
3. **Decoupled Metric Analysis:** Downstream metric calculators (Makespan, Waiting Time, Turnaround Time, DMR) can evaluate completed task lists uniformly without needing adapter layers or algorithm-specific conditionals.

---

## 2. Task Fields, Data Types, and Units

The `Task` class separates immutable task-definition properties from mutable runtime state:

### 2.1 Initial Task-Definition Properties (Immutable)

| Field Name | Java Type | Unit | Modifiers | Description |
|---|---|---|---|---|
| `taskId` | `long` | Dimensionless ID | `private final` | Unique, non-negative task identifier ($taskId \ge 0$). |
| `priority` | `int` | Discrete rank | `private final` | Base priority level within $[1, 10]$ (higher value = higher priority). |
| `arrivalTime` | `double` | Simulation seconds ($s$) | `private final` | Absolute simulation timestamp when task enters system ($t_{arr} \ge 0.0$). |
| `executionTime` | `double` | Simulation seconds ($s$) | `private final` | Nominal execution duration requirement on reference resource ($t_{exec} > 0.0$). |
| `deadline` | `double` | Simulation seconds ($s$) | `private final` | Absolute simulation timestamp by which execution must complete ($D \ge t_{arr}$). |

### 2.2 Runtime State Properties (Mutable via Lifecycle Methods)

| Field Name | Java Type | Unit | Default Value | Description |
|---|---|---|---|---|
| `status` | `TaskStatus` | Enum | `SUBMITTED` | Current execution lifecycle phase. |
| `startTime` | `double` | Simulation seconds ($s$) | `-1.0` | Absolute timestamp when task started execution on VM. |
| `completionTime` | `double` | Simulation seconds ($s$) | `-1.0` | Absolute timestamp when VM completed execution. |
| `allocatedVmId` | `long` | Dimensionless VM ID | `-1` | Identifier of the Virtual Machine assigned to run the task. |
| `dynamicPriority`| `double` | Normalized score | `0.0` | Heuristic score calculated by PAT or Deadline-Aware formula. |

---

## 3. Strict Validation Rules

All task instances validate constraints at construction time and reject illegal state changes:

1. **Non-Negative Task ID:** `taskId >= 0`. Negative IDs throw `IllegalArgumentException`.
2. **Bounded Priority:** `priority >= MIN_PRIORITY (1) && priority <= MAX_PRIORITY (10)`. Values outside this range throw `IllegalArgumentException`.
3. **Finite & Non-Negative Arrival Time:** `arrivalTime >= 0.0` and `Double.isFinite(arrivalTime)`. Negative or NaN values throw `IllegalArgumentException`.
4. **Finite & Strictly Positive Execution Time:** `executionTime > 0.0` and `Double.isFinite(executionTime)`. Zero, negative, or NaN durations throw `IllegalArgumentException`.
5. **Causal Deadline Constraint:** `deadline >= arrivalTime` and `Double.isFinite(deadline)`. A deadline earlier than arrival time violates causality and throws `IllegalArgumentException`.
6. **Causal Start Time:** `startTime >= arrivalTime`. Attempting `markRunning` with `startTime < arrivalTime` throws `IllegalArgumentException`.
7. **Causal Completion Time:** `completionTime >= startTime`. Attempting completion with `completionTime < startTime` throws `IllegalArgumentException`.
8. **Valid VM ID:** `allocatedVmId >= 0` upon assignment.

---

## 4. Lifecycle State Machine & Transition Rules

Task execution transitions strictly through the validated lifecycle defined in `scheduler.model.TaskStatus`:

```
   [SUBMITTED]
        │  (markReady())
        ▼
     [READY]
        │  (markRunning(startTime, vmId))
        ▼
    [RUNNING]
     ├───► [COMPLETED]       (markCompleted(completionTime) where completionTime <= deadline)
     └───► [MISSED_DEADLINE] (markDeadlineMissed(completionTime) where completionTime > deadline)
```

### Transition Enforcement:
- **`markReady()`**: Permitted ONLY from `SUBMITTED`.
- **`markRunning(startTime, vmId)`**: Permitted ONLY from `READY`.
- **`markCompleted(completionTime)`**: Permitted ONLY from `RUNNING`. Strictly requires `completionTime <= deadline`.
- **`markDeadlineMissed(completionTime)`**: Permitted ONLY from `RUNNING`. Strictly requires `completionTime > deadline`.
- **`markFinished(completionTime)`**: Convenience method that automatically evaluates whether `completionTime <= deadline` (calling `markCompleted`) or `completionTime > deadline` (calling `markDeadlineMissed`).
- **Terminal States:** `COMPLETED` and `MISSED_DEADLINE` are terminal (`isTerminal() == true`). Any subsequent transition attempt throws `IllegalStateException`.
- **Arbitrary Jumps Prohibited:** Direct transitions such as `SUBMITTED -> RUNNING` or `READY -> COMPLETED` immediately throw `IllegalStateException`.

---

## 5. Deadline Semantics

- **Absolute Simulation Timestamp:** `deadline` is an absolute point on the simulation timeline, not a relative delay.
  * *Example:* If $arrivalTime = 10.0\text{s}$ and $deadline = 30.0\text{s}$, the allowed response window is $20.0\text{s}$.
- **Boundary Condition (Exact Deadline):** Completion precisely at the deadline is **NOT** a miss:
  $$\text{Deadline Miss} \iff \text{completionTime} > \text{deadline}$$
  $$\text{completionTime} = \text{deadline} \implies \text{Status} = \text{COMPLETED}$$
- **Missed Deadline Accounting:** In alignment with Project Design Decision 4, tasks that miss their deadline are allowed to run to completion so that total workload makespan is preserved, but their status is permanently marked as `MISSED_DEADLINE` and counted in the Deadline Miss Rate (DMR).

---

## 6. Complete CloudSim Framework Decoupling

`Task.java` and `TaskStatus.java` contain **zero** dependencies on CloudSim Plus:
- No imports from `org.cloudsimplus.*`.
- Pure standard Java standard library (`java.util.Objects`).
- **Separation of Concepts:**
  * `Task.executionTime` represents nominal task duration in seconds.
  * In Phase 7 / integration, `TaskCloudletAdapter` translates `Task` to CloudSim `CloudletSimple` where:
    $$\text{Cloudlet Length (MI)} = \text{Task.executionTime (s)} \times \text{Reference MIPS}$$
- This guarantees that the scheduling core and algorithm logic remain fully unit-testable without spinning up CloudSim discrete-event simulation engines.
