import React, { useState } from 'react';

export const TechnicalDetails: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <div style={{
      margin: '24px 0',
      background: 'rgba(30, 41, 59, 0.4)',
      border: '1px solid #334155',
      borderRadius: '8px',
      overflow: 'hidden'
    }}>
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        style={{
          width: '100%',
          padding: '14px 18px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          background: 'transparent',
          border: 'none',
          color: '#cbd5e1',
          cursor: 'pointer',
          fontSize: '0.95rem',
          fontWeight: 600,
          textAlign: 'left'
        }}
      >
        <span>{isOpen ? '▼' : '▶'} Technical Details</span>
        <span style={{ fontSize: '0.8rem', color: '#64748b', fontWeight: 400 }}>
          {isOpen ? 'Click to collapse' : 'Click to inspect research formulas & architecture'}
        </span>
      </button>

      {isOpen && (
        <div style={{
          padding: '0 20px 20px',
          borderTop: '1px solid #334155',
          fontSize: '0.86rem',
          color: '#94a3b8',
          lineHeight: '1.6'
        }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '16px', marginTop: '16px' }}>
            
            {/* Waiting Time Matrix */}
            <div style={{ background: '#0f172a', padding: '14px', borderRadius: '6px', border: '1px solid #1e293b' }}>
              <div style={{ color: '#38bdf8', fontWeight: 600, marginBottom: '6px' }}>1. Waiting Time Matrix (WTM)</div>
              <p style={{ margin: 0 }}>
                Maintains predicted queuing delays across heterogeneous VMs. Calculates estimated waiting time before an incoming task can begin execution based on each VM&apos;s current workload.
              </p>
            </div>

            {/* Priority Assignment */}
            <div style={{ background: '#0f172a', padding: '14px', borderRadius: '6px', border: '1px solid #1e293b' }}>
              <div style={{ color: '#38bdf8', fontWeight: 600, marginBottom: '6px' }}>2. Priority Assignment</div>
              <p style={{ margin: 0 }}>
                Normalizes base task priorities (1–10) to a [0, 1] interval. High-priority tasks receive immediate preferential consideration in scheduling passes.
              </p>
            </div>

            {/* Deadline Urgency */}
            <div style={{ background: '#0f172a', padding: '14px', borderRadius: '6px', border: '1px solid #1e293b' }}>
              <div style={{ color: '#38bdf8', fontWeight: 600, marginBottom: '6px' }}>3. Dynamic Deadline Urgency</div>
              <p style={{ margin: 0 }}>
                Calculates time slack: <code style={{ color: '#e2e8f0', background: '#1e293b', padding: '2px 4px', borderRadius: '3px' }}>slack = deadline - (arrival + execution)</code>. Applies smooth sigmoid/exponential decay (<code style={{ color: '#e2e8f0', background: '#1e293b', padding: '2px 4px', borderRadius: '3px' }}>&tau; = 50.0, k = 2.0</code>) to prevent urgent starvation.
              </p>
            </div>

            {/* Scheduling Formula */}
            <div style={{ background: '#0f172a', padding: '14px', borderRadius: '6px', border: '1px solid #1e293b' }}>
              <div style={{ color: '#38bdf8', fontWeight: 600, marginBottom: '6px' }}>4. Multi-Criteria Composite Score</div>
              <p style={{ margin: 0, marginBottom: '8px' }}>
                Tasks are prioritized via calibrated composite weights:
                <code style={{ color: '#38bdf8', display: 'block', margin: '4px 0', background: '#1e293b', padding: '4px 8px', borderRadius: '3px' }}>
                  Score = 0.35 &times; Priority + 0.50 &times; Urgency + 0.15 &times; WaitingFactor
                </code>
              </p>
              <div style={{ fontSize: '0.78rem', color: '#94a3b8', lineHeight: 1.5, borderTop: '1px dashed #334155', paddingTop: '6px' }}>
                <strong style={{ color: '#cbd5e1' }}>Why these specific numbers?</strong>
                <ul style={{ margin: '4px 0 0', paddingLeft: '16px' }}>
                  <li><strong>0.50 (Urgency):</strong> Largest weight (50%) to guarantee critical tasks avoid SLA deadline breaches.</li>
                  <li><strong>0.35 (Priority):</strong> Respects customer priority tiers when deadlines are not in danger. Since 0.50 &gt; 0.35, urgency always overrides priority when a deadline is imminent.</li>
                  <li><strong>0.15 (Waiting):</strong> Prevents starvation for low-priority tasks. Sum is 1.0 (0.35 + 0.50 + 0.15 = 1.0) to keep scores normalized in [0, 1].</li>
                </ul>
              </div>
            </div>

            {/* Fibonacci Heap */}
            <div style={{ background: '#0f172a', padding: '14px', borderRadius: '6px', border: '1px solid #1e293b' }}>
              <div style={{ color: '#38bdf8', fontWeight: 600, marginBottom: '6px' }}>5. Fibonacci Heap Implementation</div>
              <p style={{ margin: 0 }}>
                Uses a custom Fibonacci Heap structure providing <code style={{ color: '#e2e8f0', background: '#1e293b', padding: '2px 4px', borderRadius: '3px' }}>O(1)</code> amortized insertion and <code style={{ color: '#e2e8f0', background: '#1e293b', padding: '2px 4px', borderRadius: '3px' }}>O(log n)</code> delete-max extraction for scalable task dispatching.
              </p>
            </div>

            {/* CloudSim Plus & VM Config */}
            <div style={{ background: '#0f172a', padding: '14px', borderRadius: '6px', border: '1px solid #1e293b' }}>
              <div style={{ color: '#38bdf8', fontWeight: 600, marginBottom: '6px' }}>6. CloudSim Plus Architecture</div>
              <p style={{ margin: 0 }}>
                Simulates a heterogeneous 4-VM cluster (1000–2500 MIPS) on dual quad-core cloud host nodes using deterministic event-driven simulation.
              </p>
            </div>

          </div>
        </div>
      )}
    </div>
  );
};
