# Proposed Deadline-Aware Priority-Based Task Scheduler

## 1. Motivation & Overview

In cloud computing environments, tasks frequently possess both differing intrinsic business importance (base priority) and explicit completion constraints (deadlines). While baseline priority approaches (such as *Lipsa et al., IEEE Access 2023*) optimize for sequential waiting time using eigenvector decomposition of a Waiting Matrix ($WM$), they are deadline-agnostic:
- A task with an imminent deadline cannot be prioritized over a task with a distant deadline if both have similar waiting delays.
- Tasks facing tight scheduling margins risk experiencing deadline misses even when computing resources could have accommodated them.
- Conversely, pure Earliest Deadline First (EDF) heuristics suffer from severe priority inversion under overload, ignoring task importance and causing high-priority jobs to fail alongside low-priority ones.

The proposed scheduling framework solves this challenge through a three-component architecture:
1. **Dynamic Deadline Urgency Algorithm** (`DeadlineUrgencyCalculator.java`)
2. **Dynamic Priority Algorithm** (`DeadlineAwarePriorityCalculator.java`)
3. **Persistent Fibonacci-Heap Scheduling Algorithm** (`ProposedPriorityScheduler.java`)

Dynamic Deadline-Aware Scheduling supports two execution modes:
- **NON-PREEMPTIVE:** A selected task runs until completion. Newly arrived tasks wait in the persistent Fibonacci heap until a virtual machine becomes available.
- **PREEMPTIVE:** A selected task may be interrupted when another task obtains a higher dynamic priority. The interrupted task preserves its remaining execution time, is reinserted into the persistent Fibonacci heap, and resumes later from its remaining execution time.

---

## 2. Distinction from the IEEE Baseline

| Dimension | IEEE Baseline (*Lipsa et al., 2023*) | Proposed Scheduler |
|---|---|---|
| **Deadline Awareness** | **None.** Deadline is purely an evaluation metadata field. | **Active.** Remaining slack time and deadline urgency dynamically guide scheduling. |
| **Urgency Calculation** | Not present. | Burst-aware dynamic deadline urgency $\Omega(t)$ using remaining slack and execution time. |
| **Priority Evolution** | Computed on admission and static during queueing. | **Dynamic.** Recomputed at every scheduling event as simulation time advances: $P(t)$. |
| **Starvation Mitigation** | Derived from sequential arrival ordering in $WM$. | Dynamic queue aging term $10 \times W(t)$. |
| **Execution Modes** | Strictly non-preemptive run-to-completion. | **Dual-Mode:** Supports both `NON_PREEMPTIVE` and `PREEMPTIVE` execution modes using the same core algorithms. |
| **Queue Structure** | Static sorted list / min-heap. | **Persistent Fibonacci Min-Heap** with in-place $O(1)$ amortized `decreaseKey()`. |

---

## 3. Representative Cloud Workload Types

In our experimental evaluation and demonstration benches, tasks are modeled using **representative cloud workload types** reflecting diverse real-world computational profiles rather than generic task IDs:

> [!NOTE]
> **Clarification of Terminology:**
> - **Representative Cloud Workload Types:** Standardized synthetic workload benchmarks parameterized with realistic computational attributes (Image Processing, Video Transcoding, Database Query, etc.) to ensure controlled, cycle-accurate, and 100% reproducible CloudSim Plus simulations.
> - **Real Cloud Execution:** The independent worker service deployed on Render/AWS (`aws/worker/`) that receives dispatched tasks and executes actual multithreaded SHA-256 cryptographic hashing to consume physical CPU cycles on live cloud hardware.

Where task profiles are presented, the standardized format is:  
**(Task_ID, Task Type, Deadline, Burst Time, Priority, Waiting Time)**

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

---

## 4. Proposed Algorithm Execution Flowchart

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
Start / Resume Task
    ↓
Check Scheduling Mode
    ↓
 ┌───────────────────────────────┐
 │                               │
NON-PREEMPTIVE              PREEMPTIVE
 │                               │
Run until complete          Monitor arrivals/events
 │                               │
 │                         Recalculate priorities
 │                               │
 │                         Higher priority arrives?
 │                              / \
 │                            Yes  No
 │                             │    │
 │                         Preempt  Continue
 │                             │
 │                         Save remaining time
 │                             │
 │                         Update heap
 │                             │
 └───────────────┬───────────────┘
                 ↓
        Advance simulation time
                 ↓
     Recalculate waiting tasks
                 ↓
            decreaseKey()
                 ↓
          Select next task
                 ↓
               Repeat
