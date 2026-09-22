import React, { useEffect, useState, useMemo } from 'react';
import { Header } from '../components/Header';
import { TaskInputTable } from '../components/TaskInputTable';
import { AlgorithmSelector } from '../components/AlgorithmSelector';
import { MetricsCards } from '../components/MetricsCards';
import { VmTimeline } from '../components/VmTimeline';
import { TaskResultsTable } from '../components/TaskResultsTable';
import { ComparisonTable } from '../components/ComparisonTable';
import { WorkloadSummaryCards } from '../components/WorkloadSummaryCards';
import { MetricsCharts } from '../components/MetricsCharts';
import { DeadlineAnalysisView, classifyTaskDeadline } from '../components/DeadlineAnalysisView';
import { VmUtilizationCards } from '../components/VmUtilizationCards';
import { TaskFilters, FilterCriteria } from '../components/TaskFilters';
import { CsvUploadModal } from '../components/CsvUploadModal';
import { ExportButtons } from '../components/ExportButtons';
import { SimulationOverview } from '../components/SimulationOverview';
import { TechnicalDetails } from '../components/TechnicalDetails';
import { ApiError, simulationApi } from '../services/api';
import {
  AlgorithmMode,
  CompareResponse,
  ExportMetricsRequest,
  SimulateResponse,
  TaskDto,
  TaskResultDto
} from '../types/simulation';

const DEFAULT_FILTERS: FilterCriteria = {
  vmId: 'ALL',
  status: 'ALL',
  minPriority: 1,
  maxPriority: 10,
  deadlineCategory: 'ALL'
};

