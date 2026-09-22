# Workload CSV Specification & Guide

This guide details the CSV workload format, validation constraints, upload workflow, and simulation export formats for the **Deadline-Aware Priority-Based Task Scheduling** platform.

---

## 1. Workload CSV Schema

A workload CSV defines a batch of computational tasks to be scheduled across the simulated virtual machine cluster.

### Header Line (Required)
```csv
taskId,priority,arrivalTime,executionTime,deadline
```
*Note: Header matching is case-insensitive and trims extraneous whitespace.*

### Column Definitions

| Column Name | Data Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `taskId` | Integer / Long | $\ge 0$, Unique | Unique identifier for the task. Duplicate IDs cause the entire CSV to be rejected. |
| `priority` | Integer | $1 \le p \le 10$ | Base task priority level, where 10 indicates the highest priority. |
| `arrivalTime` | Decimal / Float | $\ge 0.0$ | Time instance (in seconds) when the task arrives at the cloud broker. |
| `executionTime` | Decimal / Float | $> 0.0$ | Duration (in seconds) required to compute the task on a reference VM (1000 MIPS). |
| `deadline` | Decimal / Float | $\ge \text{arrivalTime}$ | Absolute completion deadline (in seconds). Must not be earlier than arrival time. |

---

## 2. Validation Rules & Rejection Policy

To guarantee academic and simulation reproducibility, the platform adheres to an **All-or-Nothing** validation policy. If any row contains an error, the **entire workload is rejected** with HTTP 400 Bad Request, returning a structured list of exact validation errors.

### Detected Errors:
1. **Empty File / Header Missing**: Rejects files with 0 data rows or invalid headers.
2. **Column Count Mismatches**: Each row must have exactly 5 comma-delimited columns.
3. **Invalid Numeric Formats**: Parsing failures for non-numeric characters (e.g., `"abc"` for `taskId`).
4. **Duplicate Task IDs**: Each task must have a distinct ID.
5. **Out-of-Range Priority**: Priority must strictly be in $[1, 10]$.
6. **Negative Arrival or Execution Time**: Tasks cannot arrive before $t=0.0$, and execution time must be strictly positive.
7. **Infeasible Deadline Formulation**: `deadline < arrivalTime` violates causal task arrival semantics and is rejected.

---

## 3. Example Workload CSV

```csv
taskId,priority,arrivalTime,executionTime,deadline
0,5,0.00,15.65,47.31
1,8,1.00,8.00,12.00
2,3,4.00,12.50,40.00
3,9,5.50,6.00,14.50
4,2,7.00,18.00,60.00
```

---

## 4. API Endpoints

### 4.1 Validate & Parse Workload
- **Endpoint**: `POST /api/workloads/validate`
- **Content-Type**: `multipart/form-data`
- **Parameter**: `file` (Multipart file `.csv`)
- **Success Response (HTTP 200)**:
  ```json
  {
    "valid": true,
    "taskCount": 5,
    "tasks": [
      { "taskId": 0, "priority": 5, "arrivalTime": 0.0, "executionTime": 15.65, "deadline": 47.31 },
      ...
    ],
    "errors": []
  }
  ```
- **Error Response (HTTP 400)**:
  ```json
  {
    "valid": false,
    "taskCount": 0,
    "tasks": [],
    "errors": [
      "Line 3: Priority (12) must be an integer between 1 and 10.",
      "Line 4: Duplicate Task ID detected (0)."
    ]
  }
  ```

---

## 5. Result Export Format

### 5.1 Task Results Export (`POST /api/export/tasks`)
Generates a downloadable CSV attachment (`tasks-result.csv`) containing per-task execution timestamps and assignment:
```csv
taskId,priority,arrivalTime,executionTime,deadline,vmId,startTime,completionTime,status
0,5,0.00,15.65,47.31,0,0.00,15.65,SUCCESS
1,8,1.00,8.00,12.00,1,1.00,9.00,SUCCESS
2,3,4.00,12.50,40.00,2,4.00,16.50,SUCCESS
```

### 5.2 Metrics Summary Export (`POST /api/export/metrics`)
Generates a downloadable CSV attachment (`metrics-summary.csv`).

#### Single Algorithm Format:
```csv
algorithm,makespan,averageWaitingTime,averageTurnaroundTime,throughput,deadlineMissRate,resourceUtilization
BASELINE,65.40,12.30,24.80,0.3058,0.00,75.40
```

#### Comparison Format:
```csv
metric,baseline,proposed,difference
Makespan (s),65.40,61.20,-4.20
Average Waiting Time (s),12.30,10.10,-2.20
Average Turnaround Time (s),24.80,22.60,-2.20
Throughput (tasks/s),0.3058,0.3268,0.0210
Deadline Miss Rate (%),5.00,0.00,-5.00
VM Resource Utilization (%),75.40,78.20,2.80
```
*Note: Differences are strictly calculated as $\text{Proposed} - \text{Baseline}$.*
