# Web Application Architecture

## 1. System Overview & Target Architecture

The web application provides an interactive presentation and simulation layer around the existing Java scheduling core without modifying or replacing any research algorithms.

```
+-------------------------------------------------------------------------+
|                          React + Vite Frontend                          |
|  - Dashboard Page (10 Analytical Sections)                              |
|  - Workload Input Table (Manual / Sample / Demo / CSV)                  |
|  - Workload Summary Cards & Algorithm Mode Selector                     |
|  - Metrics Cards & Multi-Metric Visual Comparative Charts               |
|  - Deadline Adherence Analysis & Per-VM Resource Utilization Breakdown  |
|  - Interactive Result Filters (VM, Status, Priority, Deadline Category) |
|  - Gantt-Style Interactive VM Timeline (Real Timestamps, Tooltips)      |
|  - Scheduled Task Results Table & Comparative Metrics Matrix            |
|  - Export Module (Tasks CSV, Metrics CSV)                               |
|  - API Service Layer (frontend/src/services/api.ts)                     |
+------------------------------------+------------------------------------+
                                     | HTTP REST (JSON & Multipart)
                                     | Port 8080 (Backend) / 5173 (Dev UI)
+------------------------------------v------------------------------------+
|                       Spring Boot REST Backend                          |
|  - SimulationController (/api/health, /api/simulate, /api/compare)      |
|  - WorkloadController (/api/workloads/validate, /api/export/*)          |
|  - SimulationService (orchestrates task cloning & execution)            |
|  - WorkloadCsvService (CSV parsing, validation, & RFC 4180 export)     |
|  - DTO Models & Input Validation (@Valid, Bean Validation)              |
|  - WebCorsConfig (Allows dev origins http://localhost:5173)             |
+------------------------------------+------------------------------------+
                                     | In-Memory Java Invocations
+------------------------------------v------------------------------------+
|                  Existing Java Scheduling Engine (FROZEN)               |
|  - BaselinePriorityScheduler (IEEE 2023 PAT, WTM, Fibonacci Heap)       |
|  - ProposedPriorityScheduler (Dynamic Deadline-Aware Score, Heap Rebuild)|
|  - MetricsCalculator (Time-based VM Resource Utilization, Makespan, DMR)|
|  - CloudVmSpec & Task Data Model                                        |
+------------------------------------+------------------------------------+
                                     | Simulated Execution
+------------------------------------v------------------------------------+
|                        CloudSim Plus 8.0.0 Engine                       |
+-------------------------------------------------------------------------+
```

---

## 2. Component Separation & Boundaries

### 2.1 Research Experiment Layer (Strictly Frozen & Preserved)
The existing experiment campaign classes and output directories:
- `scheduler.baseline.*` (Phase 6 Baseline)
- `scheduler.proposed.*` (Phase 7 Proposed)
- `scheduler.experiment.*` (Phase 8 Framework & Phase 9 Campaign)
- `results/` (Datasets, manifests, analysis, and charts from Phases 8–11)
remain **strictly frozen**. The web application does **not** write to `results/`, re-seed pseudo-random generators, or mutate research datasets.

### 2.2 Interactive Web Service Layer (`web.*`)
- Package: `web.controller`, `web.service`, `web.dto`, `web.config`.
- Executes simulation requests dynamically in-memory.
- Translates API DTOs into domain `Task` models using `new Task(id, priority, arrival, execution, deadline)`.
- Replays tasks through `BaselinePriorityScheduler` and `ProposedPriorityScheduler` with independent VM environments.
- Computes standard metrics through `MetricsCalculator`.
- Provides CSV ingestion with all-or-nothing validation rules and result export generation.

---

## 3. REST API Specification

| HTTP Method | Endpoint | Request Body | Response Body | Description |
|---|---|---|---|---|
| `GET` | `/api/health` | None | `{"status": "UP"}` | Service health probe |
| `GET` | `/api/sample-workload` | None | `[{"taskId":0,"priority":5,...}]` | Fixed deterministic 5-task sample workload |
| `POST` | `/api/simulate` | `{"algorithm":"BASELINE"\|"PROPOSED", "tasks":[...]}` | `{"algorithm":..., "tasks":[...], "metrics":{...}}` | Single-scheduler simulation |
| `POST` | `/api/compare` | `{"tasks":[...]}` | `{"baseline":{...}, "proposed":{...}, "comparison":{...}}` | Controlled dual-scheduler comparison |
| `POST` | `/api/workloads/validate` | `MultipartFile file` | `{"valid":true, "taskCount":N, "tasks":[...], "errors":[]}` | Multipart CSV validation and parsing |
| `POST` | `/api/export/tasks` | `List<TaskResultDto>` | `text/csv` attachment | Download task execution results CSV |
| `POST` | `/api/export/metrics` | `ExportMetricsRequest` | `text/csv` attachment | Download single or comparison metrics CSV |

---

## 4. Request / Response Lifecycle

1. **User Interaction:** The user adds, edits, or uploads workload tasks in the React UI and selects an execution mode (`BASELINE`, `PROPOSED`, or `COMPARE`).
2. **API Dispatch:** `frontend/src/services/api.ts` validates payload readiness and issues HTTP requests to `http://localhost:8080/api/...`.
3. **Backend Validation:** Spring Boot interceptors validate field constraints:
   - `taskId >= 0` (and unique across the task set)
   - `1 <= priority <= 10`
   - `arrivalTime >= 0.0`
   - `executionTime > 0.0`
   - `deadline >= arrivalTime`
   - Algorithm in `BASELINE`, `PROPOSED`.
   Any violation results in HTTP 400 Bad Request with descriptive JSON error feedback.
4. **Service Execution:**
   - Tasks are mapped to domain `Task` objects.
   - For `/api/compare`, two deep copies are created via `WorkloadGenerator.cloneWorkload` to guarantee identical inputs.
   - `BaselinePriorityScheduler` and `ProposedPriorityScheduler` execute using standard 4-VM configurations (1000 MIPS).
   - Metrics are computed via `MetricsCalculator`.
   - Pairwise difference is calculated as:
     $$\Delta = \text{Proposed} - \text{Baseline}$$
5. **Frontend Rendering & Visualizations:**
   - **Workload Summary Cards:** Display high-level descriptive properties of the workload.
   - **Metrics Cards:** Display Makespan, Waiting Time, Turnaround Time, Throughput, DMR, and Resource Utilization.
   - **Metric Visualizations:** Horizontal comparative bars for each metric.
   - **Deadline Adherence Analysis:** Breakdown of tasks completed strictly before deadline, at deadline, or missed deadline.
   - **VM Resource Utilization:** Per-VM busy duration, busy percentage, and cluster utilization.
   - **VM Gantt Timeline:** Horizontally maps task blocks ($[\text{startTime}, \text{completionTime}]$) onto VM rows with hover tooltips and time axes.
   - **Interactive Filters:** Filter results by VM, status, priority, or deadline category without mutating underlying metrics.
   - **Comparison Table:** Neutral side-by-side view with numerical differences.
   - **Export Results:** Direct download of research-compliant CSV records.
