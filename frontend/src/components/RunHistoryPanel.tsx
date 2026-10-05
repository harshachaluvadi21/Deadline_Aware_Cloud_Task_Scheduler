import React, { useState } from 'react';
import { MetricsDto } from '../types/simulation';

interface RunHistoryEntry {
  id: string;
  timestamp: Date;
  algorithm: string;
  tasksCount: number;
  baselineMetrics?: MetricsDto;
  proposedMetrics?: MetricsDto;
  singleMetrics?: MetricsDto;
}

interface RunHistoryPanelProps {
  history: RunHistoryEntry[];
  onSelectRun?: (entry: RunHistoryEntry) => void;
}

export type { RunHistoryEntry };

export const RunHistoryPanel: React.FC<RunHistoryPanelProps> = ({ history, onSelectRun }) => {
  const [isOpen, setIsOpen] = useState(false);

  const formatTime = (d: Date) => {
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  };

  const renderMetricLine = (label: string, value: string) => (
    <div>
      {label}: <strong>{value}</strong>
    </div>
  );

  return (
    <>
      {/* Tab on the right edge */}
      <div className="run-history-toggle" onClick={() => setIsOpen(!isOpen)}>
        History
      </div>

      {/* Slide-in panel */}
      <div className={`run-history-panel ${isOpen ? 'open' : ''}`}>
        <div className="rh-header">
          <span className="rh-title">Simulation History</span>
          <button className="rh-close" onClick={() => setIsOpen(false)}>✕</button>
        </div>

        {history.length === 0 ? (
          <div className="rhi-empty">
            No simulations run yet.<br />Results will appear here after each run.
          </div>
        ) : (
          history.slice().reverse().map((entry) => (
            <div
              key={entry.id}
              className="run-history-item"
              onClick={() => onSelectRun?.(entry)}
            >
              <div className="rhi-top">
                <span className="rhi-algo">
                  {entry.algorithm === 'COMPARE' ? 'Compare Both' :
                    entry.algorithm === 'PROPOSED' ? 'Deadline-Aware' : 'Standard Priority'}
                </span>
                <span className="rhi-time">{formatTime(entry.timestamp)}</span>
              </div>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginBottom: 6 }}>
                {entry.tasksCount} tasks
              </div>
              <div className="rhi-metrics">
                {entry.algorithm === 'COMPARE' && entry.baselineMetrics && entry.proposedMetrics ? (
                  <>
                    {renderMetricLine('B Miss', `${entry.baselineMetrics.deadlineMissRate.toFixed(1)}%`)}
                    {renderMetricLine('P Miss', `${entry.proposedMetrics.deadlineMissRate.toFixed(1)}%`)}
                    {renderMetricLine('B Wait', `${entry.baselineMetrics.averageWaitingTime.toFixed(2)}s`)}
                    {renderMetricLine('P Wait', `${entry.proposedMetrics.averageWaitingTime.toFixed(2)}s`)}
                  </>
                ) : entry.singleMetrics ? (
                  <>
                    {renderMetricLine('Makespan', `${entry.singleMetrics.makespan.toFixed(2)}s`)}
                    {renderMetricLine('Miss Rate', `${entry.singleMetrics.deadlineMissRate.toFixed(1)}%`)}
                    {renderMetricLine('Avg Wait', `${entry.singleMetrics.averageWaitingTime.toFixed(2)}s`)}
                    {renderMetricLine('Util', `${entry.singleMetrics.resourceUtilization.toFixed(1)}%`)}
                  </>
                ) : null}
              </div>
            </div>
          ))
        )}
      </div>
    </>
  );
};
