# Viva Questions and Answers Guide

Comprehensive preparation guide containing 45 questions and detailed, technically accurate answers covering all aspects of the **Deadline-Aware Priority-Based Task Scheduling** project.

---

## Category 1: Project Basics

### Q1: What is the core title and objective of this project?
**Answer:**
The project is titled *"Deadline-Aware Priority-Based Task Scheduling in Cloud Computing"*. Its objective is to investigate heuristic cloud task scheduling by reproducing the priority-based heuristic algorithm from an IEEE Access 2023 paper and designing an enhancement that dynamically factors in task deadlines to minimize deadline miss rates while maintaining balanced cluster makespan and resource utilization.

### Q2: What problem does this project solve?
**Answer:**
In multi-tenant cloud datacenters, tasks arrive with varying priorities, durations, and strict SLA deadlines. Static priority schedulers favor high-priority jobs, causing lower-priority urgent tasks to miss deadlines. Conversely, pure Earliest Deadline First (EDF) algorithms ignore business priority hierarchies. This project solves this dilemma by formulating a dynamic multi-objective scoring function that considers base priority, remaining deadline slack, and waiting time simultaneously.

### Q3: Why is task scheduling such a critical problem in cloud computing?
**Answer:**
Cloud resources (virtual machines) are finite and incur operational costs. Inefficient scheduling leads to resource underutilization, server idle power consumption, high task latency, and SLA violations resulting in financial penalties. Effective scheduling maximizes provider profit and ensures quality of service (QoS) for cloud consumers.

### Q4: What is the difference between static and dynamic task scheduling?
**Answer:**
- **Static Scheduling:** All task arrival times, execution requirements, and resource availability are known in advance before execution begins. Tasks are assigned once and cannot be reordered.
- **Dynamic Scheduling:** Tasks arrive at runtime at unpredictable intervals. Scheduling decisions are made on-the-fly based on the current state of queues, elapsed time, and resource availability. Our project implements dynamic, event-driven task scheduling.

---

## Category 2: IEEE Baseline Approach

### Q5: What is the reference paper for the baseline scheduler?
**Answer:**
The baseline reproduces the heuristic approach published by Swati Lipsa, Ranjan Kumar Dash, Nikola Ivkovic, and Korhan Cengiz in *IEEE Access* (2023), titled *"Task Scheduling in Cloud Computing: A Priority-Based Heuristic Approach"*.

### Q6: How does the baseline Priority Assignment to Tasks (PAT) algorithm work?
**Answer:**
PAT computes a priority vector for tasks by evaluating normalized task priority and expected execution characteristics within an $M/M/n$ queuing framework. It assigns higher dispatching precedence to higher nominal priority tasks while balancing expected queuing delays.

### Q7: What is the Waiting Time Matrix (WTM) in the baseline paper?
**Answer:**
The Waiting Time Matrix tracks the estimated queuing delay for tasks across each virtual machine in the cluster. It estimates the wait time on a given VM based on tasks currently queued or running on that VM, assisting in dispatching tasks to VMs where they will complete soonest.

### Q8: Why does the baseline paper use a Fibonacci Heap?
**Answer:**
A Fibonacci Heap is an asymptotically optimal priority queue data structure. It achieves $\mathcal{O}(1)$ amortized time for insertion and key updates (`decreaseKey`), and $\mathcal{O}(\log n)$ time for extracting the highest-priority task. This ensures the scheduler scales efficiently to thousands of queued tasks without becoming a computational bottleneck.

### Q9: What is the primary limitation of the IEEE baseline algorithm?
**Answer:**
The baseline heuristic is **deadline-blind**. It treats priority as static and does not inspect task completion deadlines. Consequently, an urgent task with a near deadline can languish in the queue behind a high-priority task with a distant deadline, leading to avoidable deadline misses.

---

## Category 3: Proposed Deadline-Aware Method

### Q10: What makes the proposed scheduler "deadline-aware"?
**Answer:**
The proposed scheduler actively computes the remaining *slack* for every ready task at each scheduling event and translates it into a dynamic *deadline urgency* metric. As time passes and a task's deadline draws nearer, its priority score dynamically increases.

### Q11: What is "slack" in this project, and how is it defined?
**Answer:**
Slack is the remaining spare time before a task must start to meet its deadline. Mathematically:
$$\text{Slack}(t) = \text{Deadline} - (t + \text{ExecutionTime})$$
- If $\text{Slack}(t) > 0$: The task can still meet its deadline if scheduled before the slack expires.
- If $\text{Slack}(t) \le 0$: The task is in a *critical* state; if it does not begin immediately, it will miss its deadline.

