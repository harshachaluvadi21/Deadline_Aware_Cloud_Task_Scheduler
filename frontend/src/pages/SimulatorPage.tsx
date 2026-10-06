import React, { useEffect, useState, useMemo, useRef } from 'react';
import { TaskInputTable } from '../components/TaskInputTable';
import { AlgorithmSelector } from '../components/AlgorithmSelector';
import { MetricsCards } from '../components/MetricsCards';
import { VmTimeline } from '../components/VmTimeline';
import { TaskResultsTable } from '../components/TaskResultsTable';
import { ComparisonTable } from '../components/ComparisonTable';
import { MetricsCharts } from '../components/MetricsCharts';
import { DeadlineAnalysisView, classifyTaskDeadline, computeDeadlineStats } from '../components/DeadlineAnalysisView';
import { VmUtilizationCards } from '../components/VmUtilizationCards';
import { TaskFilters, FilterCriteria } from '../components/TaskFilters';
import { CsvUploadModal } from '../components/CsvUploadModal';
import { ExportButtons } from '../components/ExportButtons';
import { SimulationOverview } from '../components/SimulationOverview';
import { TechnicalDetails } from '../components/TechnicalDetails';
import { GeminiAiAnalyst } from '../components/GeminiAiAnalyst';
import { SimulationSkeleton } from '../components/SimulationSkeleton';
import { InsightsBanner } from '../components/InsightsBanner';
import { RunHistoryPanel, RunHistoryEntry } from '../components/RunHistoryPanel';
import { PresetScenario } from '../data/presetWorkloads';
import { ApiError, simulationApi } from '../services/api';
import {
  AlgorithmMode, CompareResponse, ExecutionTarget, ExportMetricsRequest,
  SimulateResponse, TaskDto, TaskResultDto,
} from '../types/simulation';
import { CloudDatacenterTopology } from '../components/CloudDatacenterTopology';
import { CloudSlaAndCostView } from '../components/CloudSlaAndCostView';
import { CloudDegreeOfImbalance } from '../components/CloudDegreeOfImbalance';

const DEFAULT_FILTERS: FilterCriteria = {
  vmId: 'ALL', status: 'ALL', minPriority: 1, maxPriority: 10, deadlineCategory: 'ALL',
};

