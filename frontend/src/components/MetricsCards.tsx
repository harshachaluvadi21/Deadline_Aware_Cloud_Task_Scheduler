import React from 'react';
import { MetricsDto, TaskResultDto } from '../types/simulation';
import { computeDeadlineStats } from './DeadlineAnalysisView';
import { AnimatedCounter } from './AnimatedCounter';

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

  const cards: Array<{
    label: string;
    value: React.ReactNode;
    sub: string;
    color: string;
    title: string;
  }> = [
    {
      label: 'Makespan',
      value: <AnimatedCounter value={metrics.makespan} decimals={2} suffix=" s" />,
      sub: 'Total completion time',
      color: 'var(--cyan)',
      title: 'Time from simulation start until all tasks finish.',
    },
    {
      label: 'Avg. Waiting Time',
      value: <AnimatedCounter value={metrics.averageWaitingTime} decimals={2} suffix=" s" />,
      sub: 'Queue delay per task',
      color: 'var(--text-primary)',
      title: 'Average time tasks waited before execution began.',
    },
    {
      label: 'Avg. Turnaround',
      value: <AnimatedCounter value={metrics.averageTurnaroundTime} decimals={2} suffix=" s" />,
      sub: 'Wait + execution time',
      color: 'var(--text-primary)',
      title: 'Average time from task arrival until completion.',
    },
    {
      label: 'Throughput',
      value: <AnimatedCounter value={metrics.throughput} decimals={4} />,
      sub: 'Tasks / second',
      color: 'var(--text-primary)',
      title: 'Completed tasks divided by total simulation time.',
    },
    {
      label: 'Deadline Miss Rate',
      value: <AnimatedCounter value={missRate} decimals={1} suffix="%" />,
      sub: `${missedCount} of ${totalTasks} missed`,
      color: missedCount > 0 ? 'var(--danger)' : 'var(--success)',
      title: 'Percentage of tasks that finished after their deadline.',
    },
    {
      label: 'VM Utilization',
      value: <AnimatedCounter value={metrics.resourceUtilization} decimals={1} suffix="%" />,
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
