# Live Demonstration Script (3–5 Minutes)

A step-by-step guide for delivering a smooth, professional demonstration of the **Deadline-Aware Priority-Based Task Scheduling** web application to evaluation examiners.

---

## Pre-Demo Checklist (1 Minute Before Demo)
1. Ensure Terminal 1 is running:
   ```bash
   mvn spring-boot:run
   ```
2. Ensure Terminal 2 is running:
   ```bash
   cd frontend && npm run dev
   ```
3. Open a clean browser window at:
   ```
   http://localhost:5173
   ```

---

## Step-by-Step Demonstration Flow

### Step 1: Health & Connectivity (15 seconds)
- **Action:** Point to the top-right corner of the web header.
- **Say:**
  > *"As you can see on the top right, our live health probe confirms `Backend Connected` to the Spring Boot REST API running on port 8080. The backend is connected to our discrete-event simulation engine powered by CloudSim Plus."*

---

### Step 2: Load the Deterministic Demo Workload (30 seconds)
- **Action:** In the Workload Specification section, click the **"✨ Demo Workload"** button.
- **Say:**
  > *"I will now load our preconfigured 10-task demo workload by clicking 'Demo Workload'. Each task has an ID, a base priority from 1 to 10, an arrival time in seconds, nominal execution duration on a 1000 MIPS VM, and an absolute completion deadline. Notice that the Workload Summary cards instantly update to show total tasks, cluster VMs, average priority, and execution spans."*

---

### Step 3: Select Algorithm & Run Simulation (25 seconds)
- **Action:** In the Algorithm Selector, ensure **"Compare Both (Controlled)"** is selected. Click the large blue **"Run Simulation"** button.
- **Say:**
  > *"We select 'Compare Both' to execute a controlled experiment where both the IEEE baseline and our proposed deadline-aware scheduler receive exact clones of these 10 tasks. I'll click 'Run Simulation'. The backend dispatches the simulation in-memory and returns results in sub-second time."*

---

### Step 4: Explain Comparative Metrics (45 seconds)
- **Action:** Scroll to the **Comparative Simulation Analysis** section and highlight the **Comparison Table** and **Metric Visualizations**.
- **Say:**
  > *"Here we see our side-by-side comparison across all six research metrics: Makespan, Average Waiting Time, Average Turnaround Time, Throughput, Deadline Miss Rate, and VM Resource Utilization.
  >
  > Notice the difference column, calculated strictly as Proposed minus Baseline. In this workload, the proposed algorithm reduces the deadline miss rate while preserving virtually identical cluster resource utilization at around 75%."*

---

### Step 5: Inspect Deadline Adherence Analysis (30 seconds)
- **Action:** Point to the **Deadline Analysis** section showing the green, cyan, and red progress bars.
- **Say:**
  > *"In this section, we analyze deadline adherence. Our system classifies tasks into three categories: Completed Strictly Before Deadline, Completed At Deadline, and Missed Deadline. Notice how the proposed scheduler prioritizes tasks with decaying slack, keeping more tasks within their SLA window."*

---

### Step 6: Virtual Machine Allocation Timeline (Gantt Chart) (35 seconds)
- **Action:** Scroll down to the **Virtual Machine Allocation Timeline**. Hover your mouse over one or two task blocks on VM 0 and VM 1.
- **Say:**
  > *"This is our interactive Gantt timeline. It maps the actual simulation timestamps across our 4 virtual machines. Green blocks represent tasks that met their deadlines, while red blocks indicate missed deadlines. When we hover over any task block, a tooltip displays the task ID, priority, exact start time, completion time, and deadline status."*

---

### Step 7: Interactive Filters (25 seconds)
- **Action:** In the **Interactive Result Filters** toolbar, change the VM filter to **"VM 1"**, and then change Status to **"Completed (Success)"**.
- **Say:**
  > *"We have also built interactive filters that allow researchers or examiners to isolate results by specific VM, status, priority range, or deadline classification. Notice that the tables and timelines filter dynamically on the client side without altering underlying simulation metrics."*
- **Action:** Click **"Reset Filters"**.

---

### Step 8: Custom Workload CSV Upload & Validation (40 seconds)
- **Action:** Click the **"📁 Upload CSV"** button in the workload toolbar.
- **Say:**
  > *"The application also supports custom workload ingestion. Let's open the CSV Upload Modal. It supports drag-and-drop or file selection."*
- **Action:** Select a valid CSV file (or `frontend/public/demo-workload.csv`) and click **"Validate CSV"**.
- **Say:**
  > *"Our backend performs strict All-or-Nothing validation. It checks for duplicate IDs, invalid priority ranges, and ensures deadlines are not earlier than arrival times. Once validated, a preview table appears, and we can load it straight into the simulator."*
- **Action:** Click **"Cancel"** or close the modal.

---

### Step 9: Exporting Research Results (20 seconds)
- **Action:** Scroll to the bottom and point to the **Export Simulation Results** buttons. Click **"📥 Download Task Results CSV"**.
- **Say:**
  > *"Finally, users can export results for further academic analysis. Clicking 'Download Task Results CSV' immediately downloads a standard RFC 4180 CSV file containing all discrete execution timestamps, assigned VM IDs, and terminal statuses."*

---

### Step 10: Reset Application (15 seconds)
- **Action:** Scroll to the top and click the red **"🔄 Reset"** button.
- **Say:**
  > *"Clicking 'Reset' clears all tasks, metrics, charts, timeline schedules, and filters, returning the simulator to a clean initial state. This concludes our live demonstration. Thank you."*

---
