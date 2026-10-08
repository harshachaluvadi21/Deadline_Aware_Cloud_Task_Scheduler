# Final Presentation Speech & Speaker Notes

This document provides ready-to-deliver speaker notes designed for a 10-to-12 minute academic presentation. The language is clear, professional, and conversational.

---

### Slide 1: Title & Overview
**Speech (approx. 40 seconds):**
> "Good morning, respected professors and evaluation committee members. Today, our team presents our final mini-project titled **'Deadline-Aware Priority-Based Task Scheduling in Cloud Computing'**.
>
> In this project, we investigated how cloud datacenters schedule incoming computational tasks. Specifically, we reproduced the priority-based heuristic algorithm published in the IEEE Access 2023 paper by Lipsa and colleagues, and then developed a proposed enhancement that introduces dynamic deadline awareness into the scheduling process. We tested both algorithms under discrete-event simulation using CloudSim Plus and packaged our findings into an interactive web application."

---

### Slide 2: Problem Statement
**Speech (approx. 45 seconds):**
> "In modern cloud infrastructure, thousands of tasks arrive continuously from various users. These tasks have different priorities, durations, and strict deadlines agreed upon in Service Level Agreements.
>
> The problem is that traditional priority-based schedulers only look at static task priorities. When high-priority tasks arrive, they get scheduled first, which can cause lower-priority tasks with imminent deadlines to wait too long and miss their deadlines. On the other hand, purely deadline-based algorithms like Earliest Deadline First ignore customer priority levels completely.
>
> Our goal is to balance both factors: respecting task priority while dynamically preventing deadline misses."

---

### Slide 3: Motivation & Need
**Speech (approx. 40 seconds):**
> "Why does this matter? For cloud providers, missing task deadlines leads to SLA violations, financial compensation penalties, and lost customer trust.
>
> In real cloud workloads, a task's urgency is not fixed—it increases as current time gets closer to its deadline. If a scheduling algorithm does not account for this decaying time slack, it makes suboptimal dispatch decisions.
>
> Therefore, we needed a controlled, reproducible simulation study to measure whether introducing dynamic deadline urgency into priority scheduling can improve deadline compliance without degrading cluster throughput or resource utilization."

---

### Slide 4: Existing Approach (IEEE Access 2023)
**Speech (approx. 50 seconds):**
> "To establish a rigorous baseline, we faithfully reproduced the heuristic approach from the IEEE Access 2023 paper.
>
> Their system uses four main components:
> First, the Priority Assignment to Tasks algorithm, which assigns nominal priority weights.
> Second, a Waiting Time Matrix that estimates waiting times across available virtual machines.
> Third, a standalone min-oriented Fibonacci Heap that stores ready tasks and extracts the highest-priority task in logarithmic time.
> And fourth, a dispatching rule that assigns the task to the VM that minimizes expected completion time.
>
> However, the key limitation is that this baseline algorithm is deadline-blind. It cannot distinguish between a task that has plenty of spare time and one that is about to expire."

---

### Slide 5: Research Gap
**Speech (approx. 40 seconds):**
> "This brings us to the research gap.
>
> In the baseline system, two tasks with identical base priority receive the exact same scheduling preference, even if one task expires in two seconds and the other expires in two hours.
>
> Furthermore, once a task starts waiting in the queue, its priority remains static. There is no mathematical mechanism to elevate its score as its deadline approaches. We recognized the need for a composite scoring model that accounts for priority, deadline urgency, and waiting time simultaneously."

---

### Slide 6: Proposed Approach (Deadline-Aware Priority Scheduler)
**Speech (approx. 50 seconds):**
> "To address this gap, we implemented the Proposed Deadline-Aware Priority Scheduler.
>
> Instead of using static priority, our scheduler computes a dynamic composite score for every task whenever a scheduling decision is made.
>
> This score incorporates three essential factors: the base priority of the task, its dynamic deadline urgency calculated from remaining slack, and its accrued waiting time to prevent starvation.
>
> Because task urgency changes as simulation time progresses, our proposed scheduler dynamically re-evaluates task scores and rebuilds the Fibonacci Heap at decision points, ensuring that the most urgent eligible task is always extracted next."

---

### Slide 7: System Architecture
**Speech (approx. 45 seconds):**
> "Here we see our system architecture. A key design principle of our project is the strict separation between our research simulation core and our web application layer.
>
> On the research side, we have our scheduling algorithms, the standalone Fibonacci Heap, and the CloudSim Plus discrete-event simulation engine. All experimental runs, seeds, and datasets here are frozen and permanent.
>
> On top of this, we built an interactive web layer using Spring Boot for REST APIs and React with Vite and TypeScript for the frontend dashboard. The web app allows users to interactively test workloads and visualize results in real time without altering our published research data."

---

