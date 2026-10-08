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
              <span className="badge badge-proposed">Proposed Algorithm</span>
              <span style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>Three-Component Architecture</span>
            </div>
            <p style={{ fontSize: '0.88rem', color: 'var(--text-secondary)', lineHeight: 1.65, marginBottom: 12 }}>
              The proposed scheduling framework consists of three formally defined and separated algorithms:
              the <strong>Dynamic Deadline Urgency Algorithm</strong>, the <strong>Dynamic Priority Algorithm</strong>,
              and the <strong>Persistent Fibonacci-Heap Scheduling Algorithm</strong>.
            </p>

            {/* Formula Block */}
            <div
              style={{
                background: 'var(--bg-glass)',
                border: '1px solid var(--border)',
                borderRadius: 'var(--radius-sm)',
                padding: '14px 18px',
                fontFamily: 'var(--font-mono)',
                fontSize: '0.82rem',
                color: 'var(--accent)',
                lineHeight: 1.8,
              }}
            >
              <strong>1. Dynamic Deadline Urgency:</strong><br />
              &nbsp;&nbsp;&nbsp;&nbsp;Ω(t) = [1000 / (EffectiveSlack(t) + 1)] × [1 + B / (D_effective(t) + 1)]<br />
              <strong>2. Dynamic Priority:</strong><br />
              &nbsp;&nbsp;&nbsp;&nbsp;P(t) = 100 × basePriority + Ω(t) + 10 × W(t)<br />
              <strong>3. Persistent Fibonacci Min-Heap Key:</strong><br />
              &nbsp;&nbsp;&nbsp;&nbsp;Key(t) = (-P(t), deadline, taskId)
            </div>

            {/* Algorithmic Details */}
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
                <span>📐</span> Algorithmic Formulation & Design Principles
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                {/* Step 1 */}
                <div style={{ background: 'var(--bg-glass)', padding: '10px 14px', borderRadius: '4px', borderLeft: '3px solid var(--accent)' }}>
                  <div style={{ fontWeight: 600, fontSize: '0.84rem', color: 'var(--text-primary)' }}>
                    1. Dynamic Deadline Urgency Algorithm (Ω(t))
                  </div>
                  <div style={{ fontSize: '0.80rem', color: 'var(--text-secondary)', marginTop: 4, lineHeight: 1.55 }}>
                    Evaluates slack <code>Slack(t) = (deadline − t) − B</code>. Urgency scales rapidly as slack vanishes and scales with burst execution time <code>B</code>. Overdue tasks are guarded by <code>D_effective(t) = max(0, D(t))</code>, preventing division by zero or negative denominators.
                  </div>
                </div>

                {/* Step 2 */}
                <div style={{ background: 'var(--bg-glass)', padding: '10px 14px', borderRadius: '4px', borderLeft: '3px solid #10b981' }}>
                  <div style={{ fontWeight: 600, fontSize: '0.84rem', color: 'var(--text-primary)' }}>
                    2. Dynamic Priority Algorithm (P(t))
                  </div>
                  <div style={{ fontSize: '0.80rem', color: 'var(--text-secondary)', marginTop: 4, lineHeight: 1.55 }}>
                    Combines immutable base priority (<code>100 × basePriority</code>), dynamic urgency <code>Ω(t)</code>, and queue waiting time <code>10 × W(t)</code>. The client's base priority is never mutated, while queue aging prevents low-priority starvation.
                  </div>
                </div>

                {/* Step 3 */}
                <div style={{ background: 'var(--bg-glass)', padding: '10px 14px', borderRadius: '4px', borderLeft: '3px solid #f59e0b' }}>
                  <div style={{ fontWeight: 600, fontSize: '0.84rem', color: 'var(--text-primary)' }}>
                    3. Persistent Fibonacci-Heap Scheduling Algorithm
                  </div>
                  <div style={{ fontSize: '0.80rem', color: 'var(--text-secondary)', marginTop: 4, lineHeight: 1.55 }}>
                    A single persistent Min-Heap is maintained across the entire run. Minimizing <code>−P(t)</code> extracts the highest dynamic priority in <code>O(log n)</code> amortized time. Active waiting tasks are dynamically updated in-place via <code>decreaseKey()</code> in <code>O(1)</code> amortized time without destroying or rebuilding the heap.
                  </div>
                </div>
              </div>
            </div>

            {/* Workload Types Table */}
            <div style={{ marginTop: 16 }}>
              <h4 style={{ fontSize: '0.86rem', color: 'var(--text-primary)', marginBottom: 8 }}>
                Representative Cloud Workload Types
              </h4>
              <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginBottom: 10 }}>
                Format: <code>(Task_ID, Task Type, Deadline, Burst Time, Priority, Waiting Time)</code>. Evaluates realistic computational profiles:
              </p>
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.78rem' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left', color: 'var(--text-muted)' }}>
                      <th style={{ padding: '6px 8px' }}>Task ID</th>
                      <th style={{ padding: '6px 8px' }}>Task Type</th>
                      <th style={{ padding: '6px 8px' }}>Burst Time</th>
                      <th style={{ padding: '6px 8px' }}>Deadline</th>
                      <th style={{ padding: '6px 8px' }}>Base Priority</th>
                    </tr>
                  </thead>
                  <tbody>
                    {[
                      ['T1', 'Image Processing', '8 s', '20 s', '3'],
                      ['T2', 'Video Transcoding', '15 s', '25 s', '5'],
                      ['T3', 'Database Query', '4 s', '12 s', '4'],
                      ['T4', 'ML Model Inference', '10 s', '18 s', '5'],
                      ['T5', 'Log Analysis', '6 s', '30 s', '2'],
                      ['T6', 'File Compression', '7 s', '22 s', '3'],
                      ['T7', 'Data Analytics', '12 s', '28 s', '4'],
                      ['T8', 'Backup Processing', '20 s', '45 s', '1'],
                    ].map(([id, type, burst, dline, prio]) => (
                      <tr key={id} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                        <td style={{ padding: '6px 8px', fontWeight: 600, color: 'var(--accent)' }}>{id}</td>
                        <td style={{ padding: '6px 8px', color: 'var(--text-primary)' }}>{type}</td>
                        <td style={{ padding: '6px 8px', color: 'var(--text-secondary)' }}>{burst}</td>
                        <td style={{ padding: '6px 8px', color: 'var(--text-secondary)' }}>{dline}</td>
                        <td style={{ padding: '6px 8px', color: 'var(--text-secondary)' }}>{prio}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <p style={{ marginTop: 8, fontSize: '0.74rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                Note: Representative cloud workload types model standard computational profiles for reproducible simulation; real cloud execution is provided by the Render-deployed CPU worker service.
              </p>
            </div>
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
