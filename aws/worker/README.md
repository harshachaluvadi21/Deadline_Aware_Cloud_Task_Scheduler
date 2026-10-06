# Cloud Task Scheduler - Real Execution Worker Agent

This module is the **Real Execution Worker Agent** for the *Deadline-Aware Priority-Based Task Scheduling Framework for Cloud Data Centers*.

## Architectural Modes

The scheduling framework supports two complementary execution environments:

1. **Simulation Mode (Default):**
   - Discrete-event mathematical simulation model (CloudSim Plus compatible).
   - In-memory deterministic execution simulating multi-core data center hosts, virtual machines, and cloud workloads.
   - Ideal for large-scale comparative analysis and rapid heuristic benchmarking.

2. **Real Worker Mode:**
   - Real-world CPU-bound computation executed by this FastAPI worker service.
   - The Spring Boot backend's deadline-aware scheduling algorithm calculates task urgency and determines the exact scheduling order.
   - Tasks are dispatched sequentially to this worker, which performs genuine CPU-bound computations (SHA-256 cryptographic hashing) and captures wall-clock execution time and live CPU utilization via `psutil`.
   - Telemetry is fed back to the backend `MetricsCalculator` for real-world turnaround, waiting time, makespan, and deadline-miss metrics.

> **Architectural Boundary:** The worker agent does **NOT** contain scheduling logic, priority recalculation, deadline formulas, or heap algorithms. It is strictly an execution node computing assigned tasks.

---

## Deployment Environments

The Real Worker can run either locally or in the cloud on **Render Free**:

| Environment | Base URL | How to Run |
|-------------|----------|------------|
| **Local** | `http://127.0.0.1:5000` | `uvicorn worker_agent:app --host 127.0.0.1 --port 5000` or `python worker_agent.py` |
| **Cloud (Render Free)** | `https://deadline-cloud-worker.onrender.com` | Managed by Render (`uvicorn worker_agent:app --host 0.0.0.0 --port $PORT`) |

### Backend Configuration

The Spring Boot backend endpoint is dynamically driven by the `CLOUD_WORKER_URL` environment variable:
```properties
cloud.worker.url=${CLOUD_WORKER_URL:http://127.0.0.1:5000}
```
* **For Local Execution:** Keep the default `http://127.0.0.1:5000` (no environment variable required).
* **For Cloud Execution:** Set `CLOUD_WORKER_URL=https://deadline-cloud-worker.onrender.com` in the backend's environment variables.


---

## Deploying to Render Free (Web Service)

### Service Specifications
- **Service Type:** Web Service
- **Environment:** `Python 3`
- **Root Directory:** `aws/worker`
- **Build Command:** `pip install -r requirements.txt`
- **Start Command:** `uvicorn worker_agent:app --host 0.0.0.0 --port $PORT`
- **Instance Type:** `Free`

### Dynamic Port & Host Binding
Render dynamically assigns a port via the `$PORT` environment variable and requires the application to bind to `0.0.0.0`.
- When invoked via the start command:
  ```bash
  uvicorn worker_agent:app --host 0.0.0.0 --port $PORT
  ```
  Uvicorn binds directly to `0.0.0.0:$PORT`.
- When executed directly via Python (`python worker_agent.py`), `worker_agent.py` inspects `os.environ.get("PORT", 5000)` and binds to `0.0.0.0`.

### Step-by-Step Render Dashboard Deployment
1. Log in to [Render Dashboard](https://dashboard.render.com/).
2. Click **New +** and select **Web Service**.
3. Connect your Git repository containing this project.
4. Fill in the service configuration:
   - **Name:** e.g., `cloud-task-scheduler-worker` (or your preferred name)
   - **Region:** Nearest region (e.g., Oregon, Frankfurt, Singapore)
   - **Branch:** `master` (or your active branch)
   - **Root Directory:** `aws/worker`
   - **Runtime:** `Python 3`
   - **Build Command:** `pip install -r requirements.txt`
   - **Start Command:** `uvicorn worker_agent:app --host 0.0.0.0 --port $PORT`
   - **Instance Type:** Select **Free** ($0/month)
5. Under **Environment Variables**, no custom variables are strictly required (Render automatically supplies `$PORT`).
6. Click **Create Web Service**.
7. Wait for the build and deployment logs to display:
   ```text
   Application startup complete.
   Uvicorn running on http://0.0.0.0:<PORT>
   ```
8. Copy the assigned **Render public URL** at the top of the dashboard (e.g. `https://<service-name>.onrender.com`).
9. Verify public health probe in browser or terminal:
   ```bash
   curl https://<service-name>.onrender.com/health
   ```
10. Update your Spring Boot backend configuration to point to Render:
    ```bash
    export CLOUD_WORKER_URL=https://<service-name>.onrender.com
    ```

### Render Free Tier Characteristics & Considerations
- **Inactivity Spin-Down (Cold Starts):** Free instances spin down (sleep) after 15 minutes of inactivity. The first incoming request after spin-down incurs a 30–50 second cold start delay while the container boots. Subsequent requests respond with sub-second latency.
- **Request Timeout:** Render's reverse proxy enforces a 100-second maximum HTTP request timeout. Tasks dispatched to the worker must have `executionTime < 100` seconds (mini-project tasks typically range from 1 to 20 seconds).
- **Compute Resources:** Free tier instances provide 0.1 shared vCPU and 512 MB RAM. The worker's iterative SHA-256 CPU workload consumes < 30 MB memory and yields wall-clock execution accurate to target durations without exceeding quota.
- **Outbound Bandwidth:** 100 GB/month included on Free tier.

---

## 1. Setup Virtual Environment (Optional / Recommended)

```bash
# Navigate to worker directory
cd aws/worker

# Create a virtual environment
python -m venv venv

# Activate on Windows (PowerShell)
.\venv\Scripts\Activate.ps1

# Activate on Linux / macOS
source venv/bin/activate
```

---

## 2. Install Dependencies

```bash
pip install -r requirements.txt
```

---

## 3. Start the Worker Agent

```bash
uvicorn worker_agent:app --host 127.0.0.1 --port 5000
```

or directly via Python:

```bash
python worker_agent.py
```

The worker will start listening on `http://127.0.0.1:5000`.

---

## 4. Test Health Check (`GET /health`)

### Using PowerShell:
```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:5000/health"
```

### Expected Response:
```json
{
  "status": "UP",
  "worker": "cloud-worker",
  "host": "localhost",
  "psutilAvailable": true
}
```

---

## 5. Send a Test Task to `/execute` (`POST /execute`)

### Using PowerShell:
```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:5000/execute" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"taskId": "T1", "executionTime": 2.5, "priority": 8, "deadline": 10.0}'
```

### Expected Response:
```json
{
  "taskId": "T1",
  "status": "COMPLETED",
  "actualExecutionTimeSeconds": 2.5002,
  "cpuUtilization": 99.4,
  "startTime": 1740000000.123,
  "endTime": 1740000002.623
}
```

---

## 6. Workload & CPU Utilization Calculation

* **Real CPU-bound Workload:** The worker does **not** call `time.sleep()`. Instead, it runs an iterative SHA-256 cryptographic hashing loop that drives active CPU cycles for the requested `executionTime` seconds.
* **CPU Utilization:** Computed using both active process CPU time (`time.process_time()` vs `time.perf_counter()`) and `psutil.Process().cpu_percent()`, yielding genuine CPU utilization metrics for the task.