type Step = 1 | 2 | 3 | 4;
type ResultsTab = 'overview' | 'datacenter' | 'timelines' | 'ledger' | 'charts' | 'all';

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
            <div className={`stepper-step${isActive ? ' active' : ''}${isDone ? ' done' : ''}`}>
              <div className="stepper-bubble">
                {isDone && !isActive ? '✓' : s.n}
              </div>
              <span className="stepper-label">{s.label}</span>
            </div>
            {i < steps.length - 1 && (
              <div className={`stepper-connector${isDone ? ' done' : ''}`} />
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
};

const Tip: React.FC<{ text: string }> = ({ text }) => {
  const [show, setShow] = useState(false);
  return (
    <span className="tip-wrap">
      <span className="tip-icon" onMouseEnter={() => setShow(true)} onMouseLeave={() => setShow(false)}>?</span>
      {show && <span className="tip-content">{text}</span>}
    </span>
  );
};

export const SimulatorPage: React.FC = () => {
  const [backendConnected, setBackendConnected] = useState<boolean | null>(null);
  const [workerOnline, setWorkerOnline] = useState<boolean | null>(null);
  const [tasks, setTasks] = useState<TaskDto[]>([]);
  const [selectedAlgorithm, setSelectedAlgorithm] = useState<AlgorithmMode>('COMPARE');
  const [executionTarget, setExecutionTarget] = useState<ExecutionTarget>('SIMULATION');
  const [isLoading, setIsLoading] = useState(false);
  const [simulationSuccess, setSimulationSuccess] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [errorDetails, setErrorDetails] = useState<string[]>([]);

  const [singleResult, setSingleResult] = useState<SimulateResponse | null>(null);
  const [compareResult, setCompareResult] = useState<CompareResponse | null>(null);
  const [history, setHistory] = useState<RunHistoryEntry[]>([]);
  const [isCsvModalOpen, setIsCsvModalOpen] = useState(false);
  const [filters, setFilters] = useState<FilterCriteria>(DEFAULT_FILTERS);
  const [activeTab, setActiveTab] = useState<ResultsTab>('overview');
  const resultsRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const init = async () => {
      const isUp = await simulationApi.healthCheck();
      setBackendConnected(isUp);
      if (isUp) {
        try { const s = await simulationApi.getSampleWorkload(); setTasks(s); } catch { /* ignore */ }
      }
      try {
        const workerHealth = await simulationApi.checkWorkerHealth();
        setWorkerOnline(workerHealth.reachable);
      } catch {
        setWorkerOnline(false);
      }
    };
    init();
  }, []);


  useEffect(() => {
    if (simulationSuccess && resultsRef.current) {
      setTimeout(() => resultsRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 200);
    }
  }, [simulationSuccess]);

  // Keyboard shortcuts: Ctrl+Enter to run, Ctrl+D to load demo
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement;
      const isInput = target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA');
      if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        e.preventDefault();
        handleRunSimulation();
      } else if ((e.ctrlKey || e.metaKey) && (e.key === 'd' || e.key === 'D') && !isInput) {
        e.preventDefault();
        handleLoadDemo();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [tasks, selectedAlgorithm]);

  const handleSelectPreset = (preset: PresetScenario) => {
    setErrorMsg(null); setErrorDetails([]); setSimulationSuccess(false);
    setTasks(preset.tasks);
    setSingleResult(null); setCompareResult(null);
  };

  const handleLoadSample = async () => {
    setErrorMsg(null); setErrorDetails([]); setSimulationSuccess(false);
    try { setTasks(await simulationApi.getSampleWorkload()); } catch {
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
              taskId: parseInt(parts[0].trim()), priority: parseInt(parts[1].trim()),
              arrivalTime: parseFloat(parts[2].trim()), executionTime: parseFloat(parts[3].trim()),
              deadline: parseFloat(parts[4].trim()),
            });
          }
        }
        if (demoTasks.length > 0) { setTasks(demoTasks); setSingleResult(null); setCompareResult(null); return; }
      }
    } catch { /* fallback */ }
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
    setFilters(DEFAULT_FILTERS); setActiveTab('overview');
  };

  const handleRunSimulation = async () => {
    if (tasks.length === 0) {
      setErrorMsg('Cannot run simulation with an empty workload. Add tasks, load demo, or upload a CSV.');
      return;
    }
    setIsLoading(true); setErrorMsg(null); setErrorDetails([]);
    setSimulationSuccess(false); setSingleResult(null); setCompareResult(null);
    setActiveTab('overview');
    try {
      if (selectedAlgorithm === 'COMPARE') {
        const res = await simulationApi.compareSchedulers(tasks, executionTarget);
        setCompareResult(res);
        setHistory(prev => [
          {
            id: Date.now().toString(),
            timestamp: new Date(),
            algorithm: 'COMPARE',
            tasksCount: tasks.length,
            baselineMetrics: res.baseline.metrics,
            proposedMetrics: res.proposed.metrics,
          },
          ...prev,
        ]);
      } else {
        const res = await simulationApi.runSimulation(selectedAlgorithm, tasks, executionTarget);
        setSingleResult(res);
        setHistory(prev => [
          {
            id: Date.now().toString(),
            timestamp: new Date(),
            algorithm: selectedAlgorithm,
            tasksCount: tasks.length,
            singleMetrics: res.metrics,
          },
          ...prev,
        ]);
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

  const filterTaskList = (list: TaskResultDto[]) =>
    list.filter(t => {
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
    compareResult?.baseline.tasks.forEach(t => ids.add(t.assignedVmId));
    compareResult?.proposed.tasks.forEach(t => ids.add(t.assignedVmId));
    singleResult?.tasks.forEach(t => ids.add(t.assignedVmId));
    return Array.from(ids).sort((a, b) => a - b);
  }, [compareResult, singleResult]);

  const filteredBaselineTasks = useMemo(() => compareResult ? filterTaskList(compareResult.baseline.tasks) : [], [compareResult, filters]);
  const filteredProposedTasks = useMemo(() => compareResult ? filterTaskList(compareResult.proposed.tasks) : [], [compareResult, filters]);
  const filteredSingleTasks = useMemo(() => singleResult ? filterTaskList(singleResult.tasks) : [], [singleResult, filters]);

  const exportMetricsPayload: ExportMetricsRequest = useMemo(() => {
    if (compareResult) return { baseline: compareResult.baseline, proposed: compareResult.proposed };
    if (singleResult) return { algorithm: singleResult.algorithm, metrics: singleResult.metrics };
    return {};
  }, [compareResult, singleResult]);

  const computeDI = (taskList: TaskResultDto[], vmCount = 4): number => {
    if (!taskList || taskList.length === 0) return 0;
    const loads = [0, 0, 0, 0];
    taskList.forEach(t => {
      if (t.assignedVmId >= 0 && t.assignedVmId < vmCount) {
        loads[t.assignedVmId] += t.executionTime;
      }
    });
    const tMax = Math.max(...loads);
    const tMin = Math.min(...loads);
    const total = loads.reduce((a, b) => a + b, 0);
    const tAvg = total / vmCount;
    return tAvg > 0 ? (tMax - tMin) / tAvg : 0;
  };

  // Executive KPI summary data for Compare Results Strip
  const kpiData = useMemo(() => {
    if (!compareResult) return null;
    const base = compareResult.baseline;
    const prop = compareResult.proposed;

    const msBase = base.metrics.makespan;
    const msProp = prop.metrics.makespan;
    const msDiff = msBase - msProp;
    const msDiffPct = msBase > 0 ? ((msDiff / msBase) * 100).toFixed(1) : '0';

    const baseStats = computeDeadlineStats(base.tasks || []);
    const propStats = computeDeadlineStats(prop.tasks || []);
    const missedDiff = baseStats.missedCount - propStats.missedCount;

    const diBase = computeDI(base.tasks || []);
    const diProp = computeDI(prop.tasks || []);

    const slaCostBase = baseStats.missedCount * 0.20;
    const slaCostProp = propStats.missedCount * 0.20;
    const costSaved = Math.max(0, slaCostBase - slaCostProp);

    return {
      msBase, msProp, msDiffPct,
      missBase: baseStats.missedCount, missProp: propStats.missedCount,
      missRateBase: baseStats.missedRate.toFixed(0),
      missRateProp: propStats.missedRate.toFixed(0),
      missedDiff,
      diBase: diBase.toFixed(2), diProp: diProp.toFixed(2),
      costSaved: costSaved.toFixed(2),
    };
  }, [compareResult]);

  // Executive KPI summary data for Single Result Strip
  const singleKpiData = useMemo(() => {
    if (!singleResult) return null;
    const stats = computeDeadlineStats(singleResult.tasks || []);
    const di = computeDI(singleResult.tasks || []);
    return {
      makespan: singleResult.metrics.makespan.toFixed(2),
      missedCount: stats.missedCount,
      missRate: stats.missedRate.toFixed(0),
      utilization: singleResult.metrics.resourceUtilization.toFixed(1),
      throughput: singleResult.metrics.throughput.toFixed(4),
      di: di.toFixed(2),
    };
  }, [singleResult]);

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
              Configure cloud workloads, compare scheduling heuristics, and inspect datacenter telemetry in real time.
              <Tip text="Priority: 1–10 importance. Arrival Time: when task enters the simulation. Execution Time: how long it runs. Deadline: target completion time." />
            </p>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexShrink: 0 }}>
            {backendConnected === true && <span className="status-pill online"><span className="status-dot" />API Live</span>}
            {backendConnected === false && <span className="status-pill offline"><span className="status-dot" />Backend Offline</span>}
            {backendConnected === null && <span className="status-pill connecting"><span className="status-dot pulse" />Connecting…</span>}
          </div>
        </div>

        {/* Step indicator */}
        <StepBar current={currentStep} done={!!hasResults} />

        {/* Error notification */}
        {errorMsg && (
          <div className="alert alert-error" style={{ marginBottom: 20 }}>
            <span className="alert-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
            </span>
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

        {/* ── Steps 1, 2, 3: Interactive Simulator Control Deck ── */}
        <div className="sim-config-grid">
          {/* Step 1: Workload Studio (Left column) */}
          <div className="sim-step-card sim-workload-studio">
            <div className="card-title">
              <span style={{ display: 'flex', alignItems: 'center' }}>
                <span className="step-badge">1</span>
                Cloud Workload & Tasks
              </span>
              <span className="badge badge-cyan">
                {tasks.length} {tasks.length !== 1 ? 'tasks' : 'task'}
              </span>
            </div>
            <p className="card-subtitle">
              Define arrival times, compute durations, and strict SLA deadlines for each cloudlet.
            </p>
            <TaskInputTable
              tasks={tasks}
              onTasksChange={setTasks}
              onLoadSample={handleLoadSample}
              onLoadDemo={handleLoadDemo}
              onSelectPreset={handleSelectPreset}
              onOpenUploadModal={() => setIsCsvModalOpen(true)}
              onReset={handleReset}
              disabled={isLoading}
            />
          </div>

          {/* Execution Cockpit (Right column) */}
          <div className="sim-execution-cockpit">
            {/* Step 2: Scheduling Policy */}
            <div className="sim-step-card">
              <div className="card-title">
                <span style={{ display: 'flex', alignItems: 'center' }}>
                  <span className="step-badge">2</span>
                  Scheduling Policy
                </span>
              </div>
              <p className="card-subtitle">
                Select baseline priority, proposed deadline-urgency, or compare both side-by-side.
              </p>
              <AlgorithmSelector
                selected={selectedAlgorithm}
                onChange={setSelectedAlgorithm}
                disabled={isLoading}
              />
            </div>

            {/* Step 3: Cloud Engine & Launch Station */}
            <div className="sim-step-card sim-run-station">
              <div className="card-title">
                <span style={{ display: 'flex', alignItems: 'center' }}>
                  <span className="step-badge">3</span>
                  Simulation Engine
                </span>
                <span className="badge badge-purple" style={{ fontSize: '0.7rem' }}>
                  CloudSim Plus v4.0
                </span>
              </div>
              
              <div className="engine-specs-pill">
                <span>⚡ 4 Virtual Hosts</span>
                <span>•</span>
                <span>16 Cores</span>
                <span>•</span>
                <span>32 GB RAM</span>
              </div>

              {/* Execution Environment Selector */}
              <div style={{ marginTop: 12, marginBottom: 12 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                  <span style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-muted)' }}>
                    Execution Environment
                  </span>
                  {workerOnline !== null && (
                    <span style={{
                      fontSize: '0.70rem',
                      fontWeight: 600,
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: 4,
                      color: workerOnline ? 'var(--green, #22c55e)' : 'var(--red, #ef4444)'
                    }}>
                      <span style={{
                        width: 6,
                        height: 6,
                        borderRadius: '50%',
                        backgroundColor: workerOnline ? 'var(--green, #22c55e)' : 'var(--red, #ef4444)',
                        boxShadow: workerOnline ? '0 0 6px var(--green, #22c55e)' : 'none'
                      }} />
                      Worker Status: {workerOnline ? 'ONLINE' : 'OFFLINE'}
                    </span>
                  )}
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 6 }}>
                  <button
                    type="button"
                    className={`btn btn-sm ${executionTarget === 'SIMULATION' ? 'btn-primary' : 'btn-outline'}`}
                    style={{ fontSize: '0.74rem', padding: '6px 8px', justifyContent: 'center' }}
                    onClick={() => setExecutionTarget('SIMULATION')}
                    disabled={isLoading}
                  >
                    ⚡ Simulation
                  </button>
                  <button
                    type="button"
                    className={`btn btn-sm ${executionTarget === 'REAL_WORKER' ? 'btn-primary' : 'btn-outline'}`}
                    style={{ fontSize: '0.74rem', padding: '6px 8px', justifyContent: 'center' }}
                    onClick={() => {
                      setExecutionTarget('REAL_WORKER');
                      simulationApi.checkWorkerHealth().then(h => setWorkerOnline(h.reachable));
                    }}
                    disabled={isLoading}
                    title="Dispatch scheduled tasks to the configured real execution worker"
                  >
                    ⚙️ Real Worker
                  </button>
                </div>
                {executionTarget === 'REAL_WORKER' && (
                  <div style={{
                    marginTop: 8,
                    padding: '6px 10px',
                    borderRadius: 'var(--r-sm)',
                    background: 'rgba(56, 189, 248, 0.08)',
                    border: '1px solid rgba(56, 189, 248, 0.25)',
                    fontSize: '0.72rem',
                    color: 'var(--cyan)',
                    lineHeight: 1.35
                  }}>
                    Tasks are dispatched to the configured real execution worker.
                  </div>
                )}
              </div>

              <button
                id="btn-run-simulation"
                className="btn btn-primary btn-xl btn-run-hero"
                onClick={handleRunSimulation}
                disabled={isLoading || tasks.length === 0}
              >
                {isLoading ? (
                  <><span className="spinner" /> {executionTarget === 'REAL_WORKER' ? 'Executing on Real Worker…' : 'Running Simulation Engine…'}</>
                ) : (
                  executionTarget === 'REAL_WORKER' ? '⚙️ Run on Real Worker' : '🚀 Run Simulation'
                )}
              </button>

              <div className="run-shortcuts-hint">
                {tasks.length === 0 ? (
                  <span>↑ Add tasks in Step 1 to enable execution</span>
                ) : (
                  <span>Shortcut: Press <kbd>Ctrl+Enter</kbd> to run</span>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Loading skeleton */}
        {isLoading && <SimulationSkeleton />}

        {/* Success banner */}
        {simulationSuccess && (
          <div className="alert alert-success fade-in" style={{ marginBottom: 20 }}>
            <span className="alert-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <polyline points="20 6 9 17 4 12" />
              </svg>
            </span>
            <span>Simulation executed successfully across virtual machines. Explore the tabbed analysis below.</span>
          </div>
        )}

        {/* ── Step 4: Results Dashboard ── */}
        <div ref={resultsRef} />

        {/* COMPARE RESULTS DASHBOARD */}
        {selectedAlgorithm === 'COMPARE' && compareResult && (
          <section className="fade-in">
            {/* Results Header */}
            <div className="results-header">
              <div>
                <div className="section-label">Step 4 · Telemetry & Results</div>
                <div className="results-title">Controlled Multi-Scheduler Comparison</div>
                <div className="results-subtitle">
                  Evaluated {tasks.length} cloud tasks under Standard Priority (Baseline) vs. Deadline-Aware (Proposed) heuristics.
                </div>
              </div>
              <div style={{ display: 'flex', gap: 10 }}>
                <button
                  className="btn btn-ghost btn-sm"
                  onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
                  type="button"
                >
                  ↑ Modify Inputs
                </button>
              </div>
            </div>

            {/* KPI Highlight Strip */}
            {kpiData && (
              <div className="results-kpi-strip">
                <div className="results-kpi-card accent-cyan">
                  <div className="results-kpi-label">Makespan</div>
                  <div className="results-kpi-val">{kpiData.msProp.toFixed(2)}s</div>
                  <div className="results-kpi-sub">
                    <span style={{ color: Number(kpiData.msDiffPct) >= 0 ? 'var(--success)' : 'var(--danger)', fontWeight: 700 }}>
                      {Number(kpiData.msDiffPct) >= 0 ? `-${kpiData.msDiffPct}%` : `+${Math.abs(Number(kpiData.msDiffPct))}%`}
                    </span> vs {kpiData.msBase.toFixed(2)}s baseline
                  </div>
                </div>

                <div className="results-kpi-card accent-green">
                  <div className="results-kpi-label">Deadline Compliance</div>
                  <div className="results-kpi-val">{100 - Number(kpiData.missRateProp)}%</div>
                  <div className="results-kpi-sub">
                    <span style={{ color: 'var(--success)', fontWeight: 700 }}>
                      {kpiData.missProp} missed
                    </span> vs {kpiData.missBase} in baseline
                  </div>
                </div>

                <div className="results-kpi-card accent-purple">
                  <div className="results-kpi-label">Degree of Imbalance</div>
                  <div className="results-kpi-val">{kpiData.diProp}</div>
                  <div className="results-kpi-sub">
                    vs {kpiData.diBase} baseline (lower = better)
                  </div>
                </div>

                <div className="results-kpi-card accent-amber">
                  <div className="results-kpi-label">SLA Penalties Saved</div>
                  <div className="results-kpi-val">${kpiData.costSaved}</div>
                  <div className="results-kpi-sub">
                    {kpiData.missedDiff > 0 ? `${kpiData.missedDiff} SLA breaches avoided` : 'Zero SLA violations'}
                  </div>
                </div>
              </div>
            )}

            {/* Segmented Tab Navigation Bar */}
            <div className="results-tab-bar" role="tablist">
              <button
                role="tab"
                aria-selected={activeTab === 'overview'}
                className={`results-tab-btn ${activeTab === 'overview' ? 'active' : ''}`}
                onClick={() => setActiveTab('overview')}
              >
                <span>📊</span> Executive Overview
              </button>

              <button
                role="tab"
                aria-selected={activeTab === 'datacenter'}
                className={`results-tab-btn ${activeTab === 'datacenter' ? 'active' : ''}`}
                onClick={() => setActiveTab('datacenter')}
              >
                <span>☁️</span> Datacenter & Cost Model
              </button>

              <button
                role="tab"
                aria-selected={activeTab === 'timelines'}
                className={`results-tab-btn ${activeTab === 'timelines' ? 'active' : ''}`}
                onClick={() => setActiveTab('timelines')}
              >
                <span>⏱️</span> VM Timelines & Gantt
              </button>

              <button
                role="tab"
                aria-selected={activeTab === 'ledger'}
                className={`results-tab-btn ${activeTab === 'ledger' ? 'active' : ''}`}
                onClick={() => setActiveTab('ledger')}
              >
                <span>📋</span> Task Execution Ledger
                <span className="tab-badge">{filteredProposedTasks.length}</span>
              </button>

              <button
                role="tab"
                aria-selected={activeTab === 'charts'}
                className={`results-tab-btn ${activeTab === 'charts' ? 'active' : ''}`}
                onClick={() => setActiveTab('charts')}
              >
                <span>📈</span> Performance Charts & Math
              </button>

              <button
                role="tab"
                aria-selected={activeTab === 'all'}
                className={`results-tab-btn ${activeTab === 'all' ? 'active' : ''}`}
                onClick={() => setActiveTab('all')}
              >
                <span>🌐</span> All Views
              </button>
            </div>

            {/* Tab 1: Executive Overview */}
            {(activeTab === 'overview' || activeTab === 'all') && (
              <div className="fade-in">
                <InsightsBanner
                  isCompare={true}
                  baselineMetrics={compareResult.baseline.metrics}
                  proposedMetrics={compareResult.proposed.metrics}
                  baselineTasks={compareResult.baseline.tasks}
                  proposedTasks={compareResult.proposed.tasks}
                />
                <SimulationOverview
                  baselineTasks={compareResult.baseline.tasks}
                  proposedTasks={compareResult.proposed.tasks}
                  baselineMetrics={compareResult.baseline.metrics}
                  proposedMetrics={compareResult.proposed.metrics}
                />
                <ComparisonTable comparisonData={compareResult} />
                <GeminiAiAnalyst
                  isCompare={true}
                  baselineMetrics={compareResult.baseline.metrics}
                  proposedMetrics={compareResult.proposed.metrics}
                  baselineTasks={compareResult.baseline.tasks}
                  proposedTasks={compareResult.proposed.tasks}
                  tasksCount={tasks.length}
                />
              </div>
            )}

            {/* Tab 2: Datacenter & Cost Model */}
            {(activeTab === 'datacenter' || activeTab === 'all') && (
              <div className="fade-in">
                <CloudDatacenterTopology
                  tasks={compareResult.proposed.tasks}
                  schedulerName="Deadline-Aware (Active Allocation)"
                />
                <CloudSlaAndCostView
                  isCompare={true}
                  baselineMetrics={compareResult.baseline.metrics}
                  proposedMetrics={compareResult.proposed.metrics}
                  baselineTasks={compareResult.baseline.tasks}
                  proposedTasks={compareResult.proposed.tasks}
                />
                <CloudDegreeOfImbalance
                  isCompare={true}
                  baselineTasks={compareResult.baseline.tasks}
                  proposedTasks={compareResult.proposed.tasks}
                />
              </div>
            )}

            {/* Tab 3: VM Timelines & Gantt */}
            {(activeTab === 'timelines' || activeTab === 'all') && (
              <div className="fade-in">
                <div className="dual-col">
                  {/* Baseline column */}
                  <div>
                    <div className="dual-col-header baseline">
                      <span>◈</span> Standard Priority
                    </div>
                    <MetricsCards metrics={compareResult.baseline.metrics} tasks={compareResult.baseline.tasks} title="Standard Priority Metrics" schedulerName="BASELINE" />
                    <DeadlineAnalysisView tasks={filteredBaselineTasks} title="Deadline Analysis" schedulerName="BASELINE" />
                    <VmUtilizationCards tasks={compareResult.baseline.tasks} makespan={compareResult.baseline.metrics.makespan} overallClusterUtilization={compareResult.baseline.metrics.resourceUtilization} />
                    <VmTimeline tasks={filteredBaselineTasks} makespan={compareResult.baseline.metrics.makespan} title="Execution Timeline" schedulerName="BASELINE" />
                  </div>

                  {/* Proposed column */}
                  <div>
                    <div className="dual-col-header proposed">
                      <span>◆</span> Deadline-Aware
                    </div>
                    <MetricsCards metrics={compareResult.proposed.metrics} tasks={compareResult.proposed.tasks} title="Deadline-Aware Metrics" schedulerName="PROPOSED" />
                    <DeadlineAnalysisView tasks={filteredProposedTasks} title="Deadline Analysis" schedulerName="PROPOSED" />
                    <VmUtilizationCards tasks={compareResult.proposed.tasks} makespan={compareResult.proposed.metrics.makespan} overallClusterUtilization={compareResult.proposed.metrics.resourceUtilization} />
                    <VmTimeline tasks={filteredProposedTasks} makespan={compareResult.proposed.metrics.makespan} title="Execution Timeline" schedulerName="PROPOSED" />
                  </div>
                </div>
              </div>
            )}

            {/* Tab 4: Task Execution Ledger */}
            {(activeTab === 'ledger' || activeTab === 'all') && (
              <div className="fade-in">
                <TaskFilters
                  filters={filters}
                  onFilterChange={setFilters}
                  onResetFilters={() => setFilters(DEFAULT_FILTERS)}
                  availableVmIds={availableVmIds}
                />
                <div className="dual-col">
                  <div>
                    <div className="dual-col-header baseline">
                      <span>◈</span> Standard Priority Task Ledger
                    </div>
                    <TaskResultsTable tasks={filteredBaselineTasks} totalTasksCount={compareResult.baseline.tasks.length} title="Task Results" schedulerName="BASELINE" />
                  </div>
                  <div>
                    <div className="dual-col-header proposed">
                      <span>◆</span> Deadline-Aware Task Ledger
                    </div>
                    <TaskResultsTable tasks={filteredProposedTasks} totalTasksCount={compareResult.proposed.tasks.length} title="Task Results" schedulerName="PROPOSED" />
                  </div>
                </div>
              </div>
            )}

            {/* Tab 5: Performance Charts & Technical Details */}
            {(activeTab === 'charts' || activeTab === 'all') && (
              <div className="fade-in">
                <MetricsCharts
                  baselineMetrics={compareResult.baseline.metrics}
                  proposedMetrics={compareResult.proposed.metrics}
                  baselineTasks={compareResult.baseline.tasks}
                  proposedTasks={compareResult.proposed.tasks}
                />
                <TechnicalDetails />
              </div>
            )}

            {/* Export Toolbar */}
            <div style={{ marginTop: 24 }}>
              <ExportButtons tasks={compareResult.proposed.tasks} metricsRequest={exportMetricsPayload} />
            </div>
          </section>
        )}

        {/* SINGLE RESULT DASHBOARD */}
        {selectedAlgorithm !== 'COMPARE' && singleResult && (
          <section className="fade-in">
            <div className="results-header">
              <div>
                <div className="section-label">Step 4 · Telemetry & Results</div>
                <div className="results-title">
                  {singleResult.algorithm === 'PROPOSED' ? 'Deadline-Aware Simulation Results' : 'Standard Priority Simulation Results'}
                </div>
                <div className="results-subtitle">
                  Workload of {singleResult.tasks.length} cloud tasks evaluated on CloudSim Plus virtual infrastructure.
                </div>
              </div>
              <div>
                <button
                  className="btn btn-ghost btn-sm"
                  onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
                  type="button"
                >
                  ↑ Modify Inputs
                </button>
              </div>
            </div>

            {/* Single KPI Strip */}
            {singleKpiData && (
              <div className="results-kpi-strip">
                <div className="results-kpi-card accent-cyan">
                  <div className="results-kpi-label">Makespan</div>
                  <div className="results-kpi-val">{singleKpiData.makespan}s</div>
                  <div className="results-kpi-sub">Total time to execute workload</div>
                </div>

                <div className="results-kpi-card accent-green">
                  <div className="results-kpi-label">Deadline Compliance</div>
                  <div className="results-kpi-val">{100 - Number(singleKpiData.missRate)}%</div>
                  <div className="results-kpi-sub">{singleKpiData.missedCount} SLA breaches observed</div>
                </div>

                <div className="results-kpi-card accent-purple">
                  <div className="results-kpi-label">Cluster Utilization</div>
                  <div className="results-kpi-val">{singleKpiData.utilization}%</div>
                  <div className="results-kpi-sub">Average across 4 Virtual Machines</div>
                </div>

                <div className="results-kpi-card accent-amber">
                  <div className="results-kpi-label">Throughput</div>
                  <div className="results-kpi-val">{singleKpiData.throughput}</div>
                  <div className="results-kpi-sub">Tasks completed per second</div>
                </div>
              </div>
            )}

            {/* Segmented Tab Navigation Bar */}
            <div className="results-tab-bar" role="tablist">
              <button
                role="tab"
                aria-selected={activeTab === 'overview'}
                className={`results-tab-btn ${activeTab === 'overview' ? 'active' : ''}`}
                onClick={() => setActiveTab('overview')}
              >
                <span>📊</span> Executive Overview
              </button>

              <button
                role="tab"
                aria-selected={activeTab === 'datacenter'}
                className={`results-tab-btn ${activeTab === 'datacenter' ? 'active' : ''}`}
                onClick={() => setActiveTab('datacenter')}
              >
                <span>☁️</span> Datacenter & Cost Model
              </button>

              <button
                role="tab"
                aria-selected={activeTab === 'timelines'}
                className={`results-tab-btn ${activeTab === 'timelines' ? 'active' : ''}`}
                onClick={() => setActiveTab('timelines')}
              >
                <span>⏱️</span> VM Timelines & Gantt
              </button>

              <button
                role="tab"
                aria-selected={activeTab === 'ledger'}
                className={`results-tab-btn ${activeTab === 'ledger' ? 'active' : ''}`}
                onClick={() => setActiveTab('ledger')}
              >
                <span>📋</span> Task Execution Ledger
                <span className="tab-badge">{filteredSingleTasks.length}</span>
              </button>

              <button
                role="tab"
                aria-selected={activeTab === 'charts'}
                className={`results-tab-btn ${activeTab === 'charts' ? 'active' : ''}`}
                onClick={() => setActiveTab('charts')}
              >
                <span>📈</span> Performance Charts & Math
              </button>

              <button
                role="tab"
                aria-selected={activeTab === 'all'}
                className={`results-tab-btn ${activeTab === 'all' ? 'active' : ''}`}
                onClick={() => setActiveTab('all')}
              >
                <span>🌐</span> All Views
              </button>
            </div>

            {/* Tab 1: Executive Overview */}
            {(activeTab === 'overview' || activeTab === 'all') && (
              <div className="fade-in">
                <InsightsBanner
                  isCompare={false}
                  singleMetrics={singleResult.metrics}
                  singleAlgorithm={singleResult.algorithm}
                  singleTasks={singleResult.tasks}
                />
                <SimulationOverview singleTasks={singleResult.tasks} singleMetrics={singleResult.metrics} singleAlgorithmName={singleResult.algorithm} />
                <MetricsCards metrics={singleResult.metrics} tasks={singleResult.tasks} schedulerName={singleResult.algorithm} />
                <GeminiAiAnalyst
                  isCompare={false}
                  singleMetrics={singleResult.metrics}
                  singleAlgorithm={singleResult.algorithm}
                  tasksCount={tasks.length}
                />
              </div>
            )}

            {/* Tab 2: Datacenter & Cost Model */}
            {(activeTab === 'datacenter' || activeTab === 'all') && (
              <div className="fade-in">
                <CloudDatacenterTopology
                  tasks={singleResult.tasks}
                  schedulerName={singleResult.algorithm}
                />
                <CloudSlaAndCostView
                  isCompare={false}
                  singleMetrics={singleResult.metrics}
                  singleAlgorithm={singleResult.algorithm}
                  singleTasks={singleResult.tasks}
                />
                <CloudDegreeOfImbalance
                  isCompare={false}
                  singleTasks={singleResult.tasks}
                  singleAlgorithm={singleResult.algorithm}
                />
              </div>
            )}

            {/* Tab 3: VM Timelines & Gantt */}
            {(activeTab === 'timelines' || activeTab === 'all') && (
              <div className="fade-in">
                <DeadlineAnalysisView tasks={filteredSingleTasks} title={`${singleResult.algorithm === 'PROPOSED' ? 'Deadline-Aware' : 'Standard Priority'} Deadline Analysis`} schedulerName={singleResult.algorithm} />
                <VmUtilizationCards tasks={singleResult.tasks} makespan={singleResult.metrics.makespan} overallClusterUtilization={singleResult.metrics.resourceUtilization} />
                <VmTimeline tasks={filteredSingleTasks} makespan={singleResult.metrics.makespan} schedulerName={singleResult.algorithm} />
              </div>
            )}

            {/* Tab 4: Task Execution Ledger */}
            {(activeTab === 'ledger' || activeTab === 'all') && (
              <div className="fade-in">
                <TaskFilters filters={filters} onFilterChange={setFilters} onResetFilters={() => setFilters(DEFAULT_FILTERS)} availableVmIds={availableVmIds} />
                <TaskResultsTable tasks={filteredSingleTasks} totalTasksCount={singleResult.tasks.length} schedulerName={singleResult.algorithm} />
              </div>
            )}

            {/* Tab 5: Performance Charts & Math */}
            {(activeTab === 'charts' || activeTab === 'all') && (
              <div className="fade-in">
                <MetricsCharts singleMetrics={singleResult.metrics} singleTasks={singleResult.tasks} singleAlgorithmName={singleResult.algorithm} />
                <TechnicalDetails />
              </div>
            )}

            {/* Export Toolbar */}
            <div style={{ marginTop: 24 }}>
              <ExportButtons tasks={singleResult.tasks} metricsRequest={exportMetricsPayload} />
            </div>
          </section>
        )}
      </div>

      <CsvUploadModal
        isOpen={isCsvModalOpen}
        onClose={() => setIsCsvModalOpen(false)}
        onWorkloadLoaded={loadedTasks => {
          setTasks(loadedTasks); setSingleResult(null); setCompareResult(null); setSimulationSuccess(false);
        }}
      />

      <RunHistoryPanel history={history} />
    </main>
  );
};
