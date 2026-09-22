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

  return (
    <div className="card">
      <div className="card-title">
        <span>{title || 'Performance Metrics'}</span>
        {schedulerName && (
          <span className="badge badge-info" style={{ textTransform: 'uppercase' }}>
            {schedulerName}
          </span>
        )}
      </div>

      <div className="metrics-grid">
        <div className="metric-card" title="Time from the start of the simulation until all tasks finish.">
          <span className="metric-label">Total Completion Time ⓘ</span>
          <span className="metric-value">{metrics.makespan.toFixed(2)} s</span>
          <span className="metric-sub">Makespan</span>
        </div>

        <div className="metric-card" title="Average time tasks waited before execution.">
          <span className="metric-label">Average Waiting Time ⓘ</span>
          <span className="metric-value">{metrics.averageWaitingTime.toFixed(2)} s</span>
          <span className="metric-sub">Average Queue Delay</span>
        </div>

        <div className="metric-card" title="Average time from task arrival until completion.">
          <span className="metric-label">Average Turnaround Time ⓘ</span>
          <span className="metric-value">{metrics.averageTurnaroundTime.toFixed(2)} s</span>
          <span className="metric-sub">Wait + Execution Time</span>
        </div>

        <div className="metric-card" title="Number of completed tasks divided by total simulation time.">
          <span className="metric-label">Tasks Completed / Second ⓘ</span>
          <span className="metric-value">{metrics.throughput.toFixed(4)}</span>
          <span className="metric-sub">Throughput</span>
        </div>

        <div className="metric-card" title="Percentage of tasks that finished after their deadlines.">
          <span className="metric-label">Deadline Miss Rate ⓘ</span>
          <span className="metric-value" style={{ color: missedCount > 0 ? 'var(--danger)' : 'var(--success)' }}>
            {missRate.toFixed(1)}%
          </span>
          <span className="metric-sub">{missedCount} of {totalTasks} tasks missed</span>
        </div>

        <div className="metric-card" title="Percentage of available VM time used for executing tasks.">
          <span className="metric-label">VM Usage ⓘ</span>
          <span className="metric-value" style={{ color: 'var(--accent-primary)' }}>
            {metrics.resourceUtilization.toFixed(1)}%
          </span>
          <span className="metric-sub">Time-based Resource Utilization</span>
        </div>
      </div>
    </div>
  );
};
