import React from 'react';
import { Link } from 'react-router-dom';

export const AboutPage: React.FC = () => {
  return (
    <main className="about-page">
      <div className="container-narrow">
        {/* Page header */}
        <div style={{ marginBottom: 48, paddingBottom: 24, borderBottom: '1px solid var(--border)' }}>
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 8,
              padding: '4px 12px',
              borderRadius: 'var(--radius-sm)',
              border: '1px solid var(--border-strong)',
              background: 'var(--accent-dim)',
              color: 'var(--accent)',
              fontSize: '0.75rem',
              fontWeight: 700,
              letterSpacing: '0.08em',
              textTransform: 'uppercase',
              marginBottom: 16,
            }}
          >
            Documentation
          </div>
          <h1 style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--text-primary)', letterSpacing: '-0.02em', marginBottom: 8 }}>
            How It Works
          </h1>
          <p style={{ fontSize: '0.95rem', color: 'var(--text-secondary)', lineHeight: 1.65 }}>
            A complete guide to the scheduling algorithms, simulation environment, and evaluation metrics
            used in this cloud task scheduling platform.
          </p>
        </div>

        {/* Problem */}
        <section className="about-section">
          <h2 className="section-heading">The Problem</h2>
          <p className="section-subheading">Why deadline-aware scheduling matters</p>
          <div className="card">
            <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.7, marginBottom: 12 }}>
              In cloud computing environments, multiple tasks arrive continuously and compete for
              limited virtual machine resources. Standard priority-based schedulers treat all tasks
              with the same urgency regardless of their deadlines, leading to deadline violations
              for time-sensitive workloads.
            </p>
            <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.7 }}>
              This platform studies how incorporating deadline urgency into the scheduling score
              affects performance across various metrics compared to a standard priority baseline.
            </p>
          </div>
        </section>

        {/* Simulation Environment */}
        <section className="about-section">
          <h2 className="section-heading">Simulation Environment</h2>
          <p className="section-subheading">Powered by CloudSim Plus</p>
          <div className="about-grid">
            {[
              { num: '01', title: 'CloudSim Plus', text: 'Industry-standard Java cloud computing simulation framework used to model VM allocation and task execution.' },
              { num: '02', title: 'Virtual Machines', text: 'The simulation uses 4 virtual machines (VM 0–3) with configurable MIPS ratings to process submitted tasks.' },
              { num: '03', title: 'Task Model', text: 'Each task has a Task ID, Priority (1–10), Arrival Time, Execution Time, and Deadline.' },
              { num: '04', title: 'Fibonacci Heap', text: 'A Fibonacci Heap data structure is used as the priority queue, providing efficient O(log n) task extraction.' },
            ].map((c) => (
              <div className="about-card" key={c.num}>
                <div className="about-card-num">{c.num}</div>
                <div className="about-card-title">{c.title}</div>
                <div className="about-card-text">{c.text}</div>
              </div>
            ))}
          </div>
        </section>

        {/* Scheduling Approaches */}
        <section className="about-section">
          <h2 className="section-heading">Scheduling Approaches</h2>
          <p className="section-subheading">Two methods studied side-by-side</p>

          <div className="card" style={{ marginBottom: 16 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
              <span className="badge badge-baseline">Standard Priority</span>
            </div>
            <p style={{ fontSize: '0.88rem', color: 'var(--text-secondary)', lineHeight: 1.65, marginBottom: 12 }}>
              The baseline scheduler uses a waiting-time weighted priority assignment. Tasks are
              ordered by a combination of their assigned priority and how long they have been
              waiting in the queue.
            </p>
            <div
              style={{
                background: 'var(--bg-glass)',
                border: '1px solid var(--border)',
                borderRadius: 'var(--radius-sm)',
                padding: '12px 16px',
                fontFamily: 'var(--font-mono)',
                fontSize: '0.82rem',
                color: 'var(--accent)',
              }}
            >
              Score = Priority × WaitingFactor
            </div>
          </div>

          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
              <span className="badge badge-proposed">Deadline-Aware</span>
            </div>
            <p style={{ fontSize: '0.88rem', color: 'var(--text-secondary)', lineHeight: 1.65, marginBottom: 12 }}>
              The proposed scheduler adds a deadline urgency component to the multi-criteria score.
              Tasks closer to their deadline receive a higher urgency value, influencing their
              scheduling order.
            </p>
            <div
              style={{
                background: 'var(--bg-glass)',
                border: '1px solid var(--border)',
                borderRadius: 'var(--radius-sm)',
                padding: '12px 16px',
                fontFamily: 'var(--font-mono)',
                fontSize: '0.82rem',
                color: 'var(--accent)',
                lineHeight: 1.7,
              }}
            >
              Score = 0.35 × Priority<br />
              &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;+ 0.50 × Deadline Urgency<br />
              &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;+ 0.15 × Waiting Factor
            </div>

            {/* Exact Mathematical Reasons for 0.35, 0.50, 0.15 */}
            <div
              style={{
                marginTop: 16,
                padding: '16px 18px',
                background: 'rgba(56, 189, 248, 0.05)',
                border: '1px solid rgba(56, 189, 248, 0.25)',
                borderRadius: 'var(--radius-sm)',
              }}
            >
              <div
                style={{
                  fontWeight: 700,
                  fontSize: '0.92rem',
                  color: 'var(--text-primary)',
                  marginBottom: 12,
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                }}
              >
                <span>📐</span> Exact Mathematical Derivation: Why Specifically 0.35, 0.50, and 0.15?
              </div>

              <p style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', marginBottom: 12, lineHeight: 1.6 }}>
                These values are not random guesses. They are mathematically derived from a system of <strong>4 boundary constraints</strong> and a <strong>70/30 remaining-budget partition</strong>:
              </p>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                {/* Step 1 */}
                <div style={{ background: 'var(--bg-glass)', padding: '10px 14px', borderRadius: '4px', borderLeft: '3px solid var(--accent)' }}>
                  <div style={{ fontWeight: 600, fontSize: '0.84rem', color: 'var(--text-primary)' }}>
                    1. The 50% Operational Allocation (Why W<sub>d</sub> = 0.50)
                  </div>
                  <div style={{ fontSize: '0.80rem', color: 'var(--text-secondary)', marginTop: 4, lineHeight: 1.55 }}>
                    Because the core research objective is <strong>Deadline-Awareness and SLA Breach Minimization</strong>, deadline urgency is assigned exactly <strong>50% (0.50)</strong> of the total decision space. This gives urgency primary operational authority whenever a task approaches its deadline.
                  </div>
                </div>

                {/* Step 2 */}
                <div style={{ background: 'var(--bg-glass)', padding: '10px 14px', borderRadius: '4px', borderLeft: '3px solid #10b981' }}>
                  <div style={{ fontWeight: 600, fontSize: '0.84rem', color: 'var(--text-primary)' }}>
                    2. The 70/30 Non-Urgency Partition (Why W<sub>p</sub> = 0.35 and W<sub>w</sub> = 0.15)
                  </div>
                  <div style={{ fontSize: '0.80rem', color: 'var(--text-secondary)', marginTop: 4, lineHeight: 1.55 }}>
                    After allocating 0.50 to urgency, a remaining budget of <code>1.0 − 0.50 = 0.50</code> remains. To balance customer service tiers against queue fairness, this remaining budget is partitioned in a standard <strong>70% / 30%</strong> ratio:
                    <ul style={{ margin: '6px 0 0', paddingLeft: 18 }}>
                      <li><strong>Base Priority (W<sub>p</sub>):</strong> <code>70% × 0.50 = 0.35</code> (maintains high priority when deadlines are safe).</li>
                      <li><strong>Waiting Factor (W<sub>w</sub>):</strong> <code>30% × 0.50 = 0.15</code> (prevents queue starvation for older tasks).</li>
                      <li><strong>Check:</strong> <code>0.50 + 0.35 + 0.15 = 1.00</code> (exact unit convex sum).</li>
                    </ul>
                  </div>
                </div>

                {/* Step 3 */}
                <div style={{ background: 'var(--bg-glass)', padding: '10px 14px', borderRadius: '4px', borderLeft: '3px solid #f59e0b' }}>
                  <div style={{ fontWeight: 600, fontSize: '0.84rem', color: 'var(--text-primary)' }}>
                    3. The Urgency Rescue Inequality (Why 0.50 &gt; 0.315)
                  </div>
                  <div style={{ fontSize: '0.80rem', color: 'var(--text-secondary)', marginTop: 4, lineHeight: 1.55 }}>
                    To mathematically guarantee that a critical task (even with lowest Priority 1, <code>P<sub>norm</sub> = 0.1</code>) can preempt a non-urgent task with highest Priority 10 (<code>P<sub>norm</sub> = 1.0</code>), the urgency weight must satisfy:
                    <div style={{ fontFamily: 'var(--font-mono)', margin: '6px 0', padding: '4px 8px', background: 'rgba(0,0,0,0.2)', borderRadius: 3, color: 'var(--accent)' }}>
                      W<sub>d</sub> &gt; W<sub>p</sub> × (1.0 − 0.1) &nbsp;⟹&nbsp; W<sub>d</sub> &gt; 0.90 × W<sub>p</sub>
                    </div>
                    With <code>W<sub>p</sub> = 0.35</code>, the maximum base priority gap is <code>0.35 × 0.9 = 0.315</code>.<br />
                    Since <code>W<sub>d</sub> = 0.50 &gt; 0.315</code>, the critical task receives <code>0.35(0.1) + 0.50(1.0) = 0.535</code> vs <code>0.35(1.0) + 0.50(0) = 0.350</code>. The rescue margin is <strong>+0.185</strong>, mathematically preventing false starvation.
                  </div>
                </div>

                {/* Step 4 */}
                <div style={{ background: 'var(--bg-glass)', padding: '10px 14px', borderRadius: '4px', borderLeft: '3px solid #8b5cf6' }}>
                  <div style={{ fontWeight: 600, fontSize: '0.84rem', color: 'var(--text-primary)' }}>
                    4. Anti-Starvation Protection (Why W<sub>w</sub> = 0.15 is strictly smaller)
                  </div>
                  <div style={{ fontSize: '0.80rem', color: 'var(--text-secondary)', marginTop: 4, lineHeight: 1.55 }}>
                    If <code>W<sub>w</sub></code> were too large (e.g. 0.25+), aging tasks would displace actively urgent tasks. Setting <code>W<sub>w</sub> = 0.15</code> maintains the strict operational hierarchy:
                    <div style={{ fontFamily: 'var(--font-mono)', margin: '6px 0', padding: '4px 8px', background: 'rgba(0,0,0,0.2)', borderRadius: 3, color: 'var(--accent)' }}>
                      W<sub>d</sub> (0.50) &gt; W<sub>p</sub> (0.35) &gt; W<sub>w</sub> (0.15)
                    </div>
                    This ensures aging never causes another task to breach its SLA deadline.
                  </div>
                </div>
              </div>
            </div>

            <p
              style={{
                marginTop: 12,
                fontSize: '0.78rem',
                color: 'var(--text-muted)',
                fontStyle: 'italic',
              }}
            >
              Note: The weights (0.35 / 0.50 / 0.15) are calibrated project design parameters
              that can also be customized dynamically for different cloud workload profiles.
            </p>
          </div>
        </section>

        {/* Workflow */}
        <section className="about-section">
          <h2 className="section-heading">Scheduling Workflow</h2>
          <p className="section-subheading">Step-by-step process</p>
          <div className="about-grid">
            {[
              { num: '01', title: 'Task Arrival', text: 'Tasks enter the simulation at their specified arrival times and are added to the ready queue.' },
              { num: '02', title: 'Score Calculation', text: 'Each task\'s scheduling score is computed using the selected algorithm\'s scoring formula.' },
              { num: '03', title: 'Heap Insertion', text: 'Tasks are inserted into the Fibonacci Heap, which maintains ordering by score.' },
              { num: '04', title: 'VM Assignment', text: 'The scheduler extracts the highest-priority task and assigns it to an available VM.' },
              { num: '05', title: 'Execution', text: 'The VM executes the task. Completion time, waiting time, and deadline status are recorded.' },
              { num: '06', title: 'Metrics Collection', text: 'After all tasks complete, aggregate metrics (makespan, throughput, etc.) are computed.' },
            ].map((c) => (
              <div className="about-card" key={c.num}>
                <div className="about-card-num">{c.num}</div>
                <div className="about-card-title">{c.title}</div>
                <div className="about-card-text">{c.text}</div>
              </div>
            ))}
          </div>
        </section>

        {/* Metrics */}
        <section className="about-section">
          <h2 className="section-heading">Evaluation Metrics</h2>
          <p className="section-subheading">What the simulation measures</p>
          <div className="card">
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr>
                  <th style={{ textAlign: 'left', padding: '8px 12px', fontSize: '0.72rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.07em', color: 'var(--text-muted)', borderBottom: '1px solid var(--border)' }}>Metric</th>
                  <th style={{ textAlign: 'left', padding: '8px 12px', fontSize: '0.72rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.07em', color: 'var(--text-muted)', borderBottom: '1px solid var(--border)' }}>Description</th>
                  <th style={{ textAlign: 'left', padding: '8px 12px', fontSize: '0.72rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.07em', color: 'var(--text-muted)', borderBottom: '1px solid var(--border)' }}>Unit</th>
                </tr>
              </thead>
              <tbody>
                {[
                  ['Makespan', 'Total time to complete all tasks', 'seconds'],
                  ['Avg. Waiting Time', 'Average time tasks wait before execution', 'seconds'],
                  ['Avg. Turnaround Time', 'Average from arrival to completion', 'seconds'],
                  ['Throughput', 'Tasks completed per time unit', 'tasks/s'],
                  ['Deadline Miss Rate', 'Fraction of tasks missing their deadline', '%'],
                  ['VM Utilization', 'Fraction of VM capacity used', '%'],
                ].map(([m, d, u]) => (
                  <tr key={m}>
                    <td style={{ padding: '9px 12px', fontWeight: 600, color: 'var(--accent)', fontSize: '0.85rem', borderBottom: '1px solid rgba(255,255,255,0.04)' }}>{m}</td>
                    <td style={{ padding: '9px 12px', color: 'var(--text-secondary)', fontSize: '0.85rem', borderBottom: '1px solid rgba(255,255,255,0.04)' }}>{d}</td>
                    <td style={{ padding: '9px 12px', color: 'var(--text-muted)', fontSize: '0.82rem', fontFamily: 'var(--font-mono)', borderBottom: '1px solid rgba(255,255,255,0.04)' }}>{u}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        {/* CTA */}
        <div style={{ textAlign: 'center', paddingTop: 16 }}>
          <Link to="/simulator" className="btn btn-primary btn-lg">
            Try the Simulator →
          </Link>
        </div>
      </div>
    </main>
  );
};
