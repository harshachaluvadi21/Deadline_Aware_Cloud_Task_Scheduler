# Cloud Task Scheduling Simulator: User Guide

A comprehensive operating guide for the **Deadline-Aware Priority-Based Task Scheduling** web application and simulation testbed.

---

## 1. Prerequisites & Environment
Ensure the following tools are installed on your host machine:
- **Java Development Kit (JDK)**: Version 21 LTS or newer.
- **Apache Maven**: Version 3.8+ (or use `./mvnw`).
- **Node.js**: Version 18.x or 20.x LTS.
- **npm**: Version 9.x or 10.x.

---

## 2. Starting the Application

### Step 1: Start the Spring Boot Backend Server
Open a terminal in the project root directory (`mini_project/`) and run:
```bash
mvn spring-boot:run
```
The backend initializes embedded Tomcat on port `8080`.
Verify health probe by visiting:
```
http://localhost:8080/api/health
```
*(Expected response: `{"status":"UP"}`)*

### Step 2: Start the React + Vite Frontend Dev Server
Open a second terminal, navigate to `frontend/`, and launch the development server:
```bash
cd frontend
npm install   # If running for the first time
npm run dev
```
The Vite development server will start at:
```
http://localhost:5173
```

### Step 3: Open the Dashboard
Open your modern web browser (Chrome, Firefox, Edge) and navigate to `http://localhost:5173`.
Look at the top-right header:
- `● Backend Connected` (Green badge): Confirms live communication with the Spring Boot simulation engine.
- `● Backend Unavailable` (Red badge): Indicates the backend server is offline or unreachable.

---

## 3. Workload Specification & Setup

You can populate the task workload using four distinct methods:

### Option A: Load Demo Workload
Click the **"✨ Demo Workload"** button in the workload toolbar.
- Loads a deterministic 10-task workload featuring diverse priority levels ($1–10$), staggered arrival times ($0.0–12.0$ s), varied execution lengths, and a realistic mixture of tight and slack deadlines.
- Perfect for demonstrating deadline-sensitive scheduling across the 4-VM cluster.

### Option B: Upload Custom CSV Workload
Click the **"📁 Upload CSV"** button to open the CSV Import Modal:
1. Drag and drop your `.csv` file into the upload zone, or click to browse.
2. The modal displays the selected filename and file size.
3. Click **"Validate CSV"**: The file is sent to `POST /api/workloads/validate`.
4. If validation passes: A green badge confirms success, and a scrollable table previews the parsed tasks. Click **"Load into Simulator"**.
5. If validation fails: A detailed red alert displays every row error (e.g. duplicate IDs, deadline earlier than arrival, priority out of bounds). Correct your CSV and try again.

### Option C: Load Sample Workload
Click **"Sample Workload"** to fetch the default 5-task sample workload from the backend (`GET /api/sample-workload`).

### Option D: Manual Task Entry & Editing
- Click **"+ Add Task"** to append a new synthetic task row.
- Edit any numerical field directly in the table:
  - **Task ID**: Unique non-negative integer.
  - **Priority**: Value between 1 and 10 (10 being highest urgency).
  - **Arrival Time (s)**: Timestamp when task enters the system ($\ge 0.0$).
  - **Execution Time (s)**: Processing duration on a 1000 MIPS VM ($> 0.0$).
  - **Deadline (s)**: Target completion deadline ($\ge \text{ArrivalTime}$).
- Click **"Delete"** to remove any individual task.

---

## 4. Selecting the Scheduling Algorithm

Under the **Scheduling Algorithm** section, choose one of three operating modes:

1. **Compare Both Schedulers (Recommended)**:
   - Concurrently executes the identical workload against both the **IEEE Priority Baseline** and the **Proposed Deadline-Aware Scheduler**.
   - Computes side-by-side performance metrics and exact descriptive differences.
2. **IEEE Priority Baseline Scheduler**:
   - Schedules tasks according to static priorities and estimated waiting times using Fibonacci heap priority queues.
