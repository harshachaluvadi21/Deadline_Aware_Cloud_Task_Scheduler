# Empirical Evaluation and Controlled Experimental Results Summary

## 1. Experimental Setup

The controlled experiment evaluated the **Phase 6 IEEE Baseline Scheduler** and the **Phase 7 Proposed Deadline-Aware Priority Scheduler** under identical conditions within a simulated cloud environment.

- **Simulation Platform:** CloudSim Plus 8.0.0 on Java 21 LTS
- **Queuing Model:** Discrete-event non-preemptive multi-server queuing framework ($M/M/n$)
- **Priority Queue Implementation:** Dedicated standalone Fibonacci Heap (`FibonacciHeap<HeapKey, Task>`)
- **Virtual Machine Topology:** 4 homogeneous virtual machines (VM IDs: 0, 1, 2, 3)
  - MIPS Rating per VM: 1000.0 MIPS
  - Reference MIPS: 1000.0 MIPS (Duration scaling ratio = 1.0)
  - RAM: 2048 MB, Bandwidth: 1000 Mbps, Storage: 10,000 MB
- **Allocation Policy:** Earliest Available VM (Earliest Ready Time, breaking ties by lowest VM ID)

---

## 2. Workload Scenarios & Configurations

Four standardized scenarios were evaluated across three task scales ($N \in \{20, 50, 100\}$), yielding 12 unique workloads and 24 scheduler runs:

| Scenario | Deterministic Seed | Arrival Interval (s) | Execution Time (s) | Initial Slack Multiplier | Characteristics |
|---|:---:|:---:|:---:|:---:|---|
| `NORMAL_LOAD` | 1001 | $[0.5, 2.5]$ | $[5.0, 20.0]$ | $[1.5, 3.5]$ | Moderate arrival cadence, moderate task durations, comfortable deadline margins |
| `HIGH_LOAD` | 2001 | $[0.1, 0.8]$ | $[15.0, 40.0]$ | $[0.8, 2.0]$ | Rapid arrival cadence, heavy task workloads, high VM queue contention |
| `DEADLINE_SENSITIVE` | 3001 | $[0.4, 2.0]$ | $[5.0, 30.0]$ | $[0.2, 1.2]$ | Stringent deadline buffers with varied task execution times |
| `MIXED` | 4001 | $[0.2, 3.0]$ | $[3.0, 45.0]$ | $[0.2, 4.0]$ | Broad spectrum of arrival gaps, task lengths, and slack factors |

Workload task sets were generated using `java.util.Random(seed)` and verified against 8 integrity invariants (unique task IDs, valid priorities $[1, 10]$, non-negative arrivals, strictly positive durations, valid deadlines, and absence of NaN/Infinity).

---

## 3. Metrics Evaluated

1. **Time-based VM Resource Utilization (%):**
   $$\text{ResourceUtilization} = \frac{\sum(\text{VM busy time})}{M \times \text{experiment makespan}} \times 100$$
   where $\sum(\text{VM busy time}) = \sum_{i=1}^N (\text{completionTime}_i - \text{startTime}_i)$, strictly bounded within $[0.0, 100.0]\%$.
2. **Makespan (s):** $\max_i(\text{completionTime}_i) - \min_i(\text{arrivalTime}_i)$
3. **Average Waiting Time (s):** $\frac{1}{N} \sum_{i=1}^N (\text{startTime}_i - \text{arrivalTime}_i)$
4. **Average Turnaround Time (s):** $\frac{1}{N} \sum_{i=1}^N (\text{completionTime}_i - \text{arrivalTime}_i)$
5. **Throughput (tasks/s):** $N_{\text{completed}} / \text{makespan}$
6. **Deadline Miss Rate (%):** $(N_{\text{missed}} / N_{\text{total}}) \times 100$, where a miss occurs when $\text{completionTime} > \text{deadline}$.

---

## 4. Raw Empirical Observations

Below are the exact measurements recorded across all 12 experimental configurations:

