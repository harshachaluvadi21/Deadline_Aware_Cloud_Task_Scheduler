# Deadline-Aware Cloud Task Scheduler

A cloud task scheduling simulation platform that compares a **deadline-aware** scheduling approach against a **standard priority-based** baseline, implemented using CloudSim Plus and a Spring Boot + React web interface.

---

## Architecture

```
React + TypeScript + Vite (port 5173)
           │
           │  REST API  (/api/*)
           ▼
Spring Boot Backend (port 8080)
           │
           ▼
    CloudSim Plus Simulation
           │
           ▼
  Schedulers + Fibonacci Heap
           │
           ▼
  Performance Metrics Output
```

---

## Project Structure

```
Deadline_Aware_Cloud_Task_Scheduler/
├── backend/                   # Spring Boot backend (Java)
│   ├── pom.xml                # Maven build file
│   ├── src/
│   │   ├── main/java/
│   │   │   ├── scheduler/     # Scheduling algorithms
│   │   │   │   ├── baseline/  # Standard Priority Scheduler (FROZEN)
│   │   │   │   ├── proposed/  # Deadline-Aware Scheduler (FROZEN)
│   │   │   │   ├── heap/      # Fibonacci Heap implementation
│   │   │   │   ├── experiment/# Experiment framework (Phases 8–10)
│   │   │   │   ├── analysis/  # Result aggregation
│   │   │   │   └── model/     # Task / VM data models
│   │   │   └── web/           # Spring Boot REST controllers + services
│   │   ├── test/java/         # JUnit tests (135 tests)
│   │   └── main/resources/
│   │       └── demo-workload.csv
│   ├── results/               # Research experiment outputs
│   ├── docs/                  # Technical documentation
│   └── scripts/               # Utility scripts (chart generation)
│
├── frontend/                  # React + TypeScript + Vite frontend
│   ├── package.json
│   ├── vite.config.ts         # Dev proxy → localhost:8080
│   ├── public/
│   │   └── demo-workload.csv  # 10-task demo workload
│   └── src/
│       ├── pages/
│       │   ├── LandingPage.tsx
│       │   ├── SimulatorPage.tsx
│       │   └── AboutPage.tsx
│       ├── components/        # Reusable UI components
│       ├── services/api.ts    # REST API client
│       └── types/simulation.ts
│
├── README.md                  # This file
└── .gitignore
```

---

## Quick Start

### Prerequisites

| Tool | Version |
|------|---------|
| Java | 21+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| npm | 9+ |

---

### 1. Start the Backend

```bash
cd backend
mvn spring-boot:run
```

The backend starts on **http://localhost:8080**

Wait for: `Started MiniProjectApplication` in the console.

---

### 2. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on **http://localhost:5173**

Open your browser at **http://localhost:5173** to use the application.

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/health` | Backend health check |
| `GET`  | `/api/sample-workload` | Fetch a sample task workload |
| `POST` | `/api/simulate` | Run a single scheduler |
| `POST` | `/api/compare` | Run both schedulers and compare |
| `POST` | `/api/workloads/validate` | Validate a CSV workload file |
| `POST` | `/api/export/tasks` | Export task results as CSV |
| `POST` | `/api/export/metrics` | Export metrics summary as CSV |

### Simulate Request Example

```json
POST /api/simulate
{
  "algorithm": "PROPOSED",
  "tasks": [
    { "taskId": 0, "priority": 5, "arrivalTime": 0.0, "executionTime": 15.65, "deadline": 47.31 }
  ]
}
```

`algorithm` values: `BASELINE` | `PROPOSED`

---

## Scheduling Approaches

### Standard Priority Scheduler (Baseline)

Uses an IEEE-inspired priority assignment combined with waiting-time information.

- Task ordering based on computed priority score
- Waiting-time matrix influences scheduling decisions

### Deadline-Aware Scheduler (Proposed)

Extends the baseline with a deadline urgency component:

```
Score = 0.35 × Priority + 0.50 × Deadline Urgency + 0.15 × Waiting Factor
```

> **Note:** The weights (0.35 / 0.50 / 0.15) are project design parameters, not universally validated values.

Both schedulers use a **Fibonacci Heap** as the internal priority queue for efficient task extraction.

---

## Evaluation Metrics

| Metric | Description |
|--------|-------------|
| Makespan | Total time to complete all tasks |
| Average Waiting Time | Average time tasks spend in the queue |
| Average Turnaround Time | Average from arrival to completion |
| Throughput | Tasks completed per time unit |
| Deadline Miss Rate | Percentage of tasks missing their deadline |
| VM Resource Utilization | Fraction of VM capacity used |

---

## CSV Workload Format

Upload or prepare task workloads as CSV:

```csv
taskId,priority,arrivalTime,executionTime,deadline
0,5,0.00,15.65,47.31
1,8,1.00,8.00,12.00
```

| Column | Type | Description |
|--------|------|-------------|
| taskId | integer | Unique task identifier |
| priority | integer (1–10) | Task importance |
| arrivalTime | decimal | Time when task enters the system |
| executionTime | decimal | Required processing time |
| deadline | decimal | Target completion time |

---

## Demo Workflow

1. Open **http://localhost:5173**
2. Click **Launch Simulator**
3. Click **Load Demo** — loads the verified 10-task workload
4. Select **Compare Both**
5. Click **Run Cloud Simulation**
6. Review:
   - Side-by-side metrics comparison
   - Deadline analysis (before / at / missed)
   - VM utilization bars
   - Task execution Gantt timeline
   - Export CSV results

### Verified 10-Task Reference Values

| Metric | Standard Priority | Deadline-Aware |
|--------|:-----------------:|:--------------:|
| Makespan | 32.50 s | 35.50 s |
| Avg. Waiting Time | 4.87 s | 4.28 s |
| Avg. Turnaround Time | 16.33 s | 15.74 s |
| Throughput | 0.3077 | 0.2817 |
| Deadline Miss Rate | 30.0% | 30.0% |
| VM Utilization | 88.2% | 80.7% |

> These are verification reference values only. Do not hard-code them.

---

## Running Tests

### Backend Tests

```bash
cd backend
mvn test
```

Expected: **135 tests, 0 failures, 0 errors, 0 skipped**

### Frontend Type Check + Build

```bash
cd frontend
npm run build
```

Expected: **0 TypeScript errors, build succeeds**

---

## Research Context

This project is a **4th year B.Tech Mini Project** on:

> *Deadline-Aware Priority-Based Task Scheduling in Cloud Computing*

**Experimental campaign:** 24 controlled runs across multiple workload sizes and seeds (Phases 8–10), with full metrics analysis and visualization artifacts in `backend/results/`.

The research core (schedulers, formulas, experiments, results) is **frozen** and must not be modified.

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Simulation | CloudSim Plus 8.0.0 |
| Backend | Spring Boot 3.x, Java 21 |
| Build | Apache Maven 3.8+ |
| Frontend | React 18, TypeScript, Vite 5 |
| Routing | React Router v6 |
| Styling | Vanilla CSS (custom design system) |
| Charts | Recharts |

---

## License

Academic Mini Project — B.Tech, 2026.
