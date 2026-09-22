import React from 'react';
import { MetricsDto, TaskResultDto } from '../types/simulation';
import { computeDeadlineStats } from './DeadlineAnalysisView';

interface MetricsChartsProps {
  baselineMetrics?: MetricsDto;
  proposedMetrics?: MetricsDto;
  singleMetrics?: MetricsDto;
  baselineTasks?: TaskResultDto[];
  proposedTasks?: TaskResultDto[];
  singleTasks?: TaskResultDto[];
  singleAlgorithmName?: string;
}

interface MetricItem {
  name: string;
  unit: string;
  baseline: number;
  proposed: number;
  format: (v: number) => string;
}

export const MetricsCharts: React.FC<MetricsChartsProps> = ({
  baselineMetrics,
  proposedMetrics,
  singleMetrics,
  baselineTasks,
  proposedTasks,
  singleTasks,
  singleAlgorithmName
}) => {
  const isComparison = !!(baselineMetrics && proposedMetrics);

  const baseMissRate = baselineTasks && baselineTasks.length > 0
    ? computeDeadlineStats(baselineTasks).missedRate
    : (baselineMetrics ? (baselineMetrics.deadlineMissRate > 1 ? baselineMetrics.deadlineMissRate : baselineMetrics.deadlineMissRate * 100) : 0);

  const propMissRate = proposedTasks && proposedTasks.length > 0
    ? computeDeadlineStats(proposedTasks).missedRate
    : (proposedMetrics ? (proposedMetrics.deadlineMissRate > 1 ? proposedMetrics.deadlineMissRate : proposedMetrics.deadlineMissRate * 100) : 0);

  const singleMissRate = singleTasks && singleTasks.length > 0
    ? computeDeadlineStats(singleTasks).missedRate
    : (singleMetrics ? (singleMetrics.deadlineMissRate > 1 ? singleMetrics.deadlineMissRate : singleMetrics.deadlineMissRate * 100) : 0);

  const metricsList: MetricItem[] = isComparison
    ? [
        {
          name: 'Total Completion Time (Makespan)',
          unit: 's',
          baseline: baselineMetrics.makespan,
          proposed: proposedMetrics.makespan,
          format: (v) => `${v.toFixed(2)} s`
        },
        {
          name: 'Average Waiting Time',
          unit: 's',
          baseline: baselineMetrics.averageWaitingTime,
          proposed: proposedMetrics.averageWaitingTime,
          format: (v) => `${v.toFixed(2)} s`
        },
        {
          name: 'Average Turnaround Time',
          unit: 's',
          baseline: baselineMetrics.averageTurnaroundTime,
          proposed: proposedMetrics.averageTurnaroundTime,
          format: (v) => `${v.toFixed(2)} s`
        },
        {
          name: 'Tasks Completed per Second (Throughput)',
          unit: 'tasks/s',
          baseline: baselineMetrics.throughput,
          proposed: proposedMetrics.throughput,
          format: (v) => `${v.toFixed(4)} tasks/s`
        },
        {
          name: 'Deadline Miss Rate',
          unit: '%',
          baseline: baseMissRate,
          proposed: propMissRate,
          format: (v) => `${v.toFixed(1)}%`
        },
        {
          name: 'VM Usage (Resource Utilization)',
          unit: '%',
          baseline: baselineMetrics.resourceUtilization,
          proposed: proposedMetrics.resourceUtilization,
          format: (v) => `${v.toFixed(1)}%`
        }
      ]
    : singleMetrics
    ? [
        {
          name: 'Total Completion Time (Makespan)',
          unit: 's',
          baseline: 0,
          proposed: singleMetrics.makespan,
          format: (v) => `${v.toFixed(2)} s`
        },
        {
          name: 'Average Waiting Time',
          unit: 's',
          baseline: 0,
          proposed: singleMetrics.averageWaitingTime,
          format: (v) => `${v.toFixed(2)} s`
        },
        {
          name: 'Average Turnaround Time',
          unit: 's',
          baseline: 0,
          proposed: singleMetrics.averageTurnaroundTime,
          format: (v) => `${v.toFixed(2)} s`
        },
        {
          name: 'Tasks Completed per Second (Throughput)',
          unit: 'tasks/s',
          baseline: 0,
          proposed: singleMetrics.throughput,
          format: (v) => `${v.toFixed(4)} tasks/s`
        },
        {
          name: 'Deadline Miss Rate',
          unit: '%',
          baseline: 0,
          proposed: singleMissRate,
          format: (v) => `${v.toFixed(1)}%`
        },
        {
          name: 'VM Usage (Resource Utilization)',
          unit: '%',
          baseline: 0,
          proposed: singleMetrics.resourceUtilization,
          format: (v) => `${v.toFixed(1)}%`
        }
      ]
    : [];

  if (metricsList.length === 0) return null;

  return (
    <div className="card" style={{ marginTop: '24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <h3 style={{ margin: 0, fontSize: '1.15rem', color: '#f8fafc' }}>
          Visual Comparison of Key Metrics
        </h3>
        {isComparison ? (
          <div style={{ display: 'flex', gap: '16px', fontSize: '0.85rem' }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ display: 'inline-block', width: '12px', height: '12px', backgroundColor: '#3b82f6', borderRadius: '2px' }} />
              Standard Priority
            </span>
            <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ display: 'inline-block', width: '12px', height: '12px', backgroundColor: '#06b6d4', borderRadius: '2px' }} />
              Deadline-Aware
            </span>
          </div>
        ) : (
          <span style={{ fontSize: '0.85rem', color: '#94a3b8' }}>
            Scheduler: <strong style={{ color: '#38bdf8' }}>{singleAlgorithmName === 'PROPOSED' ? 'Deadline-Aware' : 'Standard Priority'}</strong>
          </span>
        )}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '18px' }}>
        {metricsList.map((m, idx) => {
          const maxVal = Math.max(m.baseline, m.proposed, 0.0001);
          const baselinePct = (m.baseline / maxVal) * 100;
          const proposedPct = (m.proposed / maxVal) * 100;
          const diff = m.proposed - m.baseline;

          return (
            <div
              key={idx}
              style={{
                background: '#0f172a',
                padding: '14px 16px',
                borderRadius: '8px',
                border: '1px solid #1e293b'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', fontSize: '0.88rem' }}>
                <span style={{ fontWeight: 600, color: '#e2e8f0' }}>{m.name}</span>
                {isComparison && (
                  <span style={{ fontSize: '0.8rem', color: '#94a3b8' }}>
                    Diff: <strong style={{ color: diff < 0 ? '#38bdf8' : '#e2e8f0' }}>
                      {diff >= 0 ? `+${diff.toFixed(2)}` : diff.toFixed(2)} {m.unit}
                    </strong>
                  </span>
                )}
              </div>

              {isComparison ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {/* Baseline bar */}
                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem', color: '#94a3b8', marginBottom: '2px' }}>
                      <span>Standard Priority</span>
                      <span>{m.format(m.baseline)}</span>
                    </div>
                    <div style={{ height: '10px', background: '#1e293b', borderRadius: '5px', overflow: 'hidden' }}>
                      <div
                        style={{
                          width: `${baselinePct}%`,
                          height: '100%',
                          backgroundColor: '#3b82f6',
                          transition: 'width 0.4s ease'
                        }}
                      />
                    </div>
                  </div>

                  {/* Proposed bar */}
                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem', color: '#94a3b8', marginBottom: '2px' }}>
                      <span>Deadline-Aware</span>
                      <span>{m.format(m.proposed)}</span>
                    </div>
                    <div style={{ height: '10px', background: '#1e293b', borderRadius: '5px', overflow: 'hidden' }}>
                      <div
                        style={{
                          width: `${proposedPct}%`,
                          height: '100%',
                          backgroundColor: '#06b6d4',
                          transition: 'width 0.4s ease'
                        }}
                      />
                    </div>
                  </div>
                </div>
              ) : (
                /* Single algorithm bar */
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', color: '#f8fafc', marginBottom: '6px' }}>
                    <span>Observed Value</span>
                    <strong style={{ color: '#38bdf8' }}>{m.format(m.proposed)}</strong>
                  </div>
                  <div style={{ height: '12px', background: '#1e293b', borderRadius: '6px', overflow: 'hidden' }}>
                    <div
                      style={{
                        width: '100%',
                        height: '100%',
                        backgroundColor: '#38bdf8'
                      }}
                    />
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};