### Slide 8: Proposed Algorithm Workflow & Formulations
**Speech (approx. 50 seconds):**
> "Let us look at the mathematical formulation behind our proposed scheduler, which consists of three distinct components:
>
> First is the Dynamic Deadline Urgency Algorithm. It calculates Omega(t) using the remaining slack and burst execution time B, scaled by a numerical safety guard that prevents division by zero or negative values when tasks become overdue.
>
> Second is the Dynamic Priority Algorithm. It computes P(t) as 100 times the base priority, plus Omega(t), plus 10 times the queue waiting time. This keeps the user's base priority immutable while dynamically elevating urgent tasks and preventing queue starvation.
>
> Third is the Persistent Fibonacci-Heap Scheduling Algorithm. We maintain a single persistent Min-Heap keyed by negative P(t). As time advances, waiting tasks are updated in-place via decreaseKey in O(1) amortized time, and the highest-priority task is dispatched via extractMin.
>
> We validate this using representative cloud workload types—such as Image Processing, Video Transcoding, and Database Queries—to reflect realistic computing demands."

---

### Slide 9: Technology Stack
**Speech (approx. 35 seconds):**
> "Our technology stack was chosen for performance, academic rigor, and clean design.
>
> On the backend, we use Java 21 LTS with Spring Boot 3.3.4 and Apache Maven. We integrated CloudSim Plus 8.0.0 for discrete-event datacenter modeling, and wrote a custom standalone Fibonacci Heap from scratch.
>
> On the frontend, we used React 18, Vite, and TypeScript with a custom responsive dark theme. Our automated test suite includes 135 unit and integration tests, all of which pass with zero errors and zero failures."

---

### Slide 10: Experimental Methodology
**Speech (approx. 45 seconds):**
> "To ensure our experimental findings were scientifically valid, we maintained a strict principle of controlled comparison.
>
> We generated workloads across four distinct scenarios: Normal Load, High Load, Tight Deadlines, and Mixed Load, testing at three scales: 20, 50, and 100 tasks.
>
> For every single experiment, we used deep cloning so that both the IEEE baseline and our proposed scheduler received the exact same task IDs, arrival timestamps, execution lengths, priorities, and deadlines on an identical 4-VM cluster. In total, we executed a 24-run controlled campaign."

---

### Slide 11: Evaluation Metrics
**Speech (approx. 45 seconds):**
> "We evaluated both algorithms across six standard cloud scheduling metrics:
>
> Makespan, which is the total time to complete the entire workload;
> Average Waiting Time;
> Average Turnaround Time;
> System Throughput in tasks per second;
> Deadline Miss Rate, which is the percentage of tasks that completed after their deadline;
> And Time-based VM Resource Utilization, which measures how actively the virtual machines were utilized across the makespan.
>
> All comparative differences are calculated strictly as Proposed minus Baseline using neutral descriptive terminology."

---

### Slide 12: Experimental Results & Observations
**Speech (approx. 50 seconds):**
> "Here are the key observations from our 24-run experimental campaign.
>
> In scenarios with tight deadlines and high queuing load, our proposed deadline-aware scheduler achieved a substantial reduction in deadline misses. For example, in our 20-task tight deadline scenario, the deadline miss rate dropped from 40% under the baseline to just 5% under the proposed scheduler.
>
> Importantly, VM resource utilization remained virtually identical across both algorithms, confirming that prioritizing urgent tasks does not leave VMs idle or waste cloud capacity.
>
> Rather than claiming one algorithm is universally superior, our results objectively show that the proposed approach successfully trades off small variations in waiting time to achieve dramatically better deadline compliance."

---

### Slide 13: Interactive Web Dashboard
**Speech (approx. 40 seconds):**
> "To make our research accessible and practical, we developed a comprehensive web dashboard.
>
> The dashboard presents high-level Workload Summary cards, a Side-by-Side Comparison matrix showing exact metric differences, interactive comparative bar charts, and a detailed breakdown of VM utilization across each individual virtual machine in the cluster."

---

### Slide 14: Demonstration & Workload Capabilities
**Speech (approx. 50 seconds):**
> "Our application includes several powerful demonstration features.
>
> Users can click 'Demo Workload' to instantly load a deterministic 10-task benchmark, or upload their own custom CSV files with automatic all-or-nothing validation that checks for duplicate IDs, invalid priorities, or inverted deadlines.
>
> The dashboard also features an interactive Gantt timeline that maps actual task execution intervals across VM tracks, a dedicated Deadline Adherence analysis tool, interactive filters by VM and status, and one-click CSV export for all results."

---

### Slide 15: Conclusion & Future Scope
**Speech (approx. 45 seconds):**
> "In conclusion, our project has successfully achieved all its objectives.
>
> We faithfully reproduced the IEEE Access 2023 baseline scheduler, developed a validated deadline-aware dynamic priority enhancement, executed a rigorous 24-run experimental campaign, and packaged the entire system into a tested, full-stack web application.
>
> For future work, we plan to evaluate this heuristic against real-world production traces from Google or Alibaba datacenters, explore preemptive task scheduling, and incorporate energy-aware dynamic VM voltage scaling.
>
> Thank you for your time and attention. We now welcome your questions."

---