| Scenario | Tasks | Scheduler | Makespan (s) | Avg Wait (s) | Avg Turnaround (s) | Throughput (tasks/s) | Missed Tasks | DMR (%) | Time-based Utilization (%) |
|---|:---:|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| `NORMAL_LOAD` | 20 | IEEE_BASELINE | 70.9700 | 14.0220 | 26.0205 | 0.2818 | 5 | 25.00% | 84.53% |
| `NORMAL_LOAD` | 20 | PROPOSED_DEADLINE_AWARE | 67.6200 | 12.6800 | 24.6785 | 0.2958 | 1 | 5.00% | 88.72% |
| `NORMAL_LOAD` | 50 | IEEE_BASELINE | 162.5700 | 36.5830 | 48.7594 | 0.3076 | 20 | 40.00% | 93.62% |
| `NORMAL_LOAD` | 50 | PROPOSED_DEADLINE_AWARE | 162.5600 | 34.2332 | 46.4096 | 0.3076 | 20 | 40.00% | 93.63% |
| `NORMAL_LOAD` | 100 | IEEE_BASELINE | 315.1700 | 78.8016 | 91.0891 | 0.3173 | 51 | 51.00% | 97.47% |
| `NORMAL_LOAD` | 100 | PROPOSED_DEADLINE_AWARE | 313.8300 | 73.4795 | 85.7670 | 0.3186 | 56 | 56.00% | 97.88% |
| `HIGH_LOAD` | 20 | IEEE_BASELINE | 151.8100 | 54.4150 | 82.7025 | 0.1317 | 12 | 60.00% | 93.17% |
| `HIGH_LOAD` | 20 | PROPOSED_DEADLINE_AWARE | 152.7000 | 53.2975 | 81.5850 | 0.1310 | 14 | 70.00% | 92.62% |
| `HIGH_LOAD` | 50 | IEEE_BASELINE | 361.3100 | 154.3804 | 182.5016 | 0.1384 | 42 | 84.00% | 97.29% |
| `HIGH_LOAD` | 50 | PROPOSED_DEADLINE_AWARE | 359.9700 | 149.9540 | 178.0752 | 0.1389 | 44 | 88.00% | 97.65% |
| `HIGH_LOAD` | 100 | IEEE_BASELINE | 697.9900 | 314.5416 | 342.0252 | 0.1433 | 92 | 92.00% | 98.44% |
| `HIGH_LOAD` | 100 | PROPOSED_DEADLINE_AWARE | 691.9700 | 302.7518 | 330.2354 | 0.1445 | 95 | 95.00% | 99.29% |
| `DEADLINE_SENSITIVE` | 20 | IEEE_BASELINE | 99.3200 | 24.6790 | 42.1645 | 0.2014 | 11 | 55.00% | 88.03% |
| `DEADLINE_SENSITIVE` | 20 | PROPOSED_DEADLINE_AWARE | 97.4300 | 27.0365 | 44.5220 | 0.2053 | 13 | 65.00% | 89.73% |
| `DEADLINE_SENSITIVE` | 50 | IEEE_BASELINE | 226.4700 | 73.3016 | 91.0644 | 0.2208 | 37 | 74.00% | 98.04% |
| `DEADLINE_SENSITIVE` | 50 | PROPOSED_DEADLINE_AWARE | 231.6100 | 75.8804 | 93.6432 | 0.2159 | 42 | 84.00% | 95.87% |
| `DEADLINE_SENSITIVE` | 100 | IEEE_BASELINE | 435.4300 | 143.1456 | 160.0054 | 0.2297 | 77 | 77.00% | 96.80% |
| `DEADLINE_SENSITIVE` | 100 | PROPOSED_DEADLINE_AWARE | 432.7700 | 151.9932 | 168.8530 | 0.2311 | 89 | 89.00% | 97.39% |
| `MIXED` | 20 | IEEE_BASELINE | 128.5300 | 33.4055 | 57.1310 | 0.1556 | 8 | 40.00% | 92.30% |
| `MIXED` | 20 | PROPOSED_DEADLINE_AWARE | 146.9600 | 30.0965 | 53.8220 | 0.1361 | 6 | 30.00% | 80.72% |
| `MIXED` | 50 | IEEE_BASELINE | 328.5700 | 102.0544 | 127.2578 | 0.1522 | 32 | 64.00% | 95.88% |
| `MIXED` | 50 | PROPOSED_DEADLINE_AWARE | 333.5200 | 96.0262 | 121.2296 | 0.1499 | 27 | 54.00% | 94.46% |
| `MIXED` | 100 | IEEE_BASELINE | 587.4500 | 189.5901 | 212.4443 | 0.1702 | 76 | 76.00% | 97.26% |
| `MIXED` | 100 | PROPOSED_DEADLINE_AWARE | 590.8900 | 189.8144 | 212.6686 | 0.1692 | 76 | 76.00% | 96.69% |

---

## 5. Pairwise Differences (Proposed $-$ Baseline)

Differences are defined mathematically as $\Delta = \text{Proposed} - \text{Baseline}$.

