import React from 'react';
import { MetricsDto, TaskResultDto } from '../types/simulation';

interface CloudSlaAndCostViewProps {
  isCompare: boolean;
  baselineMetrics?: MetricsDto;
  proposedMetrics?: MetricsDto;
  singleMetrics?: MetricsDto;
  singleAlgorithm?: string;
  baselineTasks?: TaskResultDto[];
  proposedTasks?: TaskResultDto[];
  singleTasks?: TaskResultDto[];
}

export const CloudSlaAndCostView: React.FC<CloudSlaAndCostViewProps> = ({
  isCompare,
  baselineMetrics,
  proposedMetrics,
  singleMetrics,
  singleAlgorithm,
  baselineTasks,
  proposedTasks,
  singleTasks,
}) => {
  // Cloud Economic Cost Constants
  const VM_HOURLY_RATE = 0.08; // $0.08/hr per VM (typical cloud c5.large tier)
  const VM_COUNT = 4;
  const SLA_FLAT_PENALTY = 0.20; // $0.20 per SLA breach
  const SLA_LATENCY_PENALTY_PER_SEC = 0.02; // $0.02 per second of deadline overrun

  const calculateCostAndSla = (tasks?: TaskResultDto[], metrics?: MetricsDto) => {
    if (!metrics || !tasks) return null;

    const makespanHours = metrics.makespan / 3600;
    const infrastructureComputeCost = makespanHours * VM_COUNT * VM_HOURLY_RATE;

    let slaViolations = 0;
    let totalLatenessSeconds = 0;

    tasks.forEach(t => {
      if (t.deadlineMissed || t.completionTime > t.deadline) {
        slaViolations++;
        totalLatenessSeconds += Math.max(0, t.completionTime - t.deadline);
      }
    });

    const slaPenaltyCost = (slaViolations * SLA_FLAT_PENALTY) + (totalLatenessSeconds * SLA_LATENCY_PENALTY_PER_SEC);
    const totalCost = infrastructureComputeCost + slaPenaltyCost;
    const slaComplianceRate = tasks.length > 0 ? ((tasks.length - slaViolations) / tasks.length) * 100 : 100;

    return {
      infrastructureComputeCost,
      slaViolations,
      totalLatenessSeconds,
      slaPenaltyCost,
      totalCost,
      slaComplianceRate,
    };
  };

  const baselineData = isCompare ? calculateCostAndSla(baselineTasks, baselineMetrics) : null;
  const proposedData = isCompare ? calculateCostAndSla(proposedTasks, proposedMetrics) : null;
  const singleData = !isCompare ? calculateCostAndSla(singleTasks, singleMetrics) : null;

  return (
    <div className="card" style={{ marginBottom: 20 }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14, flexWrap: 'wrap', gap: 10 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{
            width: 32,
            height: 32,
            borderRadius: 'var(--r-md)',
            background: 'rgba(16, 185, 129, 0.12)',
            border: '1px solid rgba(16, 185, 129, 0.3)',
            color: 'var(--success, #10b981)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="12" y1="1" x2="12" y2="23"/>
              <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
            </svg>
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-bright)' }}>
                Cloud SLA Compliance & Economic Cost Model
              </span>
              <span className="badge badge-cyan" style={{ fontSize: '0.68rem' }}>
                FinOps & SLA
              </span>
            </div>
            <p style={{ margin: 0, fontSize: '0.78rem', color: 'var(--text-muted)' }}>
              Evaluation of contractual SLA violation penalties and VM instance infrastructure billing.
            </p>
          </div>
        </div>

        {/* Pricing assumption note */}
        <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
          Model: $0.08/hr·VM · $0.20/breach penalty
        </span>
      </div>

      {isCompare && baselineData && proposedData ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          {/* Comparison Cards Grid */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 14 }}>
            {/* Standard Priority Baseline */}
            <div style={{
              padding: '14px',
              borderRadius: 'var(--r-md)',
              border: '1px solid var(--border)',
              background: 'rgba(255, 255, 255, 0.01)',
            }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
                <span style={{ fontWeight: 700, fontSize: '0.85rem', color: 'var(--text-bright)' }}>
                  Standard Priority
                </span>
                <span className="badge badge-baseline">Baseline</span>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 8, fontSize: '0.8rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--text-muted)' }}>SLA Compliance Rate:</span>
                  <strong style={{ color: baselineData.slaComplianceRate === 100 ? 'var(--success)' : 'var(--danger)' }}>
                    {baselineData.slaComplianceRate.toFixed(1)}%
                  </strong>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Contractual SLA Breaches:</span>
                  <span style={{ color: baselineData.slaViolations > 0 ? 'var(--danger)' : 'var(--text-primary)' }}>
                    {baselineData.slaViolations} tasks
                  </span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--text-muted)' }}>SLA Penalty Fines:</span>
                  <span style={{ color: baselineData.slaPenaltyCost > 0 ? 'var(--danger)' : 'var(--text-primary)' }}>
                    ${baselineData.slaPenaltyCost.toFixed(4)}
                  </span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', paddingTop: 6, borderTop: '1px solid var(--border)' }}>
                  <span style={{ fontWeight: 600 }}>Total Cloud Cost:</span>
                  <strong style={{ color: 'var(--text-bright)' }}>
                    ${baselineData.totalCost.toFixed(4)}
                  </strong>
                </div>
              </div>
            </div>

            {/* Deadline-Aware Proposed */}
            <div style={{
              padding: '14px',
              borderRadius: 'var(--r-md)',
              border: '1px solid rgba(56, 189, 248, 0.3)',
              background: 'rgba(14, 165, 233, 0.04)',
            }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
                <span style={{ fontWeight: 700, fontSize: '0.85rem', color: 'var(--cyan)' }}>
                  Deadline-Aware
                </span>
                <span className="badge badge-proposed">Proposed</span>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 8, fontSize: '0.8rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--text-muted)' }}>SLA Compliance Rate:</span>
                  <strong style={{ color: proposedData.slaComplianceRate >= baselineData.slaComplianceRate ? 'var(--success)' : 'var(--warning)' }}>
                    {proposedData.slaComplianceRate.toFixed(1)}%
                  </strong>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Contractual SLA Breaches:</span>
                  <span style={{ color: proposedData.slaViolations > 0 ? 'var(--danger)' : 'var(--success)' }}>
                    {proposedData.slaViolations} tasks
                  </span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--text-muted)' }}>SLA Penalty Fines:</span>
                  <span style={{ color: proposedData.slaPenaltyCost > 0 ? 'var(--danger)' : 'var(--success)' }}>
                    ${proposedData.slaPenaltyCost.toFixed(4)}
                  </span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', paddingTop: 6, borderTop: '1px solid var(--border)' }}>
                  <span style={{ fontWeight: 600 }}>Total Cloud Cost:</span>
                  <strong style={{ color: 'var(--cyan)' }}>
                    ${proposedData.totalCost.toFixed(4)}
                  </strong>
                </div>
              </div>
            </div>
          </div>

          {/* Economic Delta Highlight */}
          <div style={{
            padding: '10px 14px',
            borderRadius: 'var(--r-md)',
            background: proposedData.totalCost <= baselineData.totalCost
              ? 'rgba(16, 185, 129, 0.08)'
              : 'rgba(245, 158, 11, 0.08)',
            border: `1px solid ${proposedData.totalCost <= baselineData.totalCost ? 'rgba(16, 185, 129, 0.25)' : 'rgba(245, 158, 11, 0.25)'}`,
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            fontSize: '0.8rem',
            color: 'var(--text-primary)',
          }}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0 }}>
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="16" x2="12" y2="12"/>
              <line x1="12" y1="8" x2="12.01" y2="8"/>
            </svg>
            <div>
              {proposedData.totalCost <= baselineData.totalCost ? (
                <span>
                  The <strong>Deadline-Aware</strong> scheduler saved <strong>${(baselineData.totalCost - proposedData.totalCost).toFixed(4)}</strong> in SLA penalties and compute cost while maintaining a <strong>{proposedData.slaComplianceRate.toFixed(1)}%</strong> contractual delivery rate.
                </span>
              ) : (
                <span>
                  The <strong>Standard Priority</strong> scheduler had lower total cost by <strong>${(proposedData.totalCost - baselineData.totalCost).toFixed(4)}</strong> on this specific workload.
                </span>
              )}
            </div>
          </div>
        </div>
      ) : singleData ? (
        <div>
          {singleAlgorithm && (
            <div style={{ fontSize: '0.85rem', fontWeight: 600, marginBottom: 10, color: 'var(--text-bright)' }}>
              {singleAlgorithm === 'PROPOSED' ? 'Deadline-Aware Scheduler' : 'Standard Priority Scheduler'}
            </div>
          )}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12 }}>
          <div style={{ padding: '12px', background: 'var(--bg-card-subtle, rgba(255,255,255,0.02))', borderRadius: 'var(--r-md)', border: '1px solid var(--border)' }}>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>SLA Compliance Rate</div>
            <div style={{ fontSize: '1.2rem', fontWeight: 700, color: singleData.slaComplianceRate === 100 ? 'var(--success)' : 'var(--danger)', marginTop: 4 }}>
              {singleData.slaComplianceRate.toFixed(1)}%
            </div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: 2 }}>{singleData.slaViolations} violations</div>
          </div>

          <div style={{ padding: '12px', background: 'var(--bg-card-subtle, rgba(255,255,255,0.02))', borderRadius: 'var(--r-md)', border: '1px solid var(--border)' }}>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Infrastructure Compute</div>
            <div style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--text-bright)', marginTop: 4 }}>
              ${singleData.infrastructureComputeCost.toFixed(4)}
            </div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: 2 }}>4 VM instance billing</div>
          </div>

          <div style={{ padding: '12px', background: 'var(--bg-card-subtle, rgba(255,255,255,0.02))', borderRadius: 'var(--r-md)', border: '1px solid var(--border)' }}>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>SLA Breach Penalties</div>
            <div style={{ fontSize: '1.2rem', fontWeight: 700, color: singleData.slaPenaltyCost > 0 ? 'var(--danger)' : 'var(--success)', marginTop: 4 }}>
              ${singleData.slaPenaltyCost.toFixed(4)}
            </div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: 2 }}>Contractual fee</div>
          </div>

          <div style={{ padding: '12px', background: 'var(--bg-card-subtle, rgba(255,255,255,0.02))', borderRadius: 'var(--r-md)', border: '1px solid var(--border)' }}>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Total Cloud Cost</div>
            <div style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--cyan)', marginTop: 4 }}>
              ${singleData.totalCost.toFixed(4)}
            </div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: 2 }}>Compute + Penalties</div>
          </div>
        </div>
      </div>
      ) : null}
    </div>
  );
};