export const Dashboard: React.FC = () => {
  const [backendConnected, setBackendConnected] = useState<boolean | null>(null);
  const [tasks, setTasks] = useState<TaskDto[]>([]);
  const [selectedAlgorithm, setSelectedAlgorithm] = useState<AlgorithmMode>('COMPARE');
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [simulationSuccess, setSimulationSuccess] = useState<boolean>(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [errorDetails, setErrorDetails] = useState<string[]>([]);
  
  const [singleResult, setSingleResult] = useState<SimulateResponse | null>(null);
  const [compareResult, setCompareResult] = useState<CompareResponse | null>(null);

  const [isCsvModalOpen, setIsCsvModalOpen] = useState<boolean>(false);
  const [filters, setFilters] = useState<FilterCriteria>(DEFAULT_FILTERS);

  // Check health on mount and load sample workload
  useEffect(() => {
    const checkConnection = async () => {
      const isUp = await simulationApi.healthCheck();
      setBackendConnected(isUp);
      if (isUp) {
        try {
          const sample = await simulationApi.getSampleWorkload();
          setTasks(sample);
        } catch {
          // Fallback to static sample if backend route not reached
        }
      }
    };
    checkConnection();
  }, []);

  const handleLoadSample = async () => {
    setErrorMsg(null);
    setErrorDetails([]);
    setSimulationSuccess(false);
    try {
      const sample = await simulationApi.getSampleWorkload();
      setTasks(sample);
    } catch {
      // Fallback deterministic sample
      setTasks([
        { taskId: 0, priority: 5, arrivalTime: 0.00, executionTime: 15.65, deadline: 47.31 },
        { taskId: 1, priority: 4, arrivalTime: 1.99, executionTime: 10.56, deadline: 30.90 },
        { taskId: 2, priority: 6, arrivalTime: 2.85, executionTime: 12.58, deadline: 56.86 },
        { taskId: 3, priority: 5, arrivalTime: 4.68, executionTime: 10.28, deadline: 40.74 },
        { taskId: 4, priority: 3, arrivalTime: 6.42, executionTime: 7.65, deadline: 30.40 }
      ]);
    }
  };

  const handleLoadDemo = async () => {
    setErrorMsg(null);
    setErrorDetails([]);
    setSimulationSuccess(false);
    try {
      const response = await fetch('/demo-workload.csv');
      if (response.ok) {
        const text = await response.text();
        const lines = text.trim().split('\n');
        const demoTasks: TaskDto[] = [];
        for (let i = 1; i < lines.length; i++) {
          const parts = lines[i].split(',');
          if (parts.length === 5) {
            demoTasks.push({
              taskId: parseInt(parts[0].trim()),
              priority: parseInt(parts[1].trim()),
              arrivalTime: parseFloat(parts[2].trim()),
              executionTime: parseFloat(parts[3].trim()),
              deadline: parseFloat(parts[4].trim())
            });
          }
        }
        if (demoTasks.length > 0) {
          setTasks(demoTasks);
          return;
        }
      }
    } catch {
      // ignore and use fallback
    }

    // Deterministic 10-task demo workload fallback
    setTasks([
      { taskId: 0, priority: 5, arrivalTime: 0.00, executionTime: 15.65, deadline: 47.31 },
      { taskId: 1, priority: 8, arrivalTime: 1.00, executionTime: 8.00, deadline: 12.00 },
      { taskId: 2, priority: 3, arrivalTime: 2.50, executionTime: 14.00, deadline: 32.00 },
      { taskId: 3, priority: 9, arrivalTime: 3.00, executionTime: 6.50, deadline: 11.00 },
      { taskId: 4, priority: 4, arrivalTime: 4.50, executionTime: 18.00, deadline: 55.00 },
      { taskId: 5, priority: 7, arrivalTime: 6.00, executionTime: 10.00, deadline: 20.00 },
      { taskId: 6, priority: 2, arrivalTime: 7.50, executionTime: 12.00, deadline: 45.00 },
      { taskId: 7, priority: 6, arrivalTime: 8.00, executionTime: 9.50, deadline: 22.00 },
      { taskId: 8, priority: 10, arrivalTime: 10.00, executionTime: 5.00, deadline: 16.00 },
      { taskId: 9, priority: 4, arrivalTime: 12.00, executionTime: 16.00, deadline: 35.00 }
    ]);
  };

  const handleReset = () => {
    setTasks([]);
    setSingleResult(null);
    setCompareResult(null);
    setErrorMsg(null);
    setErrorDetails([]);
    setSimulationSuccess(false);
    setFilters(DEFAULT_FILTERS);
  };

  const handleRunSimulation = async () => {
    if (tasks.length === 0) {
      setErrorMsg('Cannot run simulation with an empty workload. Add tasks, load demo workload, or upload a CSV.');
      return;
    }

    setIsLoading(true);
    setErrorMsg(null);
    setErrorDetails([]);
    setSimulationSuccess(false);
    setSingleResult(null);
    setCompareResult(null);

    try {
      if (selectedAlgorithm === 'COMPARE') {
        const result = await simulationApi.compareSchedulers(tasks);
        setCompareResult(result);
        setSimulationSuccess(true);
      } else {
        const result = await simulationApi.runSimulation(selectedAlgorithm, tasks);
        setSingleResult(result);
        setSimulationSuccess(true);
      }
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setErrorMsg(err.message);
        setErrorDetails(err.messages);
      } else if (err instanceof Error) {
        setErrorMsg(err.message);
      } else {
        setErrorMsg('An unexpected simulation error occurred.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  // Helper to filter tasks based on current criteria
  const filterTaskList = (list: TaskResultDto[]): TaskResultDto[] => {
    return list.filter((t) => {
      // VM filter
      if (filters.vmId !== 'ALL' && t.assignedVmId.toString() !== filters.vmId) {
        return false;
      }
      // Status filter
      if (filters.status !== 'ALL') {
        if (filters.status === 'SUCCESS' && (t.deadlineMissed || t.status === 'MISSED_DEADLINE')) return false;
        if (filters.status === 'MISSED_DEADLINE' && !t.deadlineMissed && t.status !== 'MISSED_DEADLINE') return false;
      }
      // Priority filter
      if (t.priority < filters.minPriority || t.priority > filters.maxPriority) {
        return false;
      }
      // Deadline category filter
      if (filters.deadlineCategory !== 'ALL') {
        const cat = classifyTaskDeadline(t.completionTime, t.deadline, t.deadlineMissed, t.status);
        if (cat.category !== filters.deadlineCategory) {
          return false;
        }
      }
      return true;
    });
  };

  // Available VM IDs for filters
  const availableVmIds = useMemo(() => {
    const ids = new Set<number>([0, 1, 2, 3]);
    if (compareResult) {
      compareResult.baseline.tasks.forEach(t => ids.add(t.assignedVmId));
      compareResult.proposed.tasks.forEach(t => ids.add(t.assignedVmId));
    } else if (singleResult) {
      singleResult.tasks.forEach(t => ids.add(t.assignedVmId));
    }
    return Array.from(ids).sort((a, b) => a - b);
  }, [compareResult, singleResult]);

  // Filtered task results
  const filteredBaselineTasks = useMemo(() => {
    return compareResult ? filterTaskList(compareResult.baseline.tasks) : [];
  }, [compareResult, filters]);

  const filteredProposedTasks = useMemo(() => {
    return compareResult ? filterTaskList(compareResult.proposed.tasks) : [];
  }, [compareResult, filters]);

  const filteredSingleTasks = useMemo(() => {
    return singleResult ? filterTaskList(singleResult.tasks) : [];
  }, [singleResult, filters]);

  // Export metrics payload
  const exportMetricsPayload: ExportMetricsRequest = useMemo(() => {
    if (compareResult) {
      return {
        baseline: compareResult.baseline,
        proposed: compareResult.proposed
      };
    } else if (singleResult) {
      return {
        algorithm: singleResult.algorithm,
        metrics: singleResult.metrics
      };
    }
    return {};
  }, [compareResult, singleResult]);

  return (
    <div className="app-container">
      <Header backendConnected={backendConnected} isRunning={isLoading} />

      {errorMsg && (
        <div className="alert-error">
          <strong>Error:</strong> {errorMsg}
          {errorDetails.length > 0 && (
            <ul style={{ margin: '8px 0 0', paddingLeft: '20px' }}>
              {errorDetails.map((msg, i) => (
                <li key={i}>{msg}</li>
              ))}
            </ul>
          )}
        </div>
      )}

      {/* Step 1: Add Your Tasks */}
      <TaskInputTable
        tasks={tasks}
        onTasksChange={setTasks}
        onLoadSample={handleLoadSample}
        onLoadDemo={handleLoadDemo}
        onOpenUploadModal={() => setIsCsvModalOpen(true)}
        onReset={handleReset}
        disabled={isLoading}
      />

      {/* Workload Summary Statistics */}
      <WorkloadSummaryCards
        tasks={tasks}
        results={compareResult ? compareResult.proposed.tasks : singleResult?.tasks}
      />

      {/* Step 2: Choose How Tasks Are Scheduled */}
      <AlgorithmSelector
        selected={selectedAlgorithm}
        onChange={setSelectedAlgorithm}
        disabled={isLoading}
      />

      {/* Step 3: Run Cloud Simulation */}
      <div style={{
        textAlign: 'center',
        margin: '32px 0 36px',
        padding: '24px',
        background: '#1e293b',
        borderRadius: '10px',
        border: '1px solid #334155'
      }}>
        <div style={{ marginBottom: '8px', fontSize: '1.05rem', fontWeight: 600, color: '#f8fafc' }}>
          Step 3: Run Simulation
        </div>
        <button
          className="btn btn-primary"
          style={{
            padding: '14px 44px',
            fontSize: '1.1rem',
            fontWeight: 700,
            letterSpacing: '0.01em',
            boxShadow: '0 4px 14px rgba(56, 189, 248, 0.25)',
            cursor: isLoading || tasks.length === 0 ? 'not-allowed' : 'pointer'
          }}
          onClick={handleRunSimulation}
          disabled={isLoading || tasks.length === 0}
        >
          {isLoading ? 'Running Cloud Simulation...' : 'Run Cloud Simulation'}
        </button>
        <p style={{ margin: '10px 0 0', fontSize: '0.86rem', color: '#94a3b8' }}>
          Run the selected scheduler and view how the tasks are executed.
        </p>
      </div>

      {/* Success Notification */}
      {simulationSuccess && (
        <div style={{
          margin: '0 0 24px',
          padding: '12px 18px',
          background: 'rgba(34, 197, 94, 0.12)',
          border: '1px solid #22c55e',
          borderRadius: '8px',
          color: '#86efac',
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          fontSize: '0.95rem',
          fontWeight: 600
        }}>
          <span>✔</span>
          <span>Simulation completed successfully.</span>
        </div>
      )}

      {/* Step 4: Results Rendering - Comparison Mode */}
      {selectedAlgorithm === 'COMPARE' && compareResult && (
        <>
          <div style={{ margin: '20px 0 12px' }}>
            <h2 style={{ fontSize: '1.4rem', color: '#f8fafc', margin: 0, fontWeight: 700 }}>
              Step 4: Simulation Results
            </h2>
            <p style={{ margin: '6px 0 0', fontSize: '0.9rem', color: '#94a3b8' }}>
              The results show how quickly tasks were completed, how long they waited, whether they met their deadlines, and how much the virtual machines were used.
            </p>
          </div>

          {/* Simulation Overview Summary Cards */}
          <SimulationOverview
            baselineTasks={compareResult.baseline.tasks}
            proposedTasks={compareResult.proposed.tasks}
            baselineMetrics={compareResult.baseline.metrics}
            proposedMetrics={compareResult.proposed.metrics}
          />

          {/* Metric Comparison Table */}
          <ComparisonTable comparisonData={compareResult} />

          {/* Metric Charts */}
          <MetricsCharts
            baselineMetrics={compareResult.baseline.metrics}
            proposedMetrics={compareResult.proposed.metrics}
            baselineTasks={compareResult.baseline.tasks}
            proposedTasks={compareResult.proposed.tasks}
          />

          {/* Collapsible Technical Details */}
          <TechnicalDetails />

          {/* Task Filters */}
          <TaskFilters
            filters={filters}
            onFilterChange={setFilters}
            onResetFilters={() => setFilters(DEFAULT_FILTERS)}
            availableVmIds={availableVmIds}
          />

          {/* Dual Column Analysis */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(480px, 1fr))', gap: '24px' }}>
            {/* Standard Priority Column */}
            <div>
              <MetricsCards
                metrics={compareResult.baseline.metrics}
                tasks={compareResult.baseline.tasks}
                title="Standard Priority Metrics"
                schedulerName="BASELINE"
              />
              <DeadlineAnalysisView
                tasks={filteredBaselineTasks}
                title="Standard Priority Deadline Analysis"
                schedulerName="BASELINE"
              />
              <VmUtilizationCards
                tasks={compareResult.baseline.tasks}
                makespan={compareResult.baseline.metrics.makespan}
                overallClusterUtilization={compareResult.baseline.metrics.resourceUtilization}
              />
              <VmTimeline
                tasks={filteredBaselineTasks}
                makespan={compareResult.baseline.metrics.makespan}
                title="Standard Priority Task Execution Timeline"
                schedulerName="BASELINE"
              />
              <TaskResultsTable
                tasks={filteredBaselineTasks}
                totalTasksCount={compareResult.baseline.tasks.length}
                title="Standard Priority Detailed Task Results"
                schedulerName="BASELINE"
              />
            </div>

            {/* Deadline-Aware Column */}
            <div>
              <MetricsCards
                metrics={compareResult.proposed.metrics}
                tasks={compareResult.proposed.tasks}
                title="Deadline-Aware Metrics"
                schedulerName="PROPOSED"
              />
              <DeadlineAnalysisView
                tasks={filteredProposedTasks}
                title="Deadline-Aware Deadline Analysis"
                schedulerName="PROPOSED"
              />
              <VmUtilizationCards
                tasks={compareResult.proposed.tasks}
                makespan={compareResult.proposed.metrics.makespan}
                overallClusterUtilization={compareResult.proposed.metrics.resourceUtilization}
              />
              <VmTimeline
                tasks={filteredProposedTasks}
                makespan={compareResult.proposed.metrics.makespan}
                title="Deadline-Aware Task Execution Timeline"
                schedulerName="PROPOSED"
              />
              <TaskResultsTable
                tasks={filteredProposedTasks}
                totalTasksCount={compareResult.proposed.tasks.length}
                title="Deadline-Aware Detailed Task Results"
                schedulerName="PROPOSED"
              />
            </div>
          </div>

          {/* Export Results */}
          <ExportButtons
            tasks={compareResult.proposed.tasks}
            metricsRequest={exportMetricsPayload}
          />
        </>
      )}

      {/* Step 4: Results Rendering - Single Algorithm Mode */}
      {selectedAlgorithm !== 'COMPARE' && singleResult && (
        <>
          <div style={{ margin: '20px 0 12px' }}>
            <h2 style={{ fontSize: '1.4rem', color: '#f8fafc', margin: 0, fontWeight: 700 }}>
              Step 4: Simulation Results
            </h2>
            <p style={{ margin: '6px 0 0', fontSize: '0.9rem', color: '#94a3b8' }}>
              The results show how quickly tasks were completed, how long they waited, whether they met their deadlines, and how much the virtual machines were used.
            </p>
          </div>

          {/* Simulation Overview Summary Cards */}
          <SimulationOverview
            singleTasks={singleResult.tasks}
            singleMetrics={singleResult.metrics}
            singleAlgorithmName={singleResult.algorithm}
          />

          <MetricsCards
            metrics={singleResult.metrics}
            tasks={singleResult.tasks}
            schedulerName={singleResult.algorithm}
          />

          <MetricsCharts
            singleMetrics={singleResult.metrics}
            singleTasks={singleResult.tasks}
            singleAlgorithmName={singleResult.algorithm}
          />

          {/* Collapsible Technical Details */}
          <TechnicalDetails />

          {/* Task Filters */}
          <TaskFilters
            filters={filters}
            onFilterChange={setFilters}
            onResetFilters={() => setFilters(DEFAULT_FILTERS)}
            availableVmIds={availableVmIds}
          />

          <DeadlineAnalysisView
            tasks={filteredSingleTasks}
            title={`${singleResult.algorithm === 'PROPOSED' ? 'Deadline-Aware' : 'Standard Priority'} Deadline Analysis`}
            schedulerName={singleResult.algorithm}
          />

          <VmUtilizationCards
            tasks={singleResult.tasks}
            makespan={singleResult.metrics.makespan}
            overallClusterUtilization={singleResult.metrics.resourceUtilization}
          />

          <VmTimeline
            tasks={filteredSingleTasks}
            makespan={singleResult.metrics.makespan}
            schedulerName={singleResult.algorithm}
          />

          <TaskResultsTable
            tasks={filteredSingleTasks}
            totalTasksCount={singleResult.tasks.length}
            schedulerName={singleResult.algorithm}
          />

          {/* Export Results */}
          <ExportButtons
            tasks={singleResult.tasks}
            metricsRequest={exportMetricsPayload}
          />
        </>
      )}

      {/* CSV Upload Modal */}
      <CsvUploadModal
        isOpen={isCsvModalOpen}
        onClose={() => setIsCsvModalOpen(false)}
        onWorkloadLoaded={(loadedTasks) => {
          setTasks(loadedTasks);
          setSingleResult(null);
          setCompareResult(null);
          setSimulationSuccess(false);
        }}
      />
    </div>
  );
};

