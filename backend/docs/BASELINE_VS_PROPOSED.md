# Baseline vs. Proposed Scheduler: Architectural & Algorithmic Comparison

## 1. Overview

This document provides a factual, side-by-side comparative analysis of the **IEEE Access 2023 Baseline Priority Scheduler** and the **Proposed Deadline-Aware Priority Scheduler**.

To maintain academic rigor and experimental integrity:
- No claims are made regarding which approach is inherently "better".
- No speculative performance figures are reported ahead of experimental evaluation (Phases 11–14).
- Comparisons focus strictly on mathematical formulations, algorithmic flows, data structures, and operational assumptions.

---

## 2. Factual Side-by-Side Comparison

| Aspect / Dimension | IEEE Access 2023 Baseline (*Lipsa et al.*) | Proposed Deadline-Aware Scheduler | Notes & Architectural Rationale |
|---|---|---|---|
| **Primary Scientific Origin** | Swati Lipsa et al., *IEEE Access* (2023), DOI: 10.1109/ACCESS.2023.3255781 | Extended research enhancement proposed in this project | Clear separation ensures baseline strictly reflects published literature. |
| **Base Priority ($p_i$)** | Exists in Task model but is **not** used in the mathematical WTM calculation. | Normalized: $P_{\text{norm}} = \frac{p_i}{10.0} \in [0.1, 1.0]$. | Proposed scheduler explicitly preserves application/user importance. |
| **Deadline Awareness ($D_i$)** | **None.** Deadline is metadata only. | **Active factor.** Evaluates remaining slack $\text{slack}(t) = D_i - t - E_i$. | Key enhancement of the proposed system. |
| **Deadline Urgency Metric** | None. | Exponential decay: $U_{\text{deadline}}(t) = \exp\left(-\frac{\text{slack}}{k \cdot E_i}\right)$ for $\text{slack} > 0$; $1.0$ for $\text{slack} \le 0$. | Initial project design parameter $k = 2.0$. |
| **Waiting Time ($w_i$)** | Sequential cumulative waiting time: $WT(T_k) = \sum_{j=1}^{k-1} PT(T_j)$. | Elapsed waiting time: $w_i(t) = t - A_i$, normalized via $W_{\text{wait}} = \frac{w}{w + \tau}$. | Baseline models projected arrival order; Proposed models actual elapsed simulation wait. |
| **Execution Time ($E_i$)** | Cumulative sum in $WT$ calculation. | Incorporated in slack buffer and urgency decay denominator ($k \cdot E_i$). | Execution time creates deadline pressure in the proposed model without double-counting. |
| **Priority Calculation** | Eigenvector power iteration on Waiting Matrix ($WM$); Priority $= 1.0 - egvt_k$. | Convex linear combination: $\text{FinalScore} = 0.35 P_{\text{norm}} + 0.50 U_{\text{deadline}} + 0.15 W_{\text{wait}}$. | Baseline is $O(m^3)$ AHP-based; Proposed is $O(1)$ per-task multi-factor score. |
| **Priority Evolution** | Computed on task batch admission; static during queue wait. | **Dynamic.** Recomputed at every scheduling decision event as clock $t$ advances. | Urgency increases as deadlines approach in Proposed. |
| **Queue Data Structure** | Custom Min-Oriented Fibonacci Heap (`FibonacciHeap<HeapKey, Task>`). | Custom Min-Oriented Fibonacci Heap (`FibonacciHeap<HeapKey, Task>`). | Identical Phase 5 data structure used by both. |
| **Heap Key Mapping** | `primaryScore = 1.0 - dynamicPriority` | `primaryScore = -FinalScore` | Both map highest-priority task to the min-heap root. |
| **Heap Management** | Tasks inserted upon arrival; extracted sequentially. | Fresh heap rebuilt at each decision event to reflect updated urgency. | Accommodates non-monotonic score shifts in Proposed. |
| **VM Environment** | Heterogeneous VM cluster (500 to 2500 MIPS). | Heterogeneous VM cluster (500 to 2500 MIPS). | Identical datacenter configuration guarantees experimental fairness. |
| **VM Selection Policy** | Earliest Available VM ($\min(\text{readyTime})$), tie-break by higher MIPS, then VM ID. | Earliest Available VM ($\min(\text{readyTime})$), tie-break by higher MIPS, then VM ID. | Identical multi-server ($M/M/n$) dispatching policy. |
| **Scheduling Mode** | Non-preemptive event-driven. | Non-preemptive event-driven. | Neither implements speculative preemption. |
| **Overdue Task Handling** | Allowed to execute; marked as deadline-missed if $completion > deadline$. | Allowed to execute with maximum urgency ($U=1.0$); marked as deadline-missed if $completion > deadline$. | Neither algorithm drops overdue tasks. |
| **Deadline Miss Metric** | Observed post-execution result only ($completionTime > deadline$). | Both a scheduling input (urgency/slack) and an observed post-execution metric. | Baseline observes deadlines; Proposed actively attempts to meet them. |

---

## 3. Methodological Integrity & Controlled Comparison

The architecture guarantees that when benchmarks (20, 50, 100 tasks) are executed in later experimental phases:
1. **Identical Workloads:** Both schedulers consume the exact same `Task` objects (same arrival times, execution durations, deadlines, and base priorities).
2. **Identical Hardware:** Both schedulers run on the exact same VM definitions and MIPS capacities.
3. **Identical VM Assignment:** Both schedulers use the exact same earliest-available VM selection logic.
4. **Isolated Independent Variable:** The **only** difference between the two systems is the priority calculation and queue ordering heuristic.
