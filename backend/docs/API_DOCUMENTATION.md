# REST API Documentation

Base URL: `http://localhost:8080/api`

---

## 1. Endpoints Overview

| Method | Path | Request Format | Response Format | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | None | JSON | Backend health probe |
| `GET` | `/api/sample-workload` | None | JSON Array | Deterministic 5-task sample workload |
| `POST` | `/api/simulate` | JSON (`SimulateRequest`) | JSON (`SimulateResponse`) | Execute single scheduler simulation |
| `POST` | `/api/compare` | JSON (`CompareRequest`) | JSON (`CompareResponse`) | Controlled pairwise comparison |
| `POST` | `/api/workloads/validate` | Multipart Form (`file`) | JSON (`WorkloadValidationResponse`) | Validate & parse workload CSV |
| `POST` | `/api/export/tasks` | JSON (`List<TaskResultDto>`) | CSV (`tasks-result.csv`) | Export task results as RFC 4180 CSV |
| `POST` | `/api/export/metrics` | JSON (`ExportMetricsRequest`) | CSV (`metrics-summary.csv`) | Export metrics & comparison as CSV |

---

## 2. Endpoint Details

### 2.1 Health Check
- **Method:** `GET`
- **Path:** `/api/health`
- **Description:** Verifies that the Spring Boot backend is active and responsive.
- **Request:** None
- **Response (200 OK):**
  ```json
  {
    "status": "UP"
  }
  ```

---

### 2.2 Sample Workload
- **Method:** `GET`
- **Path:** `/api/sample-workload`
- **Description:** Returns a fixed, deterministic sample task set (5 tasks) for immediate testing and UI demonstration.
- **Request:** None
- **Response (200 OK):**
  ```json
  [
    { "taskId": 0, "priority": 5, "arrivalTime": 0.0, "executionTime": 15.65, "deadline": 47.31 },
    { "taskId": 1, "priority": 4, "arrivalTime": 1.99, "executionTime": 10.56, "deadline": 30.90 },
    { "taskId": 2, "priority": 6, "arrivalTime": 2.85, "executionTime": 12.58, "deadline": 56.86 },
    { "taskId": 3, "priority": 5, "arrivalTime": 4.68, "executionTime": 10.28, "deadline": 40.74 },
    { "taskId": 4, "priority": 3, "arrivalTime": 6.42, "executionTime": 7.65, "deadline": 30.40 }
  ]
  ```

---

### 2.3 Simulate Workload
- **Method:** `POST`
- **Path:** `/api/simulate`
- **Description:** Runs a single scheduling simulation using either the IEEE Baseline or Proposed Deadline-Aware algorithm.
- **Request Body (JSON):**
  ```json
  {
    "algorithm": "BASELINE",
    "tasks": [
      { "taskId": 0, "priority": 5, "arrivalTime": 0.0, "executionTime": 15.65, "deadline": 47.31 }
    ]
  }
  ```
  Supported `algorithm` values:
  - `BASELINE` (IEEE Access 2023 priority heuristic)
  - `PROPOSED` (Deadline-aware dynamic priority scheduler)

- **Response (200 OK):**
  ```json
  {
    "algorithm": "BASELINE",
    "tasks": [
      {
        "taskId": 0,
        "priority": 5,
        "arrivalTime": 0.0,
        "executionTime": 15.65,
        "deadline": 47.31,
        "assignedVmId": 0,
        "startTime": 0.0,
        "completionTime": 15.65,
        "waitingTime": 0.0,
        "turnaroundTime": 15.65,
        "status": "COMPLETED",
        "deadlineMissed": false
      }
    ],
    "metrics": {
      "makespan": 15.65,
      "averageWaitingTime": 0.0,
      "averageTurnaroundTime": 15.65,
      "throughput": 0.0639,
      "deadlineMissRate": 0.0,
      "deadlineMissedCount": 0,
      "completedTaskCount": 1,
      "resourceUtilization": 25.0
    }
  }
  ```

---

