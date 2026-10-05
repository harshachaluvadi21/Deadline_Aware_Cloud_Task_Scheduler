import React from 'react';
import { MetricsDto, TaskResultDto } from '../types/simulation';
import { computeDeadlineStats } from './DeadlineAnalysisView';

interface InsightsBannerProps {
  isCompare: boolean;
  baselineMetrics?: MetricsDto;
  proposedMetrics?: MetricsDto;
  singleMetrics?: MetricsDto;
  singleAlgorithm?: string;
  baselineTasks?: TaskResultDto[];
  proposedTasks?: TaskResultDto[];
  singleTasks?: TaskResultDto[];
}

export const InsightsBanner: React.FC<InsightsBannerProps> = ({
  isCompare,
  baselineMetrics,
  proposedMetrics,
  singleMetrics,
  singleAlgorithm,
  baselineTasks,
  proposedTasks,
  singleTasks,
}) => {
  const insights: string[] = [];

  if (isCompare && baselineMetrics && proposedMetrics) {
    const bMiss = baselineTasks && baselineTasks.length > 0
      ? computeDeadlineStats(baselineTasks).missedRate : baselineMetrics.deadlineMissRate;
    const pMiss = proposedTasks && proposedTasks.length > 0
      ? computeDeadlineStats(proposedTasks).missedRate : proposedMetrics.deadlineMissRate;

    // Waiting time insight
    const waitDiff = proposedMetrics.averageWaitingTime - baselineMetrics.averageWaitingTime;
    if (waitDiff < -0.01) {
      const pct = Math.abs(waitDiff / baselineMetrics.averageWaitingTime * 100).toFixed(0);
      insights.push(`Deadline-Aware achieved **${pct}% faster** average queue response time (${proposedMetrics.averageWaitingTime.toFixed(2)}s vs ${baselineMetrics.averageWaitingTime.toFixed(2)}s).`);
    } else if (waitDiff > 0.01) {
      insights.push(`Standard Priority had a slightly faster average waiting time by ${waitDiff.toFixed(2)}s.`);
    } else {
      insights.push(`Both schedulers achieved nearly identical average waiting times.`);
    }

    // Deadline miss insight
    const missDiff = pMiss - bMiss;
    if (Math.abs(missDiff) < 0.5) {
      insights.push(`Both algorithms achieved the same deadline miss rate of **${bMiss.toFixed(1)}%**.`);
    } else if (missDiff < 0) {
      insights.push(`Deadline-Aware reduced deadline misses from **${bMiss.toFixed(1)}%** to **${pMiss.toFixed(1)}%** — a ${Math.abs(missDiff).toFixed(1)} percentage point improvement.`);
    } else {
      insights.push(`Standard Priority had a lower miss rate (**${bMiss.toFixed(1)}%** vs **${pMiss.toFixed(1)}%**) — a trade-off of deadline-urgency reordering in this workload.`);
    }

    // VM utilization insight
    const utilDiff = Math.abs(proposedMetrics.resourceUtilization - baselineMetrics.resourceUtilization);
    if (utilDiff < 3) {
      insights.push(`VM cluster utilization remained balanced at ~**${((proposedMetrics.resourceUtilization + baselineMetrics.resourceUtilization) / 2).toFixed(1)}%** across both approaches.`);
    }
  } else if (singleMetrics) {
    const algoName = singleAlgorithm === 'PROPOSED' ? 'Deadline-Aware' : 'Standard Priority';
    const missRate = singleTasks && singleTasks.length > 0
      ? computeDeadlineStats(singleTasks).missedRate : singleMetrics.deadlineMissRate;
    insights.push(`The ${algoName} scheduler completed all tasks in **${singleMetrics.makespan.toFixed(2)}s** with a **${missRate.toFixed(1)}%** deadline miss rate and **${singleMetrics.resourceUtilization.toFixed(1)}%** VM utilization.`);
  }

  if (insights.length === 0) return null;

  const renderText = (text: string) => {
    const parts = text.split(/\*\*(.*?)\*\*/g);
    return parts.map((part, idx) =>
      idx % 2 === 1
        ? <strong key={idx}>{part}</strong>
        : <span key={idx}>{part}</span>
    );
  };

  return (
    <div className="insights-banner fade-in">
      <div className="insights-icon">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="16" x2="12" y2="12" />
          <line x1="12" y1="8" x2="12.01" y2="8" />
        </svg>
      </div>
      <div>
        <div style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.82rem', marginBottom: 4 }}>
          Quick Insights
        </div>
        {insights.map((text, idx) => (
          <div key={idx} style={{ marginBottom: idx < insights.length - 1 ? 4 : 0 }}>
            {renderText(text)}
          </div>
        ))}
      </div>
    </div>
  );
};