3. **Proposed Deadline-Aware Priority Scheduler**:
   - Evaluates dynamic scores combining base priority ($W_p=0.35$), deadline urgency ($W_d=0.50$), and waiting time ($W_w=0.15$).

---

## 5. Running the Simulation

Click the primary blue button: **"Run Simulation"**.
- During processing, the button changes to **"Running Cloud Simulation..."** and disables inputs to prevent duplicate submissions.
- Discrete-event simulation completes in sub-second time.

---

## 6. Analyzing Results & Visualizations

Upon simulation completion, the dashboard renders comprehensive academic analytics:

### 6.1 Workload Summary Cards
Located above the algorithm selector, displays high-level characteristics of the active workload:
- Total Tasks
- Target Cluster VMs
- Average Base Priority
- Average Execution Time
- Earliest Arrival Timestamp
- Latest Deadline Timestamp

### 6.2 Comparison Table (in Compare Mode)
Side-by-side metric comparison table displaying:
- Makespan (s)
- Average Waiting Time (s)
- Average Turnaround Time (s)
- Throughput (tasks/s)
- Deadline Miss Rate (%)
- VM Resource Utilization (%)
- **Difference**: Strictly defined as $\text{Proposed} - \text{Baseline}$.

### 6.3 Scheduling Metric Visualizations
Interactive comparative bar charts illustrating makespan, waiting time, turnaround time, throughput, deadline miss rate, and cluster resource utilization.

### 6.4 Deadline Adherence Analysis
Dedicated visual breakdown of contractual deadline fulfillment:
- **Completed Before Deadline**: Tasks whose completion time is strictly earlier than deadline ($T_{\text{comp}} < D$).
- **Completed At Deadline**: Tasks completed exactly at deadline ($T_{\text{comp}} = D$).
- **Missed Deadline**: Tasks exceeding deadline ($T_{\text{comp}} > D$).
- Color-coded summary progress bar and detailed slack breakdown table ($Slack = Deadline - Completion$).

### 6.5 Virtual Machine Utilization Breakdown
Individual utilization statistics for VMs 0, 1, 2, and 3:
- Assigned task count.
- Total active busy time.
- Individual VM busy percentage.
- Time-based overall cluster resource utilization.

### 6.6 Interactive Gantt Timeline
Chronological execution dispatching across VM rows:
- Color-coded blocks for each task (Green = met deadline, Cyan = at deadline, Red = missed).
- Hover over any task block to see exact start, completion, deadline, and priority values.
- Horizontal time axis with interval timestamps.
- Horizontal scrolling support for larger workloads.

### 6.7 Scheduled Task Execution Results Table
Complete tabular log with arrival times, start times, completion times, waiting times, and assigned virtual machines.

---

## 7. Interactive Result Filtering

Use the **Interactive Result Filters** toolbar to inspect subsets of results without re-running simulations:
- **VM Filter**: Isolate tasks assigned to VM 0, VM 1, VM 2, VM 3, or View All.
- **Status Filter**: View only completed tasks or deadline misses.
- **Deadline Status**: Filter by Before Deadline, At Deadline, or Missed Deadline.
- **Priority Range**: Filter tasks within a minimum and maximum priority threshold.
- Click **"Reset Filters"** to restore default unfiltered views.
*(Note: Filtering modifies only visual rendering and tables; it does not alter simulation metrics.)*

---

## 8. Exporting Simulation Data

At the bottom of the results dashboard, use the **Export Simulation Results** buttons:
- **📥 Download Task Results CSV**: Downloads `tasks-result.csv` containing task IDs, priorities, arrival times, execution durations, deadlines, assigned VMs, start times, completion times, and status.
- **📊 Download Metrics CSV**: Downloads `metrics-summary.csv` containing all 6 performance metrics and pairwise difference columns.

---

## 9. Resetting the Simulator

Click the red **"🔄 Reset"** button in the workload toolbar:
- Clears the active task list.
- Clears all simulation results, metrics, charts, timeline schedules, and filters.
- Returns the dashboard to a pristine initial state.