### Q12: What is the dynamic composite priority scoring formula?
**Answer:**
The composite score $S(t)$ is formulated as:
$$S(t) = W_p \cdot P_{\text{norm}} + W_d \cdot U_{\text{deadline}}(t) + W_w \cdot W_{\text{wait}}(t)$$
where $P_{\text{norm}}$ is normalized base priority in $[0, 1]$, $U_{\text{deadline}}(t)$ is deadline urgency in $[0, 1]$, and $W_{\text{wait}}(t)$ is normalized waiting time.

### Q13: What are the weights $W_p$, $W_d$, and $W_w$, and why were they chosen?
**Answer:**
The initial design weights are $W_p = 0.35$, $W_d = 0.50$, and $W_w = 0.15$.
- $W_d = 0.50$ (50%) gives substantial weight to deadline urgency to prevent SLA violations.
- $W_p = 0.35$ (35%) preserves user-specified priority tiers so high-paying or high-importance tasks retain prominence.
- $W_w = 0.15$ (15%) accounts for queuing delay and prevents low-priority tasks with long deadlines from starving indefinitely.
*(Note: These are initial design parameters established for controlled comparison, not claimed as globally optimal).*

### Q14: How is the deadline urgency $U_{\text{deadline}}(t)$ calculated mathematically?
**Answer:**
It is modeled using an exponential decay function:
$$U_{\text{deadline}}(t) = \exp\left( -k \cdot \frac{\text{Slack}(t)}{\text{ExecutionTime}} \right)$$
When slack is large relative to execution time, urgency is near 0. As slack shrinks toward 0, urgency exponentially approaches 1.0. If $\text{Slack}(t) \le 0$, urgency clamps to 1.0.

### Q15: What is the parameter $k = 2.0$, and why is it used?
**Answer:**
$k = 2.0$ is an initial project design parameter controlling the rate of urgency decay. A value of $2.0$ ensures that urgency rises moderately when slack equals execution time ($\exp(-2) \approx 0.135$) and accelerates rapidly as slack vanishes.

### Q16: What is the parameter $\tau = 50.0$?
**Answer:**
$\tau = 50.0$ seconds is the normalization scale factor for waiting time:
$$W_{\text{wait}}(t) = \min\left(1.0, \frac{\text{CurrentTime} - \text{ArrivalTime}}{\tau}\right)$$
It ensures that after 50 seconds of waiting, the waiting component saturates to 1.0, preventing infinite starvation.

### Q17: What happens when a task becomes "critical" or "overdue"?
**Answer:**
- **Critical ($\text{Slack}(t) \le 0$):** Urgency hits 1.0, elevating the task to maximum score within its priority bracket to expedite execution.
- **Overdue ($\text{CurrentTime} \ge \text{Deadline}$):** The task is already past its deadline. Rather than dropping the task and wasting partial cloud compute or harming throughput, the system executes it to completion while strictly recording it as a deadline miss.

### Q18: Why does the proposed scheduler rebuild the Fibonacci Heap instead of using `decreaseKey`?
**Answer:**
As simulation time progresses, task scores can both increase (due to shrinking slack and growing wait time) and decrease relative to newly arriving tasks. In a min-heap ordered by negated score, a higher score corresponds to a *smaller* key. However, if scores can fluctuate in both directions, simple `decreaseKey` is insufficient. Re-evaluating scores of ready tasks and rebuilding the heap ensures exact mathematical correctness at every dispatch decision.

---

## Category 4: CloudSim Plus Simulation

### Q19: What is CloudSim Plus, and why was it chosen?
**Answer:**
CloudSim Plus is a widely respected, open-source discrete-event simulation framework for cloud computing infrastructures. It models datacenters, physical hosts, virtualization hypervisors, virtual machines, and cloudlet execution. It was chosen because it allows reproducible, controlled experimentation on identical hardware specifications without the financial cost and variability of public cloud deployments.

### Q20: What VM specifications are used in the simulation?
**Answer:**
The simulation cluster consists of 4 homogeneous Virtual Machines:
- Each VM has 1 Processing Element (PE) provisioned at **1000 MIPS**.
- 2048 MB RAM, 1000 Mbps bandwidth.
- VMs are hosted on simulated physical datacenter hosts with 12,000 MIPS aggregate compute capacity.

### Q21: What is MIPS?
**Answer:**
MIPS stands for *Million Instructions Per Second*. It is a standard metric representing the computational processing speed of a CPU core in cloud simulation. A 1000 MIPS VM executes 1000 million instruction cycles per second.

### Q22: How is a task's duration converted into CloudSim work?
**Answer:**
A task's execution time in seconds is multiplied by the reference VM MIPS (1000 MIPS) to obtain the Cloudlet length in Million Instructions (MI). For example, a 15.65-second execution time on a 1000 MIPS VM corresponds to a Cloudlet length of 15,650 MI.

