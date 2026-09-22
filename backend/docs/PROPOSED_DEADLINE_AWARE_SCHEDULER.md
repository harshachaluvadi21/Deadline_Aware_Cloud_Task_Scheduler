# Proposed Deadline-Aware Priority-Based Task Scheduler

## 1. Motivation

In cloud computing environments, tasks frequently possess both differing intrinsic business importance (base priority) and explicit completion constraints (deadlines). While the baseline IEEE Access 2023 priority scheduling approach (*Lipsa et al.*) effectively optimizes for sequential waiting time using eigenvector decomposition of a Waiting Matrix ($WM$), it is entirely deadline-agnostic:
- A task with an imminent deadline cannot be prioritized over a task with a distant deadline if both have similar waiting delays.
- Tasks facing tight scheduling margins risk experiencing deadline misses even when computing resources could have accommodated them.
- Conversely, pure Earliest Deadline First (EDF) heuristics suffer from severe priority inversion under overload, ignoring task importance and causing high-priority jobs to fail alongside low-priority ones.

**[Project Design Decision]**  
The proposed scheduler extends heuristic task scheduling by synthesizing base task importance with dynamic deadline urgency and elapsed waiting time into a unified, balanced scheduling metric.

---

## 2. Distinction from the IEEE Baseline

| Dimension | IEEE Baseline (*Lipsa et al., 2023*) | Proposed Scheduler |
|---|---|---|
| **Deadline Awareness** | **None.** Deadline is purely an evaluation metadata field. | **Active.** Remaining slack time and deadline urgency dynamically guide scheduling. |
| **Urgency Calculation** | Not present. | Continuous exponential response function based on remaining slack. |
| **Priority Evolution** | Computed on admission and static during queueing. | **Dynamic.** Recomputed at every scheduling decision event as simulation time advances. |
| **Starvation Mitigation** | Derived from sequential arrival ordering in $WM$. | Dynamic bounded saturation function based on actual elapsed waiting time. |
| **Execution-Time Role** | Used in sequential cumulative waiting formula: $WT(T_k) = \sum PT(T_j)$. | Used directly in remaining slack buffer: $\text{slack}(t) = \text{deadline} - t - \text{executionTime}$. |

> [!NOTE]
> All enhancements in this proposed scheduler are **[Project Design Decisions]** and are **NOT** claimed to originate from the IEEE Access 2023 reference paper.

---

## 3. Input Factors

The proposed dynamic score evaluates six mandatory parameters:
1. **Task Base Priority ($p_i \in [1, 10]$):** Initial application-defined importance.
2. **Task Deadline ($D_i$):** Absolute simulation clock deadline (seconds).
3. **Task Arrival Time ($A_i$):** Absolute simulation clock arrival timestamp (seconds).
4. **Task Execution Time ($E_i$):** Nominal processing duration requirement (seconds).
5. **Current Simulation Time ($t$):** Instantaneous clock timestamp when a scheduling decision is evaluated.
6. **VM Availability & Capacity:** Modeled through identical earliest-available VM selection and reference MIPS duration scaling.

---

## 4. Mathematical Formulation & Normalization

All factors are normalized into the continuous range $[0.0, 1.0]$ so that no single factor dominates arbitrarily.

### A. Base Priority Normalization ($P_{\text{norm}} \in [0.1, 1.0]$)
**[Project Design Decision]**  
$$P_{\text{norm}} = \frac{\text{task.getPriority()}}{\text{maxPriority}} = \frac{p}{10.0}$$
For default bounds $[1, 10]$, priority 1 normalizes to $0.1$ and priority 10 normalizes to $1.0$.

### B. Deadline Urgency ($U_{\text{deadline}}(t) \in [0.0, 1.0]$)
**[Project Design Decision]**  
First, the remaining execution buffer (slack) is evaluated at instantaneous simulation time $t$:
$$\text{slack}(t) = D_i - t - E_i$$

Three distinct timing states are recognized:
- **$\text{slack}(t) \le 0$:** **Critical / Infeasible to meet if started now** ($t + E_i \ge D_i$).
- **$t \ge D_i$:** **Overdue** (deadline has elapsed before execution began).
- **$\text{completionTime} > D_i$:** **Deadline Missed** (post-execution observation).

To provide a smooth, bounded, and numerically stable urgency metric without division-by-zero or negative values, an exponential response curve is employed:
$$U_{\text{deadline}}(t) = \begin{cases}
1.0 & \text{if } \text{slack}(t) \le 0 \quad (\text{critical / overdue / zero-slack}) \\
\exp\left(-\frac{\text{slack}(t)}{k \cdot E_i}\right) & \text{if } \text{slack}(t) > 0
\end{cases}$$

- **Parameter $k = 2.0$:** An **initial project design parameter** (initial design choice controlling urgency decay), not a calibrated or mathematically optimal constant.
- **Execution-Time Awareness:** Incorporating $E_i$ in the denominator ensures that a longer task facing the same absolute slack as a shorter task exhibits higher urgency, accurately reflecting greater scheduling risk.

### C. Waiting Factor ($W_{\text{wait}}(t) \in [0.0, 1.0]$)
**[Project Design Decision]**  
Elapsed waiting time for READY tasks is:
$$w_i(t) = \max(0.0, t - A_i)$$

To prevent elapsed waiting time from exploding during high queue delays and drowning out base priority or urgency, it is normalized via a hyperbolic saturation curve:
$$W_{\text{wait}}(t) = \frac{w_i(t)}{w_i(t) + \tau}$$
where $\tau = 50.0$ seconds is an initial project design parameter representing the characteristic queue aging threshold. When $w_i(t) = 0$, $W_{\text{wait}} = 0.0$; when $w_i(t) = \tau$, $W_{\text{wait}} = 0.5$; as $w_i(t) \to \infty$, $W_{\text{wait}} \to 1.0$.

