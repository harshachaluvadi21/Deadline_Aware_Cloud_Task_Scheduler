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
    : { missedRate: baseline.metrics.deadlineMissRate > 1 ? baseline.metrics.deadlineMissRate : baseline.metrics.deadlineMissRate * 100 };
  const propStats = proposed.tasks && proposed.tasks.length > 0
    ? computeDeadlineStats(proposed.tasks)
    : { missedRate: proposed.metrics.deadlineMissRate > 1 ? proposed.metrics.deadlineMissRate : proposed.metrics.deadlineMissRate * 100 };
  const missRateDiff = propStats.missedRate - baseStats.missedRate;

  // For metrics where lower is better: negative diff is better for proposed
  // For throughput: higher is better, so positive diff is better
  const rows = [
    {
      metric: 'Makespan', tooltip: 'Total time from start until all tasks finish.',
      baseline: `${baseline.metrics.makespan.toFixed(2)} s`,
      proposed: `${proposed.metrics.makespan.toFixed(2)} s`,
      rawDiff: comparison.makespanDifference, lowerIsBetter: true,
      diff: `${comparison.makespanDifference >= 0 ? '+' : ''}${comparison.makespanDifference.toFixed(2)} s`,
    },
    {
      metric: 'Avg. Waiting Time', tooltip: 'Average time tasks waited before execution.',
      baseline: `${baseline.metrics.averageWaitingTime.toFixed(2)} s`,
      proposed: `${proposed.metrics.averageWaitingTime.toFixed(2)} s`,
      rawDiff: comparison.waitingTimeDifference, lowerIsBetter: true,
      diff: `${comparison.waitingTimeDifference >= 0 ? '+' : ''}${comparison.waitingTimeDifference.toFixed(2)} s`,
    },
    {
      metric: 'Avg. Turnaround Time', tooltip: 'Average from task arrival to completion.',
      baseline: `${baseline.metrics.averageTurnaroundTime.toFixed(2)} s`,
      proposed: `${proposed.metrics.averageTurnaroundTime.toFixed(2)} s`,
      rawDiff: comparison.turnaroundDifference, lowerIsBetter: true,
      diff: `${comparison.turnaroundDifference >= 0 ? '+' : ''}${comparison.turnaroundDifference.toFixed(2)} s`,
    },
    {
      metric: 'Throughput', tooltip: 'Tasks completed per unit of simulation time.',
      baseline: baseline.metrics.throughput.toFixed(4),
      proposed: proposed.metrics.throughput.toFixed(4),
      rawDiff: comparison.throughputDifference, lowerIsBetter: false,
      diff: `${comparison.throughputDifference >= 0 ? '+' : ''}${comparison.throughputDifference.toFixed(4)}`,
    },
    {
      metric: 'Deadline Miss Rate', tooltip: 'Percentage of tasks that finished after their deadline.',
      baseline: `${baseStats.missedRate.toFixed(1)}%`,
      proposed: `${propStats.missedRate.toFixed(1)}%`,
      rawDiff: missRateDiff, lowerIsBetter: true,
      diff: `${missRateDiff >= 0 ? '+' : ''}${missRateDiff.toFixed(1)}%`,
    },
    {
      metric: 'VM Utilization', tooltip: 'Percentage of VM capacity used during the simulation.',
      baseline: `${baseline.metrics.resourceUtilization.toFixed(1)}%`,
      proposed: `${proposed.metrics.resourceUtilization.toFixed(1)}%`,
      rawDiff: comparison.resourceUtilizationDifference, lowerIsBetter: false,
      diff: `${comparison.resourceUtilizationDifference >= 0 ? '+' : ''}${comparison.resourceUtilizationDifference.toFixed(1)}%`,
    },
  ];

  const getDiffClass = (rawDiff: number, lowerIsBetter: boolean) => {
    if (Math.abs(rawDiff) < 0.0001) return 'neutral';
    const proposedIsBetter = lowerIsBetter ? rawDiff < 0 : rawDiff > 0;
    return proposedIsBetter ? 'better' : 'worse';
  };

  return (
    <div className="comparison-table-wrap" style={{ marginBottom: 20 }}>
      <div style={{ padding: '18px 20px', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <div className="card-title" style={{ marginBottom: 4 }}>Metrics Comparison Table</div>
          <div className="card-subtitle" style={{ marginBottom: 0 }}>
            Both schedulers ran on identical workloads. Difference = Deadline-Aware − Standard Priority.
          </div>
        </div>
      </div>
      <table className="comparison-table">
        <thead>
          <tr>
            <th>Metric</th>
            <th style={{ color: 'var(--baseline-color)' }}>Standard Priority</th>
            <th style={{ color: 'var(--proposed-color)' }}>Deadline-Aware</th>
            <th>Difference</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={i}>
              <td title={r.tooltip}>{r.metric}</td>
              <td className="mono">{r.baseline}</td>
              <td className="mono">{r.proposed}</td>
              <td className={`mono ${getDiffClass(r.rawDiff, r.lowerIsBetter)}`}>{r.diff}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