```

---

## 5. Mathematical Formulations

### Component 1: Dynamic Deadline Urgency Algorithm (Ω(t))
**File:** `scheduler/proposed/DeadlineUrgencyCalculator.java`

Calculates the time-dependent burst-aware deadline urgency:
$$D(t) = \text{deadline} - \text{currentTime}$$
$$B = \text{remainingExecutionTime}$$
$$\text{Slack}(t) = D(t) - B$$
$$\text{EffectiveSlack}(t) = \max(0.0, \, \text{Slack}(t))$$
$$D_{\text{effective}}(t) = \max(0.0, \, D(t))$$

$$\Omega(t) = \left[ \frac{1000}{\text{EffectiveSlack}(t) + 1} \right] \times \left[ 1 + \frac{B}{D_{\text{effective}}(t) + 1} \right]$$

- **Slack Term:** $\frac{1000}{\text{EffectiveSlack}(t) + 1}$ scales hyper-critically as remaining slack approaches zero.
- **Burst-Aware Multiplier:** $1 + \frac{B}{D_{\text{effective}}(t) + 1}$ scales urgency with remaining burst duration $B$, prioritizing computationally heavy tasks facing tight deadlines.
- **Numerical Safety Denominator Guard:** $D_{\text{effective}}(t) = \max(0.0, D(t))$ guarantees that $D_{\text{effective}}(t) + 1.0 \ge 1.0$ at all times, preventing division by zero or negative denominators when a task becomes overdue ($D(t) \le 0$).

### Component 2: Dynamic Priority Algorithm (P(t))
**File:** `scheduler/proposed/DeadlineAwarePriorityCalculator.java`

Synthesizes the immutable base priority, dynamic urgency, and queue waiting time:
$$W(t) = \max(0.0, \, \text{currentTime} - \text{arrivalTime})$$

$$P(t) = 100 \times \text{basePriority} + \Omega(t) + 10 \times W(t)$$

- **Immutable Base Priority:** $\text{basePriority} = \text{task.getPriority()} \in [1, 10]$ is preserved and never mutated.
- **Dynamic Urgency:** $\Omega(t)$ elevates urgent tasks as simulation time advances.
- **Anti-Starvation Aging:** $10 \times W(t)$ prevents long-waiting low-priority tasks from starving.

### Component 3: Persistent Fibonacci-Heap Scheduling Algorithm
**File:** `scheduler/proposed/ProposedPriorityScheduler.java`

- **Persistent Min-Heap:** Uses ONE persistent Fibonacci Min-Heap (`FibonacciHeap<HeapKey, Task>`) across the entire simulation run. The heap is **never discarded or rebuilt**.
- **Heap Key Formulation:**
  $$\text{HeapKey} = (-P(t), \, \text{deadline}, \, \text{taskId})$$
  Because the heap is a **Min-Heap**, minimizing $-P(t)$ selects the task with the **highest dynamic priority** $P(t)$ as the root.
- **In-Place Dynamic Updates via `decreaseKey()`:**
  As simulation time advances to a dispatch event, $P(t)$ is recalculated for active waiting tasks in `activeNodes`. When the updated key is smaller than the current key, the scheduler invokes `decreaseKey(node, updatedKey)` in **$O(1)$ amortized time**.
- **Execution Mode Branching:**
  - **NON_PREEMPTIVE Mode:** Selected task runs to completion. Total preemptions = 0.
  - **PREEMPTIVE Mode:** At meaningful discrete scheduling events (task arrivals, completions), running tasks' dynamic priorities are compared against waiting tasks. If a waiting task achieves strictly higher priority ($P_{\text{wait}} > P_{\text{run}}$), the running task is preempted. Its remaining burst is preserved ($B_{\text{rem}} = B_{\text{rem}} - \Delta B$), and it is reinserted into the persistent Fibonacci heap.

---

## 6. Task States & Preemption Lifecycle

Tasks transition across four discrete execution states:
- `WAITING`: Admitted into the Persistent Fibonacci Heap, waiting for an available VM.
- `RUNNING`: Actively executing on an assigned Virtual Machine.
- `PREEMPTED`: Interrupted prior to completion; remaining execution time is preserved in the persistent heap.
- `COMPLETED`: Fully executed all nominal computation time ($B_{\text{rem}} = 0.0$).

### Preservation of Remaining Execution Time
A preempted task is **never restarted from its original burst time**.  
*Example:* If a task with nominal burst 10 s runs for 3 s before preemption, its remaining burst is strictly 7 s. When resumed, it executes only the remaining 7 s (total accumulated CPU time = 10 s).

---

## 7. Performance Metrics Calculation

| Metric | Non-Preemptive Mode | Preemptive Mode | Formula |
|---|---|---|---|
| **Makespan** | $\max(C_i) - \min(A_i)$ | $\max(C_i) - \min(A_i)$ | Total schedule duration |
| **Waiting Time** | $S_i - A_i$ | $(C_i - A_i) - E_i$ | Elapsed time spent waiting in ready queue |
| **Turnaround Time** | $C_i - A_i$ | $C_i - A_i$ | Total residence time from arrival to completion |
| **Throughput** | $N / \text{Makespan}$ | $N / \text{Makespan}$ | Completed tasks per simulation second |
| **Deadline Miss Rate** | $N_{\text{missed}} / N$ | $N_{\text{missed}} / N$ | Fraction of tasks exceeding deadline |
| **VM Utilization** | $\frac{\sum \text{busyTime}}{M \times \text{Makespan}} \times 100$ | $\frac{\sum \text{busyTime}}{M \times \text{Makespan}} \times 100$ | Time-based VM resource utilization |
| **Preemption Count** | **0** | Actual interruptions | Number of times a running task was interrupted |

---

## 8. Computational Complexity

- **Priority Calculation:** $O(1)$ per task.
- **Dynamic Key Update:**
  - Scanning $n_{\text{waiting}}$ active waiting tasks: $O(n_{\text{waiting}})$ time.
  - In-place key decrease per node via `decreaseKey()`: $O(1)$ amortized time.
- **Root Extraction:** `extractMin()` takes $O(\log n)$ amortized time.
- **Overall Scheduling Loop:** The persistent heap is never rebuilt from scratch, providing optimal theoretical data structure efficiency in both non-preemptive and preemptive execution modes.
