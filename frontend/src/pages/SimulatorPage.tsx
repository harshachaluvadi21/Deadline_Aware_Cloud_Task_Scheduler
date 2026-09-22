import React, { useEffect, useState, useMemo, useRef } from 'react';
import { TaskInputTable } from '../components/TaskInputTable';
import { AlgorithmSelector } from '../components/AlgorithmSelector';
import { MetricsCards } from '../components/MetricsCards';
import { VmTimeline } from '../components/VmTimeline';
import { TaskResultsTable } from '../components/TaskResultsTable';
import { ComparisonTable } from '../components/ComparisonTable';
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
  TaskResultDto,
} from '../types/simulation';

const DEFAULT_FILTERS: FilterCriteria = {
  vmId: 'ALL',
  status: 'ALL',
  minPriority: 1,
  maxPriority: 10,
  deadlineCategory: 'ALL',
};

// ── Step indicator ──────────────────────────────────────────
type Step = 1 | 2 | 3 | 4;

const StepBar: React.FC<{ current: Step; done: boolean }> = ({ current, done }) => {
  const steps = [
    { n: 1, label: 'Workload' },
    { n: 2, label: 'Scheduler' },
    { n: 3, label: 'Simulation' },
    { n: 4, label: 'Results' },
  ];

  return (
    <div className="stepper">
      {steps.map((s, i) => {
        const isActive = s.n === current;
        const isDone = done ? s.n <= 4 : s.n < current;
        return (
          <React.Fragment key={s.n}>
            <div className={`stepper-step ${isActive ? 'active' : ''} ${isDone ? 'done' : ''}`}>
              <div className="stepper-bubble">
                {isDone && !isActive ? '✓' : s.n}
              </div>
              <span className="stepper-label">{s.label}</span>
            </div>
            {i < steps.length - 1 && (
              <div className={`stepper-connector ${isDone ? 'done' : ''}`} />
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
};

// ── Tooltip helper ──────────────────────────────────────────
const Tip: React.FC<{ text: string }> = ({ text }) => {
  const [show, setShow] = useState(false);
  return (
    <span className="tooltip-wrapper">
      <span
        className="tooltip-icon"
        onMouseEnter={() => setShow(true)}
        onMouseLeave={() => setShow(false)}
      >
        ?
      </span>
      {show && <span className="tooltip-content">{text}</span>}
    </span>
  );
};

// ── Main page ───────────────────────────────────────────────
export const SimulatorPage: React.FC = () => {
  const [backendConnected, setBackendConnected] = useState<boolean | null>(null);
  const [tasks, setTasks] = useState<TaskDto[]>([]);
  const [selectedAlgorithm, setSelectedAlgorithm] = useState<AlgorithmMode>('COMPARE');
  const [isLoading, setIsLoading] = useState(false);
  const [simulationSuccess, setSimulationSuccess] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [errorDetails, setErrorDetails] = useState<string[]>([]);

  const [singleResult, setSingleResult] = useState<SimulateResponse | null>(null);
  const [compareResult, setCompareResult] = useState<CompareResponse | null>(null);

  const [isCsvModalOpen, setIsCsvModalOpen] = useState(false);
  const [filters, setFilters] = useState<FilterCriteria>(DEFAULT_FILTERS);

  const resultsRef = useRef<HTMLDivElement>(null);

  // Health check + sample workload on mount
  useEffect(() => {
    const init = async () => {
      const isUp = await simulationApi.healthCheck();
      setBackendConnected(isUp);
      if (isUp) {
        try {
          const sample = await simulationApi.getSampleWorkload();
          setTasks(sample);
        } catch {
          /* ignore */
        }
      }
    };
    init();
  }, []);

  // Scroll to results after simulation
  useEffect(() => {
    if (simulationSuccess && resultsRef.current) {
      setTimeout(() => {
        resultsRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }, 200);
    }
  }, [simulationSuccess]);

  // ── Workload helpers ──
  const handleLoadSample = async () => {
    setErrorMsg(null); setErrorDetails([]); setSimulationSuccess(false);
    try {
      const sample = await simulationApi.getSampleWorkload();
      setTasks(sample);
    } catch {
      setTasks([
        { taskId: 0, priority: 5, arrivalTime: 0.00, executionTime: 15.65, deadline: 47.31 },
        { taskId: 1, priority: 4, arrivalTime: 1.99, executionTime: 10.56, deadline: 30.90 },
        { taskId: 2, priority: 6, arrivalTime: 2.85, executionTime: 12.58, deadline: 56.86 },
        { taskId: 3, priority: 5, arrivalTime: 4.68, executionTime: 10.28, deadline: 40.74 },
        { taskId: 4, priority: 3, arrivalTime: 6.42, executionTime: 7.65, deadline: 30.40 },
      ]);
    }
    setSingleResult(null); setCompareResult(null);
  };

  const handleLoadDemo = async () => {
    setErrorMsg(null); setErrorDetails([]); setSimulationSuccess(false);
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
              deadline: parseFloat(parts[4].trim()),
            });
          }
        }
        if (demoTasks.length > 0) { setTasks(demoTasks); setSingleResult(null); setCompareResult(null); return; }
      }
    } catch { /* fallback */ }
    // Deterministic 10-task demo fallback
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
      { taskId: 9, priority: 4, arrivalTime: 12.00, executionTime: 16.00, deadline: 35.00 },
    ]);
    setSingleResult(null); setCompareResult(null);
  };

  const handleReset = () => {
    setTasks([]); setSingleResult(null); setCompareResult(null);
    setErrorMsg(null); setErrorDetails([]); setSimulationSuccess(false);
    setFilters(DEFAULT_FILTERS);
  };

  // ── Run simulation ──
  const handleRunSimulation = async () => {
    if (tasks.length === 0) {
      setErrorMsg('Cannot run simulation with an empty workload. Add tasks, load demo, or upload a CSV.');
      return;
    }
    setIsLoading(true); setErrorMsg(null); setErrorDetails([]);
    setSimulationSuccess(false); setSingleResult(null); setCompareResult(null);

    try {
      if (selectedAlgorithm === 'COMPARE') {
        const result = await simulationApi.compareSchedulers(tasks);
        setCompareResult(result);
      } else {
        const result = await simulationApi.runSimulation(selectedAlgorithm, tasks);
        setSingleResult(result);
      }
      setSimulationSuccess(true);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setErrorMsg(err.message); setErrorDetails(err.messages);
      } else if (err instanceof Error) {
        setErrorMsg(err.message);
      } else {
        setErrorMsg('An unexpected simulation error occurred.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  // ── Filter helpers ──
  const filterTaskList = (list: TaskResultDto[]) =>
    list.filter((t) => {
      if (filters.vmId !== 'ALL' && t.assignedVmId.toString() !== filters.vmId) return false;
      if (filters.status !== 'ALL') {
        if (filters.status === 'SUCCESS' && (t.deadlineMissed || t.status === 'MISSED_DEADLINE')) return false;
        if (filters.status === 'MISSED_DEADLINE' && !t.deadlineMissed && t.status !== 'MISSED_DEADLINE') return false;
      }
      if (t.priority < filters.minPriority || t.priority > filters.maxPriority) return false;
      if (filters.deadlineCategory !== 'ALL') {
        const cat = classifyTaskDeadline(t.completionTime, t.deadline, t.deadlineMissed, t.status);
        if (cat.category !== filters.deadlineCategory) return false;
      }
      return true;
    });

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

  const filteredBaselineTasks = useMemo(
    () => (compareResult ? filterTaskList(compareResult.baseline.tasks) : []),
    [compareResult, filters]
  );
  const filteredProposedTasks = useMemo(
    () => (compareResult ? filterTaskList(compareResult.proposed.tasks) : []),
    [compareResult, filters]
  );
  const filteredSingleTasks = useMemo(
    () => (singleResult ? filterTaskList(singleResult.tasks) : []),
    [singleResult, filters]
  );

  const exportMetricsPayload: ExportMetricsRequest = useMemo(() => {
    if (compareResult) return { baseline: compareResult.baseline, proposed: compareResult.proposed };
    if (singleResult) return { algorithm: singleResult.algorithm, metrics: singleResult.metrics };
    return {};
  }, [compareResult, singleResult]);

  const hasResults = simulationSuccess && (compareResult || singleResult);
  const currentStep: Step = hasResults ? 4 : isLoading ? 3 : tasks.length > 0 ? 2 : 1;

  return (
    <main className="simulator-page">
      <div className="container">
        {/* Page header */}
        <div className="page-header">
          <div>
            <h1 className="page-title">Cloud Scheduler Simulator</h1>
            <p className="page-desc">
              Define a workload, choose a scheduling method, and analyze results.
            </p>
          </div>
          {backendConnected === true && (
            <span className="status-indicator online" style={{ flexShrink: 0 }}>
              <span className="status-dot" /> Backend Online
            </span>
          )}
          {backendConnected === false && (
            <span className="status-indicator offline" style={{ flexShrink: 0 }}>
              <span className="status-dot" /> Backend Offline
            </span>
          )}
          {backendConnected === null && (
            <span className="status-indicator connecting" style={{ flexShrink: 0 }}>
              <span className="status-dot pulse" /> Connecting…
            </span>
          )}
        </div>

        {/* Step indicator */}
        <StepBar current={currentStep} done={!!hasResults} />

        {/* Error */}
        {errorMsg && (
          <div className="alert alert-error">
            <span>⚠</span>
            <div>
              <strong>{errorMsg}</strong>
              {errorDetails.length > 0 && (
                <ul style={{ margin: '6px 0 0', paddingLeft: 18 }}>
                  {errorDetails.map((m, i) => <li key={i}>{m}</li>)}
                </ul>
              )}
            </div>
          </div>
        )}

        {/* Step 1: Workload */}
        <div className="card">
          <div className="card-title">
            <span>
              <span
                style={{
                  display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                  width: 24, height: 24, borderRadius: '50%',
                  background: 'var(--accent-dim)', color: 'var(--accent)',
                  fontSize: '0.75rem', fontWeight: 800, marginRight: 10,
                }}
              >1</span>
              Define Your Workload
            </span>
            <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontWeight: 400 }}>
              {tasks.length} task{tasks.length !== 1 ? 's' : ''}
            </span>
          </div>
          <p className="card-subtitle">
            Tasks represent jobs submitted to the simulated cloud environment. Each task needs a
            priority, arrival time, execution time, and deadline.
            <Tip text="Priority: importance (1–10). Arrival Time: when the task enters the simulation. Execution Time: how long it runs. Deadline: target completion time." />
          </p>
          <TaskInputTable
            tasks={tasks}
            onTasksChange={setTasks}
            onLoadSample={handleLoadSample}
            onLoadDemo={handleLoadDemo}
            onOpenUploadModal={() => setIsCsvModalOpen(true)}
            onReset={handleReset}
            disabled={isLoading}
          />
        </div>

        {/* Step 2: Scheduler */}
        <div className="card">
          <div className="card-title">
            <span>
              <span
                style={{
                  display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                  width: 24, height: 24, borderRadius: '50%',
                  background: 'var(--accent-dim)', color: 'var(--accent)',
                  fontSize: '0.75rem', fontWeight: 800, marginRight: 10,
                }}
              >2</span>
              Choose Scheduling Method
            </span>
          </div>
          <p className="card-subtitle">
            Select one scheduling method or compare both using the same workload.
          </p>
          <AlgorithmSelector
            selected={selectedAlgorithm}
            onChange={setSelectedAlgorithm}
            disabled={isLoading}
          />
        </div>

        {/* Step 3: Run */}
        <div className="run-sim-area">
          <div
            style={{
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
              width: 24, height: 24, borderRadius: '50%',
              background: 'var(--accent-dim)', color: 'var(--accent)',
              fontSize: '0.75rem', fontWeight: 800, marginRight: 10,
            }}
          >3</div>
          <div className="run-sim-title" style={{ display: 'inline' }}>Run Cloud Simulation</div>
          <div className="run-sim-desc" style={{ marginTop: 6 }}>
            Submit the workload to the CloudSim Plus engine and view scheduling results.
          </div>
          <button
            className="btn btn-primary btn-xl"
            onClick={handleRunSimulation}
            disabled={isLoading || tasks.length === 0}
          >
            {isLoading ? (
              <>
                <span className="loading-spinner" />
                Running Simulation…
              </>
            ) : (
              '▶ Run Cloud Simulation'
            )}
          </button>
          {tasks.length === 0 && !isLoading && (
            <p style={{ marginTop: 10, fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              Add tasks above before running the simulation.
            </p>
          )}
        </div>

        {/* Success banner */}
        {simulationSuccess && (
          <div className="alert alert-success fade-in">
            <span>✔</span>
            <span>Simulation completed successfully — results are shown below.</span>
          </div>
        )}

        {/* ── Step 4: Results ── */}
        <div ref={resultsRef} />

        {/* COMPARE results */}
        {selectedAlgorithm === 'COMPARE' && compareResult && (
          <section className="fade-in">
            <div className="results-section-header" style={{ marginTop: 8 }}>
              <div>
                <div style={{ fontSize: '0.72rem', fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', color: 'var(--accent)', marginBottom: 4 }}>Step 4</div>
                <h2 className="results-section-title" style={{ fontSize: '1.25rem' }}>
                  Controlled Scheduler Comparison
                </h2>
                <p className="results-section-subtitle">
                  Both schedulers ran on identical workloads. Review the metrics and timeline below.
                </p>
              </div>
            </div>

            <SimulationOverview
              baselineTasks={compareResult.baseline.tasks}
              proposedTasks={compareResult.proposed.tasks}
              baselineMetrics={compareResult.baseline.metrics}
              proposedMetrics={compareResult.proposed.metrics}
            />

            <ComparisonTable comparisonData={compareResult} />

            <MetricsCharts
              baselineMetrics={compareResult.baseline.metrics}
              proposedMetrics={compareResult.proposed.metrics}
              baselineTasks={compareResult.baseline.tasks}
              proposedTasks={compareResult.proposed.tasks}
            />

            <TechnicalDetails />

            <TaskFilters
              filters={filters}
              onFilterChange={setFilters}
              onResetFilters={() => setFilters(DEFAULT_FILTERS)}
              availableVmIds={availableVmIds}
            />

            {/* Dual column */}
            <div className="dual-col">
              {/* Baseline column */}
              <div>
                <div className="dual-col-header baseline">
                  <span>◈</span> Standard Priority
                </div>
                <MetricsCards
                  metrics={compareResult.baseline.metrics}
                  tasks={compareResult.baseline.tasks}
                  title="Standard Priority Metrics"
                  schedulerName="BASELINE"
                />
                <DeadlineAnalysisView
                  tasks={filteredBaselineTasks}
                  title="Deadline Analysis"
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
                  title="Task Execution Timeline"
                  schedulerName="BASELINE"
                />
                <TaskResultsTable
                  tasks={filteredBaselineTasks}
                  totalTasksCount={compareResult.baseline.tasks.length}
                  title="Detailed Task Results"
                  schedulerName="BASELINE"
                />
              </div>

              {/* Proposed column */}
              <div>
                <div className="dual-col-header proposed">
                  <span>◆</span> Deadline-Aware
                </div>
                <MetricsCards
                  metrics={compareResult.proposed.metrics}
                  tasks={compareResult.proposed.tasks}
                  title="Deadline-Aware Metrics"
                  schedulerName="PROPOSED"
                />
                <DeadlineAnalysisView
                  tasks={filteredProposedTasks}
                  title="Deadline Analysis"
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
                  title="Task Execution Timeline"
                  schedulerName="PROPOSED"
                />
                <TaskResultsTable
                  tasks={filteredProposedTasks}
                  totalTasksCount={compareResult.proposed.tasks.length}
                  title="Detailed Task Results"
                  schedulerName="PROPOSED"
                />
              </div>
            </div>

            {/* Export */}
            <ExportButtons
              tasks={compareResult.proposed.tasks}
              metricsRequest={exportMetricsPayload}
            />
          </section>
        )}

        {/* SINGLE result */}
        {selectedAlgorithm !== 'COMPARE' && singleResult && (
          <section className="fade-in">
            <div className="results-section-header" style={{ marginTop: 8 }}>
              <div>
                <div style={{ fontSize: '0.72rem', fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', color: 'var(--accent)', marginBottom: 4 }}>Step 4</div>
                <h2 className="results-section-title" style={{ fontSize: '1.25rem' }}>
                  Simulation Results
                </h2>
                <p className="results-section-subtitle">
                  Review how the{' '}
                  {singleResult.algorithm === 'PROPOSED' ? 'Deadline-Aware' : 'Standard Priority'}{' '}
                  scheduler executed your workload.
                </p>
              </div>
            </div>

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

            <TechnicalDetails />

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

            <ExportButtons
              tasks={singleResult.tasks}
              metricsRequest={exportMetricsPayload}
            />
          </section>
        )}
      </div>

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
    </main>
  );
};