### 2.4 Compare Schedulers
- **Method:** `POST`
- **Path:** `/api/compare`
- **Description:** Executes both Baseline and Proposed schedulers on identical clones of the provided workload and computes pairwise differences ($\text{Proposed} - \text{Baseline}$).
- **Request Body (JSON):**
  ```json
  {
    "tasks": [
      { "taskId": 0, "priority": 5, "arrivalTime": 0.0, "executionTime": 15.65, "deadline": 47.31 }
    ]
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "baseline": {
      "algorithm": "BASELINE",
      "tasks": [ ... ],
      "metrics": { ... }
    },
    "proposed": {
      "algorithm": "PROPOSED",
      "tasks": [ ... ],
      "metrics": { ... }
    },
    "comparison": {
      "makespanDifference": -1.25,
      "waitingTimeDifference": -0.80,
      "turnaroundDifference": -0.80,
      "throughputDifference": 0.0120,
      "deadlineMissRateDifference": -5.00,
      "resourceUtilizationDifference": 1.50
    }
  }
  ```

---

### 2.5 Validate CSV Workload
- **Method:** `POST`
- **Path:** `/api/workloads/validate`
- **Content-Type:** `multipart/form-data`
- **Description:** Uploads a CSV file for validation and parsing according to cloud task constraints.
- **Parameters:**
  - `file`: Multipart `.csv` file.
- **Success Response (200 OK):**
  ```json
  {
    "valid": true,
    "taskCount": 3,
    "tasks": [
      { "taskId": 0, "priority": 5, "arrivalTime": 0.0, "executionTime": 15.65, "deadline": 47.31 },
      { "taskId": 1, "priority": 8, "arrivalTime": 1.0, "executionTime": 8.0, "deadline": 12.0 },
      { "taskId": 2, "priority": 3, "arrivalTime": 4.0, "executionTime": 12.5, "deadline": 40.0 }
    ],
    "errors": []
  }
  ```
- **Error Response (400 Bad Request):**
  ```json
  {
    "valid": false,
    "taskCount": 0,
    "tasks": [],
    "errors": [
      "Line 2: Priority (15) must be an integer between 1 and 10.",
      "Line 3: Duplicate Task ID detected (0)."
    ]
  }
  ```

---

### 2.6 Export Task Results CSV
- **Method:** `POST`
- **Path:** `/api/export/tasks`
- **Content-Type:** `application/json`
- **Produces:** `text/csv` (Attachment header: `filename="tasks-result.csv"`)
- **Request Body:** Array of `TaskResultDto` objects.
- **Response:** CSV formatted stream:
  ```csv
  taskId,priority,arrivalTime,executionTime,deadline,vmId,startTime,completionTime,status
  0,5,0.00,15.65,47.31,0,0.00,15.65,SUCCESS
  1,8,1.00,8.00,12.00,1,1.00,9.00,SUCCESS
  ```

---

### 2.7 Export Metrics CSV
- **Method:** `POST`
- **Path:** `/api/export/metrics`
- **Content-Type:** `application/json`
- **Produces:** `text/csv` (Attachment header: `filename="metrics-summary.csv"`)
- **Request Body (`ExportMetricsRequest`):**
  ```json
  {
    "baseline": { ... },
    "proposed": { ... }
  }
  ```
  *(Or `{ "algorithm": "BASELINE", "metrics": { ... } }` for single algorithm run)*
- **Response (Comparison Mode):**
  ```csv
  metric,baseline,proposed,difference
  Makespan (s),65.40,61.20,-4.20
  Average Waiting Time (s),12.30,10.10,-2.20
  Average Turnaround Time (s),24.80,22.60,-2.20
  Throughput (tasks/s),0.3058,0.3268,0.0210
  Deadline Miss Rate (%),5.00,0.00,-5.00
  VM Resource Utilization (%),75.40,78.20,2.80
  ```

---

## 3. Validation & Error Handling

When request validation fails, the API responds with `HTTP 400 Bad Request`:
```json
{
  "error": "Validation Failed",
  "messages": [
    "Priority must be at least 1",
    "Execution time must be strictly positive (> 0)",
    "Deadline cannot be earlier than arrival time"
  ]
}
```

### Validation Constraints Enforced:
- `taskId >= 0` (unique across task set)
- `1 <= priority <= 10`
- `arrivalTime >= 0.0`
- `executionTime > 0.0`
- `deadline >= arrivalTime`
- Workload cannot be empty (minimum 1 task)
- Algorithm must be either `BASELINE` or `PROPOSED` (case-insensitive)