---

## Category 5: Fibonacci Heap Data Structure

### Q23: Why implement a custom Fibonacci Heap instead of using Java's `PriorityQueue`?
**Answer:**
1. The reference IEEE Access 2023 paper explicitly relies on a Fibonacci Heap to achieve theoretical $\mathcal{O}(1)$ amortized insertion and decrease-key performance.
2. Java's standard `PriorityQueue` is an array-backed binary min-heap with $\mathcal{O}(\log n)$ insertion and $\mathcal{O}(n)$ search/removal.
3. Implementing a standalone, generic `FibonacciHeap<T>` satisfies strict academic reproducibility.

### Q24: What are the theoretical time complexities of a Fibonacci Heap?
**Answer:**
- **Insert:** $\mathcal{O}(1)$ amortized
- **Find-Min:** $\mathcal{O}(1)$ worst-case
- **Extract-Min:** $\mathcal{O}(\log n)$ amortized
- **Decrease-Key:** $\mathcal{O}(1)$ amortized
- **Union:** $\mathcal{O}(1)$ worst-case
- **Delete:** $\mathcal{O}(\log n)$ amortized

### Q25: If the heap is min-oriented, how does it extract the highest-priority task?
**Answer:**
The heap stores a composite `HeapKey` where the primary sorting value is negated: `primaryKey = -priorityScore`. The smallest negative number corresponds to the largest positive score. Thus, an `extractMin` operation retrieves the task with the highest priority score. Ties are broken deterministically by earlier deadline, longer waiting time, and smaller task ID.

---

## Category 6: Evaluation Metrics

### Q26: What is Makespan, and what does it measure?
**Answer:**
Makespan is the total elapsed time from the arrival of the earliest task to the completion of the last task in the workload:
$$\text{Makespan} = \max_{i}(C_i) - \min_{i}(A_i)$$
It measures total schedule duration and overall cluster execution efficiency.

### Q27: What is the difference between Waiting Time and Turnaround Time?
**Answer:**
- **Waiting Time ($W_i = S_i - A_i$):** The time a task spends in the queue from its arrival ($A_i$) until it begins execution on a VM ($S_i$).
- **Turnaround Time ($T_i = C_i - A_i$):** The total time spent in the system from arrival ($A_i$) until completion ($C_i$). It equals waiting time plus execution time.

### Q28: How is System Throughput calculated?
**Answer:**
Throughput is the rate of successfully completed tasks per second of simulation time:
$$\text{Throughput} = \frac{\text{Total Completed Tasks}}{\text{Makespan}}$$

### Q29: What is Deadline Miss Rate (DMR)?
**Answer:**
DMR is the percentage of submitted tasks whose actual completion time exceeded their specified deadline:
$$\text{DMR} = \left( \frac{\text{Number of Tasks with } C_i > D_i}{\text{Total Submitted Tasks}} \right) \times 100\%$$
*Note: A task completing exactly at its deadline ($C_i = D_i$) is not considered a miss.*

### Q30: How is Time-based VM Resource Utilization calculated?
**Answer:**
$$\text{Utilization} = \frac{\sum_{j=1}^M \text{BusyTime}_j}{M \times \text{Makespan}} \times 100\%$$
where $\text{BusyTime}_j$ is the total active computing time of VM $j$, and $M$ is the number of VMs. It is strictly bounded in $[0, 100]\%$.

---

## Category 7: Experimental Campaign & Results

### Q31: How many workloads and runs were conducted in the experimental campaign?
**Answer:**
The research campaign executed **24 simulation runs** comprising:
- 4 Scenarios: Normal Load, High Load, Tight Deadlines, and Mixed Load.
- 3 Task Scales: 20, 50, and 100 tasks per scenario.
- 2 Schedulers: IEEE Baseline vs. Proposed Deadline-Aware ($4 \times 3 \times 2 = 24$ runs).

### Q32: Why were deterministic seeds used for workload generation?
**Answer:**
Using deterministic pseudo-random seeds (seeds 1001, 2002, 3003, etc.) guarantees 100% scientific reproducibility. Anyone running the suite receives the exact same task arrival sequences, durations, and deadlines.

### Q33: Why must both algorithms be evaluated on the exact same task instances?
**Answer:**
To isolate the algorithmic effect. If baseline and proposed were given different workloads, performance differences could stem from workload variability rather than the scheduling heuristics. We used deep cloning to enforce 100% pairwise input fairness.

