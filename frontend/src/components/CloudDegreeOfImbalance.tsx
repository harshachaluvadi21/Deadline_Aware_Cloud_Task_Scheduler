import React, { useState } from 'react';
import { TaskResultDto } from '../types/simulation';

interface CloudDegreeOfImbalanceProps {
  isCompare: boolean;
  baselineTasks?: TaskResultDto[];
  proposedTasks?: TaskResultDto[];
  singleTasks?: TaskResultDto[];
  singleAlgorithm?: string;
  vmCount?: number;
}

interface VmLoadStats {
  vmId: number;
  totalTime: number;
  taskCount: number;
  percentage: number;
  status: 'overloaded' | 'balanced' | 'underutilized';
}

interface DiCalculation {
  vmLoads: VmLoadStats[];
  tMax: number;
  tMin: number;
  tAvg: number;
  totalWorkload: number;
  di: number;
  stdDev: number;
}

export const CloudDegreeOfImbalance: React.FC<CloudDegreeOfImbalanceProps> = ({
  isCompare,
  baselineTasks,
  proposedTasks,
  singleTasks,
  singleAlgorithm = 'Current Scheduler',
  vmCount = 4,
}) => {
  const [isExpanded, setIsExpanded] = useState<boolean>(true);

  const calculateDI = (tasks?: TaskResultDto[]): DiCalculation | null => {
    if (!tasks || tasks.length === 0) return null;

    const loads: Record<number, { totalTime: number; taskCount: number }> = {};
    for (let i = 0; i < vmCount; i++) {
      loads[i] = { totalTime: 0, taskCount: 0 };
    }

    tasks.forEach(t => {
      const vmId = t.assignedVmId;
      if (loads[vmId] !== undefined) {
        loads[vmId].totalTime += t.executionTime;
        loads[vmId].taskCount += 1;
      }
    });

    const times = Object.values(loads).map(l => l.totalTime);
    const totalWorkload = times.reduce((acc, curr) => acc + curr, 0);
    const tMax = Math.max(...times);
    const tMin = Math.min(...times);
    const tAvg = totalWorkload / (vmCount || 1);

    // Degree of Imbalance = (T_max - T_min) / T_avg
    const di = tAvg > 0 ? (tMax - tMin) / tAvg : 0;

    // Standard deviation
    const variance = times.reduce((acc, curr) => acc + Math.pow(curr - tAvg, 2), 0) / (vmCount || 1);
    const stdDev = Math.sqrt(variance);

    const vmLoads: VmLoadStats[] = Object.entries(loads).map(([idStr, load]) => {
      const vmId = Number(idStr);
      const percentage = totalWorkload > 0 ? (load.totalTime / totalWorkload) * 100 : 25;
      let status: 'overloaded' | 'balanced' | 'underutilized' = 'balanced';
      if (load.totalTime > tAvg * 1.25 && tAvg > 0) status = 'overloaded';
      else if (load.totalTime < tAvg * 0.75 && tAvg > 0) status = 'underutilized';

      return {
        vmId,
        totalTime: load.totalTime,
        taskCount: load.taskCount,
        percentage,
        status,
      };
    });

    return {
      vmLoads,
      tMax,
      tMin,
      tAvg,
      totalWorkload,
      di,
      stdDev,
    };
  };

  const baselineDI = calculateDI(baselineTasks);
  const proposedDI = calculateDI(proposedTasks);
  const singleDI = calculateDI(singleTasks);

  const getDiColor = (di: number) => {
    if (di <= 0.3) return '#10b981'; // excellent balance
    if (di <= 0.7) return '#3b82f6'; // good
    if (di <= 1.2) return '#f59e0b'; // moderate imbalance
    return '#ef4444'; // severe imbalance / hot-spotting
  };

  const getDiBadge = (di: number) => {
    if (di <= 0.3) return { text: 'Optimal Equilibrium', color: '#10b981' };
    if (di <= 0.7) return { text: 'Moderately Balanced', color: '#3b82f6' };
    if (di <= 1.2) return { text: 'Uneven Distribution', color: '#f59e0b' };
    return { text: 'Severe Hotspotting', color: '#ef4444' };
  };

  const renderVmBar = (load: VmLoadStats, maxTime: number) => {
    const widthPct = maxTime > 0 ? Math.min(100, Math.max(8, (load.totalTime / maxTime) * 100)) : 10;
    const barColor =
      load.status === 'overloaded'
        ? '#ef4444'
        : load.status === 'underutilized'
        ? '#f59e0b'
        : '#10b981';

    return (
      <div key={load.vmId} style={{ marginBottom: 10 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', marginBottom: 4 }}>
          <span style={{ fontWeight: 600 }}>VM {load.vmId}</span>
          <span style={{ color: 'var(--text-secondary)' }}>
            {load.totalTime.toFixed(1)}s ({load.percentage.toFixed(1)}% of load) · {load.taskCount} tasks
          </span>
        </div>
        <div
          style={{
            height: 10,
            borderRadius: 5,
            background: 'var(--border)',
            overflow: 'hidden',
            position: 'relative',
          }}
        >
          <div
            style={{
              height: '100%',
              width: `${widthPct}%`,
              background: barColor,
              borderRadius: 5,
              transition: 'width 0.4s ease',
            }}
          />
        </div>
      </div>
    );
  };

  const renderDiStatsCard = (diData: DiCalculation, title: string, subtitle?: string) => {
    const badge = getDiBadge(diData.di);
    const diColor = getDiColor(diData.di);

    return (
      <div
        style={{
          background: 'var(--bg-card)',
          border: '1px solid var(--border)',
          borderRadius: 8,
          padding: 16,
          flex: 1,
          minWidth: 280,
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
          <div>
            <div style={{ fontWeight: 600, fontSize: '0.95rem' }}>{title}</div>
            {subtitle && <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>{subtitle}</div>}
          </div>
          <span
            style={{
              fontSize: '0.72rem',
              fontWeight: 600,
              padding: '2px 8px',
              borderRadius: 12,
              background: `${badge.color}15`,
              color: badge.color,
              border: `1px solid ${badge.color}35`,
            }}
          >
            {badge.text}
          </span>
        </div>

        {/* DI Big Metric */}
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginBottom: 14 }}>
          <div style={{ fontSize: '2rem', fontWeight: 800, color: diColor, lineHeight: 1 }}>
            {diData.di.toFixed(3)}
          </div>
          <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
            DI Score (0.0 = perfect)
          </span>
        </div>

        {/* Breakdown Grid */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: 8,
            padding: '8px 10px',
            background: 'var(--bg-secondary)',
            borderRadius: 6,
            marginBottom: 16,
            fontSize: '0.75rem',
            textAlign: 'center',
          }}
        >
          <div>
            <div style={{ color: 'var(--text-secondary)' }}>T_max (Peak)</div>
            <div style={{ fontWeight: 700, marginTop: 2 }}>{diData.tMax.toFixed(1)}s</div>
          </div>
          <div>
            <div style={{ color: 'var(--text-secondary)' }}>T_avg (Mean)</div>
            <div style={{ fontWeight: 700, marginTop: 2 }}>{diData.tAvg.toFixed(1)}s</div>
          </div>
          <div>
            <div style={{ color: 'var(--text-secondary)' }}>T_min (Low)</div>
            <div style={{ fontWeight: 700, marginTop: 2 }}>{diData.tMin.toFixed(1)}s</div>
          </div>
        </div>

        {/* Per-VM Load Meters */}
        <div style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 8 }}>
          VM Workload Distribution (Active Busy Times)
        </div>
        <div>
          {diData.vmLoads.map(load => renderVmBar(load, diData.tMax))}
        </div>
      </div>
    );
  };

  return (
    <div className="card" style={{ marginBottom: 20 }}>
      {/* Header */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          cursor: 'pointer',
          userSelect: 'none',
        }}
        onClick={() => setIsExpanded(!isExpanded)}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div
            style={{
              width: 32,
              height: 32,
              borderRadius: 6,
              background: 'rgba(59, 130, 246, 0.1)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#3b82f6',
            }}
          >
            {/* Cluster Balance Scale SVG icon */}
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M16 16l3-8 3 8c-.87.65-1.92 1-3 1s-2.13-.35-3-1z" />
              <path d="M2 16l3-8 3 8c-.87.65-1.92 1-3 1s-2.13-.35-3-1z" />
              <path d="M7 21h10" />
              <path d="M12 3v18" />
              <path d="M3 7h2c2 0 5-1 7-2 2 1 5 2 7 2h2" />
            </svg>
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 600 }}>
                Cloud Load Balancing & Degree of Imbalance (DI)
              </h3>
              <span
                style={{
                  fontSize: '0.68rem',
                  padding: '2px 8px',
                  borderRadius: 4,
                  background: 'rgba(59, 130, 246, 0.1)',
                  color: '#3b82f6',
                  fontWeight: 600,
                  letterSpacing: '0.5px',
                }}
              >
                CLOUDSIM METRIC
              </span>
            </div>
            <p style={{ margin: 0, fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: 2 }}>
              Measures cluster workload equilibrium: <code style={{ fontSize: '0.75rem', background: 'var(--bg-secondary)', padding: '1px 4px', borderRadius: 3 }}>DI = (T_max - T_min) / T_avg</code>. Lower values prevent VM hotspots.
            </p>
          </div>
        </div>

        <button
          className="btn-ghost"
          style={{ padding: '4px 8px', fontSize: '0.8rem' }}
          aria-label={isExpanded ? 'Collapse' : 'Expand'}
        >
          {isExpanded ? '▲ Collapse' : '▼ Expand'}
        </button>
      </div>

      {isExpanded && (
        <div style={{ marginTop: 16 }}>
          {isCompare ? (
            <div>
              {/* Comparative Improvement Banner */}
              {baselineDI && proposedDI && (
                <div
                  style={{
                    background:
                      proposedDI.di < baselineDI.di
                        ? 'rgba(16, 185, 129, 0.08)'
                        : 'rgba(239, 68, 68, 0.08)',
                    border: `1px solid ${
                      proposedDI.di < baselineDI.di
                        ? 'rgba(16, 185, 129, 0.25)'
                        : 'rgba(239, 68, 68, 0.25)'
                    }`,
                    borderRadius: 6,
                    padding: '10px 14px',
                    marginBottom: 16,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <polyline points="22 7 13.5 15.5 8.5 10.5 2 17" />
                      <polyline points="16 7 22 7 22 13" />
                    </svg>
                    <span style={{ fontSize: '0.85rem' }}>
                      {proposedDI.di < baselineDI.di ? (
                        <>
                          <strong>Proposed Scheduler</strong> reduces Degree of Imbalance by{' '}
                          <strong style={{ color: '#10b981' }}>
                            {baselineDI.di > 0
                              ? (((baselineDI.di - proposedDI.di) / baselineDI.di) * 100).toFixed(1)
                              : '0'}
                            %
                          </strong>
                          , preventing VM queue bottlenecks and idle resource waste.
                        </>
                      ) : (
                        <>
                          <strong>Baseline Scheduler</strong> had lower or equal imbalance compared to Proposed.
                        </>
                      )}
                    </span>
                  </div>
                  <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                    DI Delta: {(proposedDI.di - baselineDI.di).toFixed(3)}
                  </div>
                </div>
              )}

              {/* Dual Cards */}
              <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
                {baselineDI && renderDiStatsCard(baselineDI, 'Baseline Scheduler (FCFS / Min-Min)', 'Standard cloud queuing without deadline awareness')}
                {proposedDI && renderDiStatsCard(proposedDI, 'Proposed Deadline-Aware Scheduler', 'Optimized multi-objective VM allocation')}
              </div>
            </div>
          ) : (
            <div>
              {singleDI ? (
                renderDiStatsCard(singleDI, singleAlgorithm, 'Current active simulation workload distribution')
              ) : (
                <div style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: 20 }}>
                  No simulation results available to calculate Degree of Imbalance.
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
