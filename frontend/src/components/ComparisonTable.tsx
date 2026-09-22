import React from 'react';
import { CompareResponse } from '../types/simulation';
import { computeDeadlineStats } from './DeadlineAnalysisView';

interface ComparisonTableProps {
  comparisonData: CompareResponse;
}

export const ComparisonTable: React.FC<ComparisonTableProps> = ({ comparisonData }) => {
  const { baseline, proposed, comparison } = comparisonData;

  const baseStats = baseline.tasks && baseline.tasks.length > 0
    ? computeDeadlineStats(baseline.tasks)
    : {
        missedRate: baseline.metrics.deadlineMissRate > 1 ? baseline.metrics.deadlineMissRate : baseline.metrics.deadlineMissRate * 100
      };

  const propStats = proposed.tasks && proposed.tasks.length > 0
    ? computeDeadlineStats(proposed.tasks)
    : {
        missedRate: proposed.metrics.deadlineMissRate > 1 ? proposed.metrics.deadlineMissRate : proposed.metrics.deadlineMissRate * 100
      };

  const missRateDiff = propStats.missedRate - baseStats.missedRate;

  const rows = [
    {
      metric: 'Total Completion Time (Makespan)',
      tooltip: 'Time from the start of the simulation until all tasks finish.',
      baseline: `${baseline.metrics.makespan.toFixed(2)} s`,
      proposed: `${proposed.metrics.makespan.toFixed(2)} s`,
      diff: `${comparison.makespanDifference >= 0 ? '+' : ''}${comparison.makespanDifference.toFixed(2)} s`
    },
    {
      metric: 'Average Waiting Time',
      tooltip: 'Average time tasks waited before execution.',
      baseline: `${baseline.metrics.averageWaitingTime.toFixed(2)} s`,
      proposed: `${proposed.metrics.averageWaitingTime.toFixed(2)} s`,
      diff: `${comparison.waitingTimeDifference >= 0 ? '+' : ''}${comparison.waitingTimeDifference.toFixed(2)} s`
    },
    {
      metric: 'Average Turnaround Time',
      tooltip: 'Average time from task arrival until completion.',
      baseline: `${baseline.metrics.averageTurnaroundTime.toFixed(2)} s`,
      proposed: `${proposed.metrics.averageTurnaroundTime.toFixed(2)} s`,
      diff: `${comparison.turnaroundDifference >= 0 ? '+' : ''}${comparison.turnaroundDifference.toFixed(2)} s`
    },
    {
      metric: 'Tasks Completed per Second (Throughput)',
      tooltip: 'Number of completed tasks divided by total simulation time.',
      baseline: baseline.metrics.throughput.toFixed(4),
      proposed: proposed.metrics.throughput.toFixed(4),
      diff: `${comparison.throughputDifference >= 0 ? '+' : ''}${comparison.throughputDifference.toFixed(4)}`
    },
    {
      metric: 'Deadline Miss Rate (%)',
      tooltip: 'Percentage of tasks that finished after their deadlines.',
      baseline: `${baseStats.missedRate.toFixed(1)}%`,
      proposed: `${propStats.missedRate.toFixed(1)}%`,
      diff: `${missRateDiff >= 0 ? '+' : ''}${missRateDiff.toFixed(1)}%`
    },
    {
      metric: 'VM Usage (Resource Utilization, %)',
      tooltip: 'Percentage of available VM time used for executing tasks.',
      baseline: `${baseline.metrics.resourceUtilization.toFixed(1)}%`,
      proposed: `${proposed.metrics.resourceUtilization.toFixed(1)}%`,
      diff: `${comparison.resourceUtilizationDifference >= 0 ? '+' : ''}${comparison.resourceUtilizationDifference.toFixed(1)}%`
    }
  ];

  return (
    <div className="card">
      <div className="card-title">
        <span style={{ fontSize: '1.2rem', fontWeight: 700, color: '#f8fafc' }}>
          Scheduler Comparison
        </span>
      </div>
      <p className="card-subtitle" style={{ margin: '4px 0 10px' }}>
        Both schedulers were evaluated using the same task workload.
      </p>

      {/* Difference Explanation */}
      <div style={{
        padding: '8px 12px',
        backgroundColor: '#0f172a',
        borderRadius: '6px',
        fontSize: '0.82rem',
        color: '#94a3b8',
        marginBottom: '14px',
        border: '1px solid #334155'
      }}>
        ℹ️ <strong>Difference:</strong> Shows how the Deadline-Aware result differs from the Standard Priority result (<em>Deadline-Aware − Standard Priority</em>).
      </div>

      <div className="table-responsive">
        <table>
          <thead>
            <tr>
              <th>Performance Metric</th>
              <th style={{ color: '#3b82f6' }}>Standard Priority</th>
              <th style={{ color: '#06b6d4' }}>Deadline-Aware</th>
              <th>Difference (Deadline-Aware − Standard Priority)</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r, i) => (
              <tr key={i}>
                <td style={{ fontWeight: 500 }} title={r.tooltip}>
                  {r.metric} ⓘ
                </td>
                <td style={{ fontFamily: 'var(--font-mono)' }}>{r.baseline}</td>
                <td style={{ fontFamily: 'var(--font-mono)' }}>{r.proposed}</td>
                <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>{r.diff}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
