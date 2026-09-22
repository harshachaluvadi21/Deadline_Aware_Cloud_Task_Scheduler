import React from 'react';
import { MetricsDto, TaskResultDto } from '../types/simulation';
import { computeDeadlineStats } from './DeadlineAnalysisView';

interface SimulationOverviewProps {
  baselineTasks?: TaskResultDto[];
  proposedTasks?: TaskResultDto[];
  singleTasks?: TaskResultDto[];
  baselineMetrics?: MetricsDto;
  proposedMetrics?: MetricsDto;
  singleMetrics?: MetricsDto;
  singleAlgorithmName?: string;
}

export const SimulationOverview: React.FC<SimulationOverviewProps> = ({
  baselineTasks,
  proposedTasks,
  singleTasks,
  baselineMetrics,
  proposedMetrics,
  singleMetrics,
  singleAlgorithmName
}) => {
  const isCompare = !!(baselineMetrics && proposedMetrics);
  const algoLabel = singleAlgorithmName === 'PROPOSED' ? 'Deadline-Aware' : 'Standard Priority';

  // Task-level status and deadline classification as the single source of truth for deadline stats
  const baseStats = baselineTasks && baselineTasks.length > 0
    ? computeDeadlineStats(baselineTasks)
    : {
        missedCount: baselineMetrics?.deadlineMissedCount ?? 0,
        missedRate: baselineMetrics ? (baselineMetrics.deadlineMissRate > 1 ? baselineMetrics.deadlineMissRate : baselineMetrics.deadlineMissRate * 100) : 0,
        total: baselineMetrics?.completedTaskCount ?? 0
      };

  const propStats = proposedTasks && proposedTasks.length > 0
    ? computeDeadlineStats(proposedTasks)
    : {
        missedCount: proposedMetrics?.deadlineMissedCount ?? 0,
        missedRate: proposedMetrics ? (proposedMetrics.deadlineMissRate > 1 ? proposedMetrics.deadlineMissRate : proposedMetrics.deadlineMissRate * 100) : 0,
        total: proposedMetrics?.completedTaskCount ?? 0
      };

  const singleStats = singleTasks && singleTasks.length > 0
    ? computeDeadlineStats(singleTasks)
    : {
        missedCount: singleMetrics?.deadlineMissedCount ?? 0,
        missedRate: singleMetrics ? (singleMetrics.deadlineMissRate > 1 ? singleMetrics.deadlineMissRate : singleMetrics.deadlineMissRate * 100) : 0,
        total: singleMetrics?.completedTaskCount ?? 0
      };

  return (
    <div style={{ margin: '20px 0' }}>
      <div style={{ marginBottom: '12px' }}>
        <h3 style={{ fontSize: '1.15rem', color: '#f8fafc', margin: 0, fontWeight: 700 }}>
          Simulation Overview
        </h3>
        <p style={{ margin: '4px 0 0', fontSize: '0.85rem', color: '#94a3b8' }}>
          {isCompare
            ? 'High-level operational summary comparing both scheduling approaches.'
            : `High-level operational summary for ${algoLabel} scheduler.`}
        </p>
      </div>

      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
        gap: '16px'
      }}>
        {/* Card 1: Tasks Completed */}
        <div style={{
          background: '#1e293b',
          border: '1px solid #334155',
          borderRadius: '8px',
          padding: '16px',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between'
        }}>
          <div style={{ fontSize: '0.82rem', color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em', fontWeight: 600 }}>
            Tasks Completed
          </div>
          {isCompare ? (
            <div style={{ marginTop: '8px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', color: '#cbd5e1', marginBottom: '4px' }}>
                <span>Standard:</span>
                <span style={{ fontWeight: 700, color: '#f8fafc' }}>
                  {baselineTasks?.length ?? baselineMetrics!.completedTaskCount}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', color: '#cbd5e1' }}>
                <span>Deadline-Aware:</span>
                <span style={{ fontWeight: 700, color: '#38bdf8' }}>
                  {proposedTasks?.length ?? proposedMetrics!.completedTaskCount}
                </span>
              </div>
            </div>
          ) : (
            <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#38bdf8', marginTop: '8px' }}>
              {singleTasks?.length ?? singleMetrics?.completedTaskCount ?? 0}
            </div>
          )}
          <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '6px' }}>
            Total cloudlets executed
          </div>
        </div>

        {/* Card 2: Deadline Misses */}
        <div style={{
          background: '#1e293b',
          border: '1px solid #334155',
          borderRadius: '8px',
          padding: '16px',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between'
        }}>
          <div style={{ fontSize: '0.82rem', color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em', fontWeight: 600 }}>
            Deadline Misses
          </div>
          {isCompare ? (
            <div style={{ marginTop: '8px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', color: '#cbd5e1', marginBottom: '4px' }}>
                <span>Standard:</span>
                <span style={{
                  fontWeight: 700,
                  color: baseStats.missedCount > 0 ? '#f87171' : '#4ade80'
                }}>
                  {baseStats.missedCount} ({baseStats.missedRate.toFixed(1)}%)
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', color: '#cbd5e1' }}>
                <span>Deadline-Aware:</span>
                <span style={{
                  fontWeight: 700,
                  color: propStats.missedCount > 0 ? '#f87171' : '#4ade80'
                }}>
                  {propStats.missedCount} ({propStats.missedRate.toFixed(1)}%)
                </span>
              </div>
            </div>
          ) : (
            <div style={{
              fontSize: '1.75rem',
              fontWeight: 700,
              color: singleStats.missedCount > 0 ? '#f87171' : '#4ade80',
              marginTop: '8px'
            }}>
              {singleStats.missedCount}
              <span style={{ fontSize: '0.9rem', fontWeight: 400, marginLeft: '6px', color: '#94a3b8' }}>
                ({singleStats.missedRate.toFixed(1)}%)
              </span>
            </div>
          )}
          <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '6px' }}>
            Tasks finished past deadline
          </div>
        </div>

        {/* Card 3: Total Completion Time */}
        <div style={{
          background: '#1e293b',
          border: '1px solid #334155',
          borderRadius: '8px',
          padding: '16px',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between'
        }}>
          <div style={{ fontSize: '0.82rem', color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em', fontWeight: 600 }}>
            Total Completion Time
          </div>
          {isCompare ? (
            <div style={{ marginTop: '8px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', color: '#cbd5e1', marginBottom: '4px' }}>
                <span>Standard:</span>
                <span style={{ fontWeight: 700, color: '#f8fafc' }}>
                  {baselineMetrics!.makespan.toFixed(2)} s
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', color: '#cbd5e1' }}>
                <span>Deadline-Aware:</span>
                <span style={{ fontWeight: 700, color: '#38bdf8' }}>
                  {proposedMetrics!.makespan.toFixed(2)} s
                </span>
              </div>
            </div>
          ) : (
            <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#f8fafc', marginTop: '8px' }}>
              {singleMetrics ? `${singleMetrics.makespan.toFixed(2)} s` : '0.00 s'}
            </div>
          )}
          <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '6px' }}>
            Simulation makespan duration
          </div>
        </div>

        {/* Card 4: VM Usage */}
        <div style={{
          background: '#1e293b',
          border: '1px solid #334155',
          borderRadius: '8px',
          padding: '16px',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between'
        }}>
          <div style={{ fontSize: '0.82rem', color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em', fontWeight: 600 }}>
            VM Usage
          </div>
          {isCompare ? (
            <div style={{ marginTop: '8px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', color: '#cbd5e1', marginBottom: '4px' }}>
                <span>Standard:</span>
                <span style={{ fontWeight: 700, color: '#f8fafc' }}>
                  {baselineMetrics!.resourceUtilization.toFixed(1)}%
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', color: '#cbd5e1' }}>
                <span>Deadline-Aware:</span>
                <span style={{ fontWeight: 700, color: '#38bdf8' }}>
                  {proposedMetrics!.resourceUtilization.toFixed(1)}%
                </span>
              </div>
            </div>
          ) : (
            <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#38bdf8', marginTop: '8px' }}>
              {singleMetrics ? `${singleMetrics.resourceUtilization.toFixed(1)}%` : '0.0%'}
            </div>
          )}
          <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '6px' }}>
            Cluster busy time ratio
          </div>
        </div>

      </div>
    </div>
  );
};
