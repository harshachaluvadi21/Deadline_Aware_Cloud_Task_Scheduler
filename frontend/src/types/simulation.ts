export interface TaskDto {
  taskId: number;
  priority: number;
  arrivalTime: number;
  executionTime: number;
  deadline: number;
}

export interface TaskResultDto {
  taskId: number;
  priority: number;
  arrivalTime: number;
  executionTime: number;
  deadline: number;
  assignedVmId: number;
  startTime: number;
  completionTime: number;
  waitingTime: number;
  turnaroundTime: number;
  status: string;
  deadlineMissed: boolean;
}

export interface MetricsDto {
  makespan: number;
  averageWaitingTime: number;
  averageTurnaroundTime: number;
  throughput: number;
  deadlineMissRate: number;
  deadlineMissedCount: number;
  completedTaskCount: number;
  resourceUtilization: number;
}

export type ExecutionTarget = 'SIMULATION' | 'REAL_WORKER';

export interface WorkerHealthResponse {
  status: string;
  worker?: string;
  host?: string;
  psutilAvailable?: boolean;
  reachable: boolean;
}

export interface SimulateResponse {
  algorithm: string;
  tasks: TaskResultDto[];
  metrics: MetricsDto;
  executionTarget?: ExecutionTarget;
}


export interface PairwiseComparisonDto {
  makespanDifference: number;
  waitingTimeDifference: number;
  turnaroundDifference: number;
  throughputDifference: number;
  deadlineMissRateDifference: number;
  resourceUtilizationDifference: number;
}

export interface CompareResponse {
  baseline: SimulateResponse;
  proposed: SimulateResponse;
  comparison: PairwiseComparisonDto;
}

export type AlgorithmMode = 'BASELINE' | 'PROPOSED' | 'COMPARE';

export interface WorkloadValidationResponse {
  valid: boolean;
  taskCount: number;
  tasks: TaskDto[];
  errors: string[];
}

export interface ExportMetricsRequest {
  algorithm?: string;
  metrics?: MetricsDto;
  baseline?: SimulateResponse;
  proposed?: SimulateResponse;
}

export interface WorkloadSummary {
  totalTasks: number;
  vmCount: number;
  averagePriority: number;
  averageExecutionTime: number;
  earliestArrival: number;
  latestDeadline: number;
}

export interface VmUtilizationStats {
  vmId: number;
  taskCount: number;
  busyTime: number;
  utilizationPercent: number;
}