| Scenario | Tasks | $\Delta$ Makespan (s) | $\Delta$ Avg Wait (s) | $\Delta$ Turnaround (s) | $\Delta$ Throughput (tasks/s) | $\Delta$ DMR (abs) | $\Delta$ DMR (%) | $\Delta$ Utilization (%) |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| `NORMAL_LOAD` | 20 | -3.3500 | -1.3420 | -1.3420 | +0.0140 | -0.2000 | -80.00% | +4.19% |
| `NORMAL_LOAD` | 50 | -0.0100 | -2.3498 | -2.3498 | 0.0000 | 0.0000 | 0.00% | +0.01% |
| `NORMAL_LOAD` | 100 | -1.3400 | -5.3221 | -5.3221 | +0.0013 | +0.0500 | +9.80% | +0.42% |
| `HIGH_LOAD` | 20 | +0.8900 | -1.1175 | -1.1175 | -0.0007 | +0.1000 | +16.67% | -0.54% |
| `HIGH_LOAD` | 50 | -1.3400 | -4.4264 | -4.4264 | +0.0005 | +0.0400 | +4.76% | +0.36% |
| `HIGH_LOAD` | 100 | -6.0200 | -11.7898 | -11.7898 | +0.0012 | +0.0300 | +3.26% | +0.86% |
| `DEADLINE_SENSITIVE` | 20 | -1.8900 | +2.3575 | +2.3575 | +0.0039 | +0.1000 | +18.18% | +1.71% |
| `DEADLINE_SENSITIVE` | 50 | +5.1400 | +2.5788 | +2.5788 | -0.0049 | +0.1000 | +13.51% | -2.18% |
| `DEADLINE_SENSITIVE` | 100 | -2.6600 | +8.8476 | +8.8476 | +0.0014 | +0.1200 | +15.58% | +0.60% |
| `MIXED` | 20 | +18.4300 | -3.3090 | -3.3090 | -0.0195 | -0.1000 | -25.00% | -11.57% |
| `MIXED` | 50 | +4.9500 | -6.0282 | -6.0282 | -0.0023 | -0.1000 | -15.62% | -1.42% |
| `MIXED` | 100 | +3.4400 | +0.2243 | +0.2243 | -0.0010 | 0.0000 | 0.00% | -0.57% |

---

## 6. Analytical Observations by Scenario

1. **NORMAL_LOAD:**
   - At 20 tasks, the proposed scheduler observed a lower deadline miss rate (5.00% vs 25.00%) and lower average waiting time (12.68 s vs 14.02 s).
   - At 50 tasks, both schedulers observed identical deadline miss rates (40.00%) and virtually identical makespan (162.56 s vs 162.57 s).
   - At 100 tasks, the baseline scheduler observed a lower deadline miss rate (51.00% vs 56.00%), while the proposed scheduler observed lower average waiting time (73.48 s vs 78.80 s).

2. **HIGH_LOAD:**
   - Under heavy task queuing, the proposed scheduler observed lower average waiting times across all task scales: $\Delta = -1.12$ s (20 tasks), $-4.43$ s (50 tasks), and $-11.79$ s (100 tasks).
   - The baseline scheduler observed lower deadline miss rates across all task scales: $60.00\%$ vs $70.00\%$ (20 tasks), $84.00\%$ vs $88.00\%$ (50 tasks), and $92.00\%$ vs $95.00\%$ (100 tasks). This occurs because prioritizing urgent tasks when capacity is exhausted pushes other closely-timed tasks beyond their deadlines.

3. **DEADLINE_SENSITIVE:**
   - The baseline scheduler observed lower deadline miss rates across all three task counts (55.00% vs 65.00% at 20 tasks; 74.00% vs 84.00% at 50 tasks; 77.00% vs 89.00% at 100 tasks).
   - The proposed scheduler exhibited higher average waiting times ($\Delta = +2.36$ s, $+2.58$ s, $+8.85$ s) due to frequent queue re-evaluations under tight deadline constraints.

4. **MIXED:**
   - In heterogeneous workloads, the proposed scheduler observed lower deadline miss rates at 20 tasks ($30.00\%$ vs $40.00\%$, $\Delta = -10.00\%$) and 50 tasks ($54.00\%$ vs $64.00\%$, $\Delta = -10.00\%$), with identical DMR at 100 tasks ($76.00\%$).
   - The baseline scheduler observed shorter makespans for 20 tasks (128.53 s vs 146.96 s) and 50 tasks (328.57 s vs 333.52 s).

---

## 7. Experimental Limitations

1. **Synthetic Workloads:** Workloads were generated using parameterized pseudorandom distributions rather than recorded production datacenter traces (e.g. Google cluster or Alibaba traces).
2. **Single Deterministic Seed per Scenario:** Each scenario/taskCount pair was evaluated with one deterministic seed. These results represent specific deterministic runs, not population distributions.
3. **Absence of Statistical Hypothesis Testing:** No $p$-values, confidence intervals, or ANOVA models can be asserted without multi-seed sampling.
4. **Initial Parameter Settings:** The parameters $k = 2.0$, $\tau = 50.0$, and weights ($W_p = 0.35, W_d = 0.50, W_w = 0.15$) represent the initial project design values and were not optimized or tuned against experimental outputs.
5. **Simulated Hardware Model:** CloudSim Plus assumes ideal network transfers without physical packet loss, memory thrashing, or hypervisor virtualization overhead.

---

## 8. Reproducibility Guarantee

All experiments are 100% reproducible:
- The full campaign can be re-run by executing:
  ```powershell
  java -cp "target/classes;target/test-classes;..." scheduler.experiment.CampaignRunner results
  ```
- All generated workload CSVs are preserved in `results/workloads/`.
- Every run is tracked in `results/manifests/final_experiment_manifest.csv`.
