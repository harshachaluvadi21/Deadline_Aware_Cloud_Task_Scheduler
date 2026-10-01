import React, { useState } from 'react';
import { ExportMetricsRequest, TaskResultDto } from '../types/simulation';
import { simulationApi } from '../services/api';

interface ExportButtonsProps {
  tasks: TaskResultDto[];
  metricsRequest: ExportMetricsRequest;
  disabled?: boolean;
}

export const ExportButtons: React.FC<ExportButtonsProps> = ({
  tasks,
  metricsRequest,
  disabled = false
}) => {
  const [isExportingTasks, setIsExportingTasks] = useState(false);
  const [isExportingMetrics, setIsExportingMetrics] = useState(false);
  const [exportError, setExportError] = useState<string | null>(null);

  const handleExportTasks = async () => {
    if (tasks.length === 0) return;
    setIsExportingTasks(true);
    setExportError(null);
    try {
      await simulationApi.exportTaskResults(tasks);
    } catch (err: unknown) {
      setExportError(err instanceof Error ? err.message : 'Failed to export task CSV');
    } finally {
      setIsExportingTasks(false);
    }
  };

  const handleExportMetrics = async () => {
    setIsExportingMetrics(true);
    setExportError(null);
    try {
      await simulationApi.exportMetrics(metricsRequest);
    } catch (err: unknown) {
      setExportError(err instanceof Error ? err.message : 'Failed to export metrics CSV');
    } finally {
      setIsExportingMetrics(false);
    }
  };

  const isCompare = !!(metricsRequest.baseline && metricsRequest.proposed);

  return (
    <div className="card" style={{ marginTop: '24px' }}>
      <div style={{ marginBottom: 16 }}>
        <div className="card-title">📦 Export Results</div>
        <p className="card-subtitle">Download simulation data as CSV for offline analysis.</p>
      </div>

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
        {isCompare ? (
          <>
            <button
              className="btn btn-secondary"
              onClick={handleExportTasks}
              disabled={disabled || isExportingTasks || tasks.length === 0}
              title="Download Deadline-Aware task-level results (timing, VM, deadline status)"
            >
              {isExportingTasks ? <><span className="spinner" /> Exporting…</> : '📥 Download Deadline-Aware Task Results'}
            </button>
            <button
              className="btn btn-secondary"
              onClick={handleExportMetrics}
              disabled={disabled || isExportingMetrics}
              title="Download comparison metrics CSV with pairwise differences"
            >
              {isExportingMetrics ? <><span className="spinner" /> Exporting…</> : '📊 Download Comparison Metrics'}
            </button>
          </>
        ) : (
          <>
            <button
              className="btn btn-secondary"
              onClick={handleExportTasks}
              disabled={disabled || isExportingTasks || tasks.length === 0}
              title="Download task-level execution results"
            >
              {isExportingTasks ? <><span className="spinner" /> Exporting…</> : '📥 Download Task Results'}
            </button>
            <button
              className="btn btn-secondary"
              onClick={handleExportMetrics}
              disabled={disabled || isExportingMetrics}
              title="Download scheduling metrics summary"
            >
              {isExportingMetrics ? <><span className="spinner" /> Exporting…</> : '📊 Download Metrics Summary'}
            </button>
          </>
        )}
      </div>

      {exportError && (
        <div className="alert alert-error" style={{ marginTop: 12 }}>
          {exportError}
        </div>
      )}
    </div>
  );
};
