"""
Real Execution Cloud Worker Agent
Part of: Deadline-Aware Priority-Based Task Scheduling Framework for Cloud Data Centers

Role: Lightweight CPU Execution Agent.
Responsibilities:
  - Exposes GET /health for orchestration probes.
  - Exposes POST /execute to accept scheduled tasks and perform genuine CPU-bound computations.
  - Measures wall-clock execution time, process CPU time, and system CPU utilization.

Architecture Constraint:
  - This worker is STRICTLY an execution node.
  - It does NOT make scheduling, priority, deadline, or resource allocation decisions.
"""

import hashlib
import os
import time
from typing import Any, Union

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

try:
    import psutil
    PSUTIL_AVAILABLE = True
except ImportError:
    PSUTIL_AVAILABLE = False

# Initialize FastAPI application
app = FastAPI(
    title="Cloud Task Scheduler - Real Worker Agent",
    description="Execution worker performing CPU-bound computation and telemetry capture",
    version="1.0.0",
)

# Enable CORS for cross-origin or local testing
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class TaskPayload(BaseModel):
    taskId: Union[str, int] = Field(..., description="Unique task identifier, e.g. 'T1' or 1")
    executionTime: float = Field(..., gt=0, description="Requested compute duration in seconds")
    priority: int = Field(default=5, ge=1, le=10, description="Task priority (1-10)")
    deadline: float = Field(default=0.0, ge=0, description="Task deadline in seconds")


class ExecutionResult(BaseModel):
    taskId: Union[str, int]
    status: str
    actualExecutionTimeSeconds: float
    cpuUtilization: float
    startTime: float
    endTime: float


def perform_cpu_workload(target_duration_seconds: float) -> tuple[float, float, float, float]:
    """
    Executes an active, deterministic CPU-bound workload (iterative SHA-256 hashing)
    until target_duration_seconds of real wall-clock time have elapsed.

    Does NOT use sleep() to guarantee actual CPU execution and measurable core utilization.

    Returns:
        tuple of (actual_wall_duration, cpu_process_duration, start_wall_ts, end_wall_ts)
    """
    start_perf = time.perf_counter()
    start_proc = time.process_time()
    start_wall = time.time()

    # Active CPU workload state
    state = b"cloud-task-scheduler-ec2-worker-workload-entropy-seed"
    chunk_size = 1500

    while (time.perf_counter() - start_perf) < target_duration_seconds:
        # Perform compute-intensive cryptographic hashing in tight chunks
        for _ in range(chunk_size):
            state = hashlib.sha256(state).digest()

    end_perf = time.perf_counter()
    end_proc = time.process_time()
    end_wall = time.time()

    actual_duration = round(end_perf - start_perf, 4)
    proc_duration = end_proc - start_proc

    return actual_duration, proc_duration, start_wall, end_wall


@app.get("/health")
def health():
    """
    Health check probe for Spring Boot backend and AWS CloudWatch / ALB probes.
    """
    return {
        "status": "UP",
        "worker": "cloud-worker",
        "host": os.environ.get("HOSTNAME", "localhost"),
        "psutilAvailable": PSUTIL_AVAILABLE,
    }


@app.post("/execute", response_model=ExecutionResult)
def execute_task(task: TaskPayload):
    """
    Executes a CPU-bound workload corresponding to the scheduled task duration.
    Calculates execution duration and process/system CPU utilization.
    """
    if task.executionTime <= 0:
        raise HTTPException(status_code=400, detail="executionTime must be greater than 0")

    # Prime psutil CPU percent measurement if available
    proc = None
    if PSUTIL_AVAILABLE:
        try:
            proc = psutil.Process()
            proc.cpu_percent(interval=None)
            psutil.cpu_percent(interval=None)
        except Exception:
            pass

    # Execute genuine CPU-bound work
    actual_duration, proc_duration, start_wall, end_wall = perform_cpu_workload(task.executionTime)

    # Calculate CPU utilization:
    # 1. Primary: ratio of active process CPU time to wall-clock time (100% = 1 full core utilized)
    # 2. Refined with psutil if available
    if actual_duration > 0:
        calculated_cpu = (proc_duration / actual_duration) * 100.0
    else:
        calculated_cpu = 100.0

    if PSUTIL_AVAILABLE and proc is not None:
        try:
            sample = proc.cpu_percent(interval=None)
            if sample > 0:
                calculated_cpu = sample
        except Exception:
            pass

    # Ensure bounded representation
    cpu_utilization = round(min(100.0, max(5.0, calculated_cpu)), 2)

    status = "COMPLETED"
    start_time = round(start_wall, 3)
    end_time = round(end_wall, 3)

    # Print real execution telemetry to stdout for cloud visibility (captured by Render logs)
    print(
        "==================================================\n"
        "REAL CLOUD WORKER EXECUTION\n"
        "==================================================\n"
        f"Task ID          : {task.taskId}\n"
        f"Status           : {status}\n"
        f"Execution Time   : {actual_duration} seconds\n"
        f"CPU Utilization  : {cpu_utilization:.2f}%\n"
        f"Start Time       : {start_time}\n"
        f"End Time         : {end_time}\n"
        "==================================================",
        flush=True,
    )

    return ExecutionResult(
        taskId=task.taskId,
        status=status,
        actualExecutionTimeSeconds=actual_duration,
        cpuUtilization=cpu_utilization,
        startTime=start_time,
        endTime=end_time,
    )


if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", 5000))
    uvicorn.run("worker_agent:app", host="0.0.0.0", port=port, reload=False)

