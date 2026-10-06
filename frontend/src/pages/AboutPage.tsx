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
            A complete guide to the deadline-aware scheduling algorithms, dual execution architecture
            (discrete-event simulation vs. real cloud worker on Render), and evaluation metrics.
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
              with the same static urgency regardless of their deadlines, leading to SLA violations
              and costly deadline breaches for time-sensitive workloads.
            </p>
            <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.7 }}>
              This platform compares a standard priority baseline against an intelligent deadline-aware
              scheduling heuristic that dynamically factors in deadline proximity, ensuring critical tasks
              are executed before their service-level agreements expire.
            </p>
          </div>
        </section>

        {/* Dual Execution Environments */}
        <section className="about-section">
          <h2 className="section-heading">Dual Execution Architecture</h2>
          <p className="section-subheading">Mathematical simulation and live cloud compute</p>
          
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 16, marginBottom: 20 }}>
            {/* Mode 1 */}
            <div className="card" style={{ borderTop: '3px solid var(--accent)' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                <span style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--text-primary)' }}>1. Simulation Mode</span>
                <span className="badge badge-proposed">CloudSim Plus 8.0</span>
              </div>
              <p style={{ fontSize: '0.86rem', color: 'var(--text-secondary)', lineHeight: 1.65, marginBottom: 14 }}>
                Discrete-event mathematical simulation modeling a cloud datacenter with <strong>4 Virtual Machines (VM 0–3)</strong>.
                Tasks execute in-memory with zero cloud network latency.
              </p>
              <ul style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', lineHeight: 1.6, paddingLeft: 18, margin: 0 }}>
                <li>Deterministic, reproducible research benchmarks</li>
                <li>Simulates multi-core cloud hosts with MIPS ratings</li>
                <li>Ideal for rapid comparative evaluation of large workloads</li>
                <li>Supports instant side-by-side algorithm comparison</li>
              </ul>
            </div>

            {/* Mode 2 */}
            <div className="card" style={{ borderTop: '3px solid #10b981' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                <span style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--text-primary)' }}>2. Real Cloud Worker Mode</span>
                <span className="badge badge-baseline" style={{ background: 'rgba(16, 185, 129, 0.15)', color: '#10b981', borderColor: 'rgba(16, 185, 129, 0.3)' }}>FastAPI · Render Free</span>
              </div>
              <p style={{ fontSize: '0.86rem', color: 'var(--text-secondary)', lineHeight: 1.65, marginBottom: 14 }}>
                Dispatches scheduled tasks over HTTPS to a genuine Python FastAPI execution node deployed on <strong>Render Free</strong> (or a local worker fallback).
              </p>
              <ul style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', lineHeight: 1.6, paddingLeft: 18, margin: 0 }}>
                <li>Runs active SHA-256 cryptographic hashing to drive genuine CPU cycles</li>
                <li>No simulated sleep; measures live wall-clock time and process CPU time</li>
                <li>Streams real hardware telemetry (<code style={{ fontSize: '0.78rem' }}>psutil</code> utilization %) back to the backend</li>
                <li>Strictly decoupled: backend decides order, worker strictly executes</li>
              </ul>
            </div>
          </div>

          <div className="card" style={{ background: 'rgba(255, 255, 255, 0.02)', borderColor: 'var(--border)' }}>
            <div style={{ fontWeight: 600, fontSize: '0.88rem', color: 'var(--text-primary)', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 8 }}>
              <span>🔒</span> Architectural Separation of Concerns
            </div>
            <p style={{ fontSize: '0.84rem', color: 'var(--text-secondary)', lineHeight: 1.6, margin: 0 }}>
              The real worker node does <strong>NOT</strong> make scheduling, priority, or deadline decisions.
              The Spring Boot backend's deadline-aware scheduling algorithm calculates task urgency and computes
              the complete task sequence <em>first</em>. Only then are tasks dispatched sequentially to the Render
              cloud worker for genuine compute execution.
            </p>
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
          <h2 className="section-heading">End-to-End Workflow</h2>
          <p className="section-subheading">From task submission to telemetry output</p>
          <div className="about-grid">
            {[
              { num: '01', title: 'Task Ingestion', text: 'Workloads are entered manually, loaded via demo presets, or imported from CSV files with priority, arrival, execution, and deadline specs.' },
              { num: '02', title: 'Urgency & Scoring', text: 'Each task\'s scheduling score is computed dynamically using either the Standard Priority baseline or the Proposed Deadline-Aware formula.' },
              { num: '03', title: 'Fibonacci Heap Queue', text: 'Tasks are queued in a Fibonacci Heap structure, enabling O(1) amortized insertion and O(log n) highest-urgency extraction.' },
              { num: '04', title: 'Target Routing', text: 'Tasks route to either the in-memory CloudSim Plus simulation engine or the live Render FastAPI worker via HTTPS.' },
              { num: '05', title: 'Execution & Compute', text: 'Tasks run across simulated virtual machines or undergo real SHA-256 cryptographic hashing to drive actual CPU cycles.' },
              { num: '06', title: 'Telemetry & Reporting', text: 'The backend captures wall-clock duration, CPU utilization, and timestamps to generate the Gantt chart and SLA metrics.' },
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
          <p className="section-subheading">What the platform measures across simulation and cloud runs</p>
          <div className="card">
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr>
                  <th style={{ textAlign: 'left', padding: '8px 12px', fontSize: '0.72rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.07em', color: 'var(--text-muted)', borderBottom: '1px solid var(--border)' }}>Metric</th>
                  <th style={{ textAlign: 'left', padding: '8px 12px', fontSize: '0.72rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.07em', color: 'var(--text-muted)', borderBottom: '1px solid var(--border)' }}>Description</th>
                  <th style={{ textAlign: 'left', padding: '8px 12px', fontSize: '0.72rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.07em', color: 'var(--text-muted)', borderBottom: '1px solid var(--border)' }}>Unit / Scope</th>
                </tr>
              </thead>
              <tbody>
                {[
                  ['Makespan', 'Total elapsed time from first task arrival until the last task finishes', 'seconds'],
                  ['Avg. Waiting Time', 'Average duration tasks spent waiting in queue prior to execution', 'seconds'],
                  ['Avg. Turnaround Time', 'Average total time from task submission to completion (waiting + execution)', 'seconds'],
                  ['Throughput', 'Number of completed tasks per unit time', 'tasks/s'],
                  ['Deadline Miss Rate', 'Percentage of tasks that finished past their target deadline', '% (SLA Penalty)'],
                  ['VM Utilization', 'Percentage of available virtual machine capacity actively utilized in simulation', '% (Simulation)'],
                  ['CPU Utilization', 'Actual host core processing utilization measured via psutil telemetry', '% (Real Worker)'],
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