### Q34: What was the primary finding regarding deadline miss rate?
**Answer:**
The proposed deadline-aware scheduler achieved dramatic reductions in deadline miss rates in deadline-sensitive scenarios (e.g., in the 20-task tight deadline scenario, DMR dropped from 40.0% under baseline to 5.0% under proposed). In scenarios where deadlines were generous, both algorithms achieved 0% misses.

### Q35: Did the deadline-aware scheduler degrade VM resource utilization or makespan?
**Answer:**
No. VM resource utilization remained virtually identical (often within 1–2 percentage points) between both algorithms because the proposed scheduler reorders tasks according to urgency rather than inserting idle gaps. Makespan differences were minimal and workload-dependent.

### Q36: Why does the project report differences as $\text{Proposed} - \text{Baseline}$ instead of declaring a "winner"?
**Answer:**
In scientific research, heuristic trade-offs depend on workload distribution, arrival burstiness, and SLA margins. No single heuristic is universally optimal under every possible condition. Reporting exact numerical differences ($\Delta = \text{Proposed} - \text{Baseline}$) maintains academic objectivity.

---

## Category 8: Web Application & System Implementation

### Q37: What is the technology stack of the web application?
**Answer:**
- **Backend:** Java 21 LTS, Spring Boot 3.3.4 (REST API, Bean Validation, Spring Web).
- **Frontend:** React 18, TypeScript 5.6, Vite 5.4, Vanilla CSS with custom dark-slate styling.
- **Engine:** CloudSim Plus 8.0.0 and custom scheduling engine.

### Q38: How does the frontend communicate with the backend?
**Answer:**
Via standard asynchronous HTTP REST calls:
- `GET /api/health` for connection monitoring.
- `GET /api/sample-workload` for retrieving default sample tasks.
- `POST /api/simulate` and `POST /api/compare` for simulation execution.
- `POST /api/workloads/validate` for multipart CSV file validation.
- `POST /api/export/tasks` and `POST /api/export/metrics` for CSV downloads.

### Q39: How does the CSV workload validation work?
**Answer:**
The backend enforces an *all-or-nothing* validation policy. It checks:
1. Valid 5-column header (`taskId,priority,arrivalTime,executionTime,deadline`).
2. Unique, non-negative task IDs.
3. Priority in $[1, 10]$.
4. Arrival time $\ge 0.0$.
5. Execution time $> 0.0$.
6. Deadline $\ge$ arrival time.
If any single row fails, the entire file is rejected with HTTP 400 and an itemized error list.

### Q40: How does the Gantt timeline chart work?
**Answer:**
The timeline maps simulation timestamps directly to visual horizontal tracks corresponding to VMs 0, 1, 2, and 3. Each task block begins at `startTime` and spans to `completionTime`. Blocks are color-coded based on deadline adherence: Green for completed before deadline, Cyan for completed at deadline, and Red for missed deadline.

---

## Category 9: Software Engineering & Architecture

### Q41: Why is there no external database in this system?
**Answer:**
The system is designed as an in-memory discrete-event simulation engine and interactive testbed. Task sets and simulation runs are transient, compute-intensive experiments. Storing ephemeral simulation runs in an external SQL/NoSQL database would introduce unnecessary I/O overhead and architectural bloat without adding functional value.

### Q42: Why are the research layer and the web application layer kept strictly separated?
**Answer:**
To protect the integrity of published research. The experimental campaign, raw CSVs, and figures in `results/` are permanently frozen. The web application is an interactive demonstration layer that calls the algorithms dynamically in-memory and never mutates or overwrites research results.

### Q43: How many automated tests exist, and what do they verify?
**Answer:**
There are **135 automated tests** (all passing with 0 errors and 0 failures):
- 18 Fibonacci Heap algorithmic tests (including differential testing against `PriorityQueue`).
- 35 Task model and state transition tests.
- 40 Baseline PAT, WTM, and dispatching tests.
- 20 Proposed deadline scoring and urgency tests.
- 10 REST controller API tests.
- 12 CSV import validation and result export tests.

### Q44: What are the main limitations of this study?
**Answer:**
1. Workloads are synthetically generated rather than sampled from live production clouds.
2. Tasks are non-preemptive; once dispatched to a VM, a task runs to completion without interruption.
3. Network transmission delays and dynamic VM migration costs are abstracted.
4. Experiments were evaluated on a fixed 4-VM cluster topology.

### Q45: What is the future scope of this work?
**Answer:**
1. Replaying production cloud traces such as the Google Cluster Workload Traces or Alibaba traces.
2. Investigating preemptive scheduling mechanisms for mission-critical tasks.
3. Incorporating energy-aware scheduling models that evaluate dynamic voltage and frequency scaling (DVFS) alongside deadline satisfaction.
