# Web Application Setup & Execution Guide

This guide details instructions for building, testing, and running both the Spring Boot backend and the React + Vite frontend.

---

## 1. Prerequisites

- **Java Development Kit:** JDK 21 LTS (Oracle OpenJDK or Eclipse Temurin)
- **Build Tool:** Apache Maven 3.9+
- **Node.js Environment:** Node.js v18+, v20+, or v22+ and npm 9+
- **Modern Web Browser:** Chrome, Firefox, Edge, or Safari

---

## 2. Backend (Spring Boot)

### Build & Run Tests
From the project root (`c:\4-1 Mini Project\mini_project`):
```powershell
mvn clean test
```
*Current test suite: 135 tests passing (113 research + 22 web/CSV), 0 failures, 0 errors, 0 skipped.*

### Start the Spring Boot Backend Server
```powershell
mvn spring-boot:run
```
Or build the executable jar:
```powershell
mvn package -DskipTests
java -jar target/deadline-aware-scheduler-1.0.0-SNAPSHOT.jar
```
The REST API starts listening at:
`http://localhost:8080/api`

Test health via terminal:
```powershell
curl http://localhost:8080/api/health
```

---

## 3. Frontend (React + Vite + TypeScript)

### Navigate to Frontend Directory
```powershell
cd frontend
```

### Install Dependencies
```powershell
npm install
```

### Build for Production
```powershell
npm run build
```
*(Produces compiled, optimized assets in `frontend/dist/`)*

### Start Local Development Server
```powershell
npm run dev
```
The Vite development server will start at:
`http://localhost:5173`

---

## 4. Complete Application Workflow

1. Open `http://localhost:5173` in your browser.
2. Verify that **Backend Connected** (green badge) is active in the top-right header.
3. Choose a workload method:
   - **Demo Workload**: Click "✨ Demo Workload" to populate a deterministic 10-task benchmark.
   - **Upload CSV**: Click "📁 Upload CSV" to upload and validate a custom workload file.
   - **Sample Workload**: Click "Sample Workload" to fetch the 5-task standard set.
   - **Manual Entry**: Click "+ Add Task" to customize tasks.
4. Select an algorithm:
   - **IEEE Baseline**
   - **Proposed Deadline-Aware**
   - **Compare Both (Recommended)**
5. Click **"Run Simulation"**.
6. Inspect the comprehensive analytics:
   - **Workload Summary Cards**: Total tasks, target VMs, average priority, average execution time, arrival/deadline span.
   - **Pairwise Comparison Table**: Numerical metrics and exact difference values ($\Delta = \text{Proposed} - \text{Baseline}$).
   - **Metrics Charts**: Multi-metric visual bar charts comparing baseline and proposed values.
   - **Deadline Analysis**: Percentage breakdown of tasks completed strictly before deadline, at deadline, or missed deadline.
   - **VM Utilization Breakdown**: Per-VM busy durations and individual busy percentages for VMs 0, 1, 2, and 3.
   - **Interactive Filters**: Filter displayed results by VM, execution status, priority range, or deadline classification.
   - **VM Gantt Timeline**: Chronological task allocation schedule across VMs with hover tooltips and time intervals.
   - **Task Results Table**: Exact per-task execution timestamps.
   - **Export Results**: Download `tasks-result.csv` or `metrics-summary.csv`.
7. Click **"🔄 Reset"** to return the simulator to its initial state.