---

## 5. Final Dynamic Priority Formula & Weight Selection

### Formula
**[Project Design Decision]**  
$$\text{FinalScore}(t) = W_p \cdot P_{\text{norm}} + W_d \cdot U_{\text{deadline}}(t) + W_w \cdot W_{\text{wait}}(t)$$

where:
$$\text{HIGHER } \text{FinalScore} \implies \text{HIGHER scheduling priority}$$

### Initial Project Design Weights
- **$W_p = 0.35$ (Base Priority Weight):** Ensures high-priority tasks retain substantial scheduling advantage over relaxed low-priority tasks, preventing total collapse into pure EDF.
- **$W_d = 0.50$ (Deadline Urgency Weight):** Serves as the primary operational weight to dynamically elevate urgent jobs facing deadline expiration, directly targeting Deadline Miss Rate (DMR) reduction.
- **$W_w = 0.15$ (Waiting Factor Weight):** Imposes anti-starvation pressure on older tasks without overriding genuine urgency.
- **Constraint:** $W_p + W_d + W_w = 0.35 + 0.50 + 0.15 = 1.0$.

> [!IMPORTANT]
> **Status:** These values are **initial project design weights**. They are not derived from the IEEE paper and must not be presented as optimal. They are fully configurable via class constructors.

---

## 6. Dynamic Heap Management & Rebuilding Strategy

**[Project Design Decision]**  
Because slack decreases and waiting time increases as simulation clock $t$ advances, task scores fluctuate dynamically:
- Tasks may become more urgent as their deadlines draw nearer.
- A newly arrived high-priority task may enter the queue at any time.
- Because relative task order can both increase and decrease non-monotonically, `FibonacciHeap.decreaseKey` is mathematically insufficient.

### Heap Rebuilding Mechanism:
At each scheduling decision point:
1. Identify all currently READY unallocated tasks ($A_i \le t$).
2. Recalculate dynamic scores for all READY tasks using the current simulation timestamp $t$.
3. Instantiate a fresh standalone `FibonacciHeap<HeapKey, Task>`.
4. Insert tasks into the heap using `HeapKey`:
   - `primaryScore = -FinalScore(t)` (transforms maximum score to min-heap root)
   - `secondaryScore = task.getDeadline()` (earlier deadline tie-breaker)
   - `sequenceId = task.getTaskId()` (strict deterministic tie-breaker)
5. Extract the root node (highest $\text{FinalScore}$) in $O(\log m)$ time.
6. Dispatch to the selected VM.

---

## 7. VM Selection & Overdue Task Policy

### VM Selection Policy
To guarantee rigorous, fair empirical comparability with Phase 6:
- The proposed scheduler uses the **exact same Earliest-Available VM policy** as the baseline.
- Evaluates $\text{effectiveReady}_j = \max(\text{readyTime}_j, A_i)$.
- Selects the VM available earliest; ties are broken by higher MIPS, then lowest VM ID.
- Nominal execution duration is scaled identically using reference MIPS:
  $$\text{actualDuration} = E_i \times \frac{\text{Reference MIPS}}{\text{VM MIPS}_j}$$

### Overdue Task Policy
**[Project Design Decision]**  
If $t \ge D_i$ prior to dispatch:
- The task is **NOT** discarded or dropped.
- It receives maximum urgency score $U_{\text{deadline}} = 1.0$ so it is not unnecessarily delayed further.
- It executes to completion to preserve total workload accounting.
- Upon completion, it is strictly flagged with $\text{deadlineMissed} = \text{true}$ in `ProposedSchedulingResult`.

---

## 8. Deterministic Tie-Breaking Order

When multiple tasks present identical scores, ties are resolved deterministically:
1. **Primary:** Higher $\text{FinalScore}$ (smallest `primaryScore` in min-heap).
2. **Secondary:** Earlier Deadline ($D_i < D_j$).
3. **Tertiary:** Longer Waiting Time ($w_i > w_j$).
4. **Quaternary:** Lowest Task ID ($\text{taskId}_i < \text{taskId}_j$).

---

## 9. Computational Complexity

- **Factor & Score Calculation:** $O(1)$ per task.
- **Dynamic Heap Rebuilding:**
  - For $m$ ready tasks: $m$ insertions into a fresh Fibonacci Heap take $O(m)$ amortized time.
  - Extraction takes $O(\log m)$ amortized time.
- **VM Allocation Search:** $O(n)$ where $n$ is the number of VMs.
- **Overall Scheduling Loop:** For $N$ total tasks across $N$ decision events, the worst-case runtime is $O(N^2 + N \log N + N \cdot n)$. For target benchmark sizes (20, 50, 100 tasks), execution finishes in milliseconds.

---

## 10. Assumptions and Limitations

1. **Initial Parameter Calibration:** The decay parameter $k = 2.0$, waiting parameter $\tau = 50.0$, and weights $(0.35, 0.50, 0.15)$ are initial design choices that may require parametric tuning across varied workload profiles.
2. **Non-Preemptive Execution:** Once a VM begins executing a task, it cannot be preempted, meaning an extremely urgent arriving task must wait until at least one VM finishes its current job.
3. **Decoupled Architecture:** Pure priority calculation classes (`PriorityNormalization`, `DeadlineUrgencyCalculator`, `DeadlineAwarePriorityCalculator`) contain zero dependencies on CloudSim Plus or external frameworks.
