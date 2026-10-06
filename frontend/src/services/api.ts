import {
  CompareResponse,
  ExecutionTarget,
  ExportMetricsRequest,
  SimulateResponse,
  TaskDto,
  TaskResultDto,
  WorkerHealthResponse,
  WorkloadValidationResponse
} from '../types/simulation';
function resolveApiBase(): string {
  const envUrl = import.meta.env.VITE_API_BASE_URL;
  if (!envUrl || typeof envUrl !== 'string' || !envUrl.trim()) {
    return '/api';
  }
  const clean = envUrl.trim().replace(/\/+$/, '');
  return clean.endsWith('/api') ? clean : `${clean}/api`;
}

const API_BASE = resolveApiBase();

export class ApiError extends Error {
  messages: string[];
  constructor(message: string, messages: string[] = []) {
    super(message);
    this.name = 'ApiError';
    this.messages = messages;
  }
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let errorMsg = `Server responded with ${response.status} ${response.statusText}`;
    let errorDetails: string[] = [];
    try {
      const errorJson = await response.json();
      if (errorJson.error) {
        errorMsg = errorJson.error;
      }
      if (Array.isArray(errorJson.messages)) {
        errorDetails = errorJson.messages;
      } else if (Array.isArray(errorJson.errors)) {
        errorDetails = errorJson.errors;
      }
    } catch {
      // Non-JSON response
    }
    throw new ApiError(errorMsg, errorDetails);
  }
  return response.json();
}

function triggerDownload(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.style.display = 'none';
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
}

export const simulationApi = {
  async healthCheck(): Promise<boolean> {
    try {
      const response = await fetch(`${API_BASE}/health`);
      if (!response.ok) return false;
      const data = await response.json();
      return data.status === 'UP';
    } catch {
      return false;
    }
  },

  async checkWorkerHealth(): Promise<WorkerHealthResponse> {
    try {
      const response = await fetch(`${API_BASE}/worker/health`);
      if (!response.ok) {
        return { status: 'DOWN', reachable: false };
      }
      const data = await response.json();
      return {
        status: data.status || 'DOWN',
        worker: data.worker,
        host: data.host,
        psutilAvailable: data.psutilAvailable,
        reachable: data.reachable ?? (data.status === 'UP')
      };
    } catch {
      return { status: 'DOWN', reachable: false };
    }
  },


  async getSampleWorkload(): Promise<TaskDto[]> {
    const response = await fetch(`${API_BASE}/sample-workload`);
    return handleResponse<TaskDto[]>(response);
  },

  async runSimulation(
    algorithm: 'BASELINE' | 'PROPOSED',
    tasks: TaskDto[],
    executionTarget: ExecutionTarget = 'SIMULATION'
  ): Promise<SimulateResponse> {
    const response = await fetch(`${API_BASE}/simulate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ algorithm, tasks, executionTarget })
    });
    return handleResponse<SimulateResponse>(response);
  },

  async compareSchedulers(
    tasks: TaskDto[],
    executionTarget: ExecutionTarget = 'SIMULATION'
  ): Promise<CompareResponse> {
    const response = await fetch(`${API_BASE}/compare`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ tasks, executionTarget })
    });
    return handleResponse<CompareResponse>(response);
  },


  async validateWorkloadCsv(file: File): Promise<WorkloadValidationResponse> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(`${API_BASE}/workloads/validate`, {
      method: 'POST',
      body: formData
    });

    if (!response.ok) {
      let errorJson: WorkloadValidationResponse | null = null;
      try {
        errorJson = await response.json();
      } catch {
        // ignore
      }

      if (errorJson && Array.isArray(errorJson.errors)) {
        throw new ApiError('CSV Workload Validation Failed', errorJson.errors);
      }
      throw new ApiError(`Validation request failed with HTTP ${response.status}`);
    }

    return response.json();
  },

  async exportTaskResults(tasks: TaskResultDto[]): Promise<void> {
    const response = await fetch(`${API_BASE}/export/tasks`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(tasks)
    });

    if (!response.ok) {
      throw new ApiError('Failed to export task results CSV');
    }

    const blob = await response.blob();
    triggerDownload(blob, 'tasks-result.csv');
  },

  async exportMetrics(request: ExportMetricsRequest): Promise<void> {
    const response = await fetch(`${API_BASE}/export/metrics`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request)
    });

    if (!response.ok) {
      throw new ApiError('Failed to export metrics CSV');
    }

    const blob = await response.blob();
    triggerDownload(blob, 'metrics-summary.csv');
  }
};
