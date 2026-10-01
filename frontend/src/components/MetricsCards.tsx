import React from 'react';
import { MetricsDto, TaskResultDto } from '../types/simulation';
import { computeDeadlineStats } from './DeadlineAnalysisView';

interface MetricsCardsProps {
  metrics: MetricsDto;
  tasks?: TaskResultDto[];
  title?: string;
  schedulerName?: string;
}

export const MetricsCards: React.FC<MetricsCardsProps> = ({ metrics, tasks, title, schedulerName }) => {
  const stats = tasks && tasks.length > 0 ? computeDeadlineStats(tasks) : null;
  const missedCount = stats ? stats.missedCount : metrics.deadlineMissedCount;
  const totalTasks = tasks?.length ?? metrics.completedTaskCount;
  const missRate = stats
    ? stats.missedRate
    : (metrics.deadlineMissRate > 1 ? metrics.deadlineMissRate : metrics.deadlineMissRate * 100);

  const cards = [
    {
      label: 'Makespan',
      value: `${metrics.makespan.toFixed(2)} s`,
      sub: 'Total completion time',
      color: 'var(--cyan)',
      title: 'Time from simulation start until all tasks finish.',
    },
    {
      label: 'Avg. Waiting Time',
      value: `${metrics.averageWaitingTime.toFixed(2)} s`,
      sub: 'Queue delay per task',
      color: 'var(--text-primary)',
      title: 'Average time tasks waited before execution began.',
    },
    {
      label: 'Avg. Turnaround',
      value: `${metrics.averageTurnaroundTime.toFixed(2)} s`,
      sub: 'Wait + execution time',
      color: 'var(--text-primary)',
      title: 'Average time from task arrival until completion.',
    },
    {
      label: 'Throughput',
      value: metrics.throughput.toFixed(4),
      sub: 'Tasks / second',
      color: 'var(--text-primary)',
      title: 'Completed tasks divided by total simulation time.',
    },
    {
      label: 'Deadline Miss Rate',
      value: `${missRate.toFixed(1)}%`,
      sub: `${missedCount} of ${totalTasks} missed`,
      color: missedCount > 0 ? 'var(--danger)' : 'var(--success)',
      title: 'Percentage of tasks that finished after their deadline.',
    },
    {
      label: 'VM Utilization',
      value: `${metrics.resourceUtilization.toFixed(1)}%`,
      sub: 'Resource usage',
      color: 'var(--violet)',
      title: 'Fraction of available VM execution capacity used.',
    },
  ];

  return (
    <div className="card" style={{ marginBottom: 16 }}>
      <div className="card-title">
        <span>{title || 'Performance Metrics'}</span>
        {schedulerName && (
          <span className={`badge ${schedulerName === 'BASELINE' ? 'badge-baseline' : 'badge-proposed'}`}>
            {schedulerName === 'BASELINE' ? 'Standard Priority' : 'Deadline-Aware'}
          </span>
        )}
      </div>
      <div className="metrics-grid">
        {cards.map(c => (
          <div className="metric-card" key={c.label} title={c.title}>
            <div className="metric-card-label">{c.label}</div>
            <div className="metric-card-value" style={{ color: c.color }}>{c.value}</div>
            <div className="metric-card-unit">{c.sub}</div>
          </div>
        ))}
      </div>
    </div>
  );
};
