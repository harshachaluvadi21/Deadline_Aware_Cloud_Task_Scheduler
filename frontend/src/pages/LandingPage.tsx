import React from 'react';
import { Link } from 'react-router-dom';

export const LandingPage: React.FC = () => {
  return (
    <main className="landing-page">
      {/* ── HERO ── */}
      <section className="hero">
        <div className="container">
          <div className="hero-eyebrow">
            <span>⚡</span>
            <span>Cloud Scheduling Research Platform</span>
          </div>

          <h1 className="hero-title">
            <span className="hero-title-accent">Deadline-Aware</span>
            <br />
            Cloud Task Scheduler
          </h1>

          <p className="hero-subtitle">
            Simulate cloud workloads, intelligently prioritize tasks, and analyze
            scheduling performance across virtual machines.
          </p>

          <div className="hero-cta">
            <Link to="/simulator" className="btn btn-primary btn-xl">
              🚀 Launch Simulator
            </Link>
            <Link to="/about" className="btn btn-secondary btn-lg">
              How It Works
            </Link>
          </div>

          {/* Pipeline visualization */}
          <div className="pipeline-visual fade-in">
            <div
              style={{
                fontSize: '0.72rem',
                fontWeight: 700,
                letterSpacing: '0.1em',
                textTransform: 'uppercase',
                color: 'var(--text-muted)',
                marginBottom: 20,
                textAlign: 'center',
              }}
            >
              Scheduling Pipeline
            </div>
            <div className="pipeline-nodes">
              <div className="pipeline-node">
                <div
                  className="pipeline-node-icon"
                  style={{ background: 'rgba(56,189,248,0.1)', borderColor: 'rgba(56,189,248,0.3)' }}
                >
                  📋
                </div>
                <span className="pipeline-node-label">Tasks</span>
              </div>
              <span className="pipeline-arrow">→</span>
              <div className="pipeline-node">
                <div
                  className="pipeline-node-icon"
                  style={{ background: 'rgba(129,140,248,0.1)', borderColor: 'rgba(129,140,248,0.3)' }}
                >
                  🧠
                </div>
                <span className="pipeline-node-label">Priority Engine</span>
              </div>
              <span className="pipeline-arrow">→</span>
              <div className="pipeline-node">
                <div
                  className="pipeline-node-icon"
                  style={{ background: 'rgba(34,211,238,0.1)', borderColor: 'rgba(34,211,238,0.3)' }}
                >
                  ⏰
                </div>
                <span className="pipeline-node-label">Deadline Analysis</span>
              </div>
              <span className="pipeline-arrow">→</span>
              <div className="pipeline-node">
                <div
                  className="pipeline-node-icon"
                  style={{ background: 'rgba(167,139,250,0.1)', borderColor: 'rgba(167,139,250,0.3)' }}
                >
                  🌀
                </div>
                <span className="pipeline-node-label">Fibonacci Heap</span>
              </div>
              <span className="pipeline-arrow">→</span>
              <div className="pipeline-node">
                <div
                  className="pipeline-node-icon"
                  style={{ background: 'rgba(16,185,129,0.1)', borderColor: 'rgba(16,185,129,0.3)' }}
                >
                  🖥️
                </div>
                <span className="pipeline-node-label">VM Cluster</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── FEATURES ── */}
      <section className="features-section">
        <div className="container">
          <div style={{ textAlign: 'center', marginBottom: 40 }}>
            <h2 className="section-heading">Built for Cloud Scheduling Analysis</h2>
            <p className="section-subheading" style={{ maxWidth: 520, margin: '4px auto 0' }}>
              A complete simulation platform for studying and comparing task scheduling approaches.
            </p>
          </div>

          <div className="features-grid">
            {[
              {
                icon: '⏰',
                bg: 'rgba(56,189,248,0.12)',
                title: 'Deadline-Aware Scheduling',
                desc: 'Prioritizes tasks using deadline urgency together with task priority and waiting information for smarter scheduling decisions.',
              },
              {
                icon: '📊',
                bg: 'rgba(129,140,248,0.12)',
                title: 'Priority-Based Scheduling',
                desc: 'Provides a baseline priority-based scheduling approach for controlled comparison against the proposed method.',
              },
              {
                icon: '🌀',
                bg: 'rgba(167,139,250,0.12)',
                title: 'Fibonacci Heap',
                desc: 'Uses a Fibonacci Heap priority structure for efficient O(log n) task extraction during scheduling.',
              },
              {
                icon: '☁️',
                bg: 'rgba(34,211,238,0.12)',
                title: 'CloudSim Plus',
                desc: 'Runs scheduling experiments in a simulated cloud environment with multiple configurable virtual machines.',
              },
              {
                icon: '📈',
                bg: 'rgba(245,158,11,0.12)',
                title: 'Performance Metrics',
                desc: 'Analyze makespan, waiting time, turnaround time, throughput, deadline miss rate, and VM utilization.',
              },
              {
                icon: '⚖️',
                bg: 'rgba(16,185,129,0.12)',
                title: 'Controlled Comparison',
                desc: 'Run both scheduling approaches using identical workloads for a fair, objective side-by-side analysis.',
              },
            ].map((f) => (
              <div className="feature-card" key={f.title}>
                <div className="feature-card-icon" style={{ background: f.bg }}>
                  {f.icon}
                </div>
                <div className="feature-card-title">{f.title}</div>
                <div className="feature-card-desc">{f.desc}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── HOW IT WORKS ── */}
      <section className="how-section">
        <div className="container">
          <div style={{ textAlign: 'center', marginBottom: 48 }}>
            <h2 className="section-heading">How It Works</h2>
            <p className="section-subheading">Five simple steps from workload to insights</p>
          </div>

          <div className="how-steps">
            {[
              { n: '01', title: 'Define Workload', desc: 'Enter tasks or upload a CSV with priority, arrival time, execution time, and deadlines.' },
              { n: '02', title: 'Choose Scheduler', desc: 'Select Standard Priority, Deadline-Aware, or run both for a comparison.' },
              { n: '03', title: 'Run Simulation', desc: 'The CloudSim Plus engine executes your workload across virtual machines.' },
              { n: '04', title: 'Analyze Execution', desc: 'View per-VM task timelines, deadline status, and resource utilization.' },
              { n: '05', title: 'Compare Metrics', desc: 'Compare makespan, waiting time, throughput, and deadline miss rate.' },
            ].map((step, i, arr) => (
              <React.Fragment key={step.n}>
                <div className="how-step">
                  <div className="how-step-num">{step.n}</div>
                  <div className="how-step-title">{step.title}</div>
                  <div className="how-step-desc">{step.desc}</div>
                </div>
                {i < arr.length - 1 && <div className="how-connector" />}
              </React.Fragment>
            ))}
          </div>
        </div>
      </section>

      {/* ── SCHEDULER COMPARISON ── */}
      <section className="compare-section">
        <div className="container">
          <div style={{ textAlign: 'center', marginBottom: 40 }}>
            <h2 className="section-heading">Two Scheduling Approaches</h2>
            <p className="section-subheading">Study how different strategies handle the same workload</p>
          </div>

          <div className="scheduler-compare-grid">
            <div className="scheduler-card baseline-card">
              <div className="scheduler-card-header">
                <div className="scheduler-card-dot" style={{ background: 'var(--baseline-color)' }} />
                <div className="scheduler-card-name">Standard Priority</div>
                <span className="badge badge-baseline">Baseline</span>
              </div>
              <p style={{ fontSize: '0.84rem', color: 'var(--text-secondary)', marginBottom: 16, lineHeight: 1.6 }}>
                An existing priority-based scheduling approach that uses waiting-time information
                for task ordering. Serves as the baseline for comparison.
              </p>
              <ul className="scheduler-feature-list">
                <li><span className="feature-dot" style={{ background: 'var(--baseline-color)' }} />Priority-based task ordering</li>
                <li><span className="feature-dot" style={{ background: 'var(--baseline-color)' }} />Waiting-time factor</li>
                <li><span className="feature-dot" style={{ background: 'var(--baseline-color)' }} />Controlled baseline for comparison</li>
              </ul>
            </div>

            <div className="scheduler-card proposed-card">
              <div className="scheduler-card-header">
                <div className="scheduler-card-dot" style={{ background: 'var(--proposed-color)' }} />
                <div className="scheduler-card-name">Deadline-Aware</div>
                <span className="badge badge-proposed">Proposed</span>
              </div>
              <p style={{ fontSize: '0.84rem', color: 'var(--text-secondary)', marginBottom: 16, lineHeight: 1.6 }}>
                A scheduling approach that incorporates deadline urgency into the scoring formula,
                alongside task priority and waiting factor for dynamic scheduling decisions.
              </p>
              <ul className="scheduler-feature-list">
                <li><span className="feature-dot" style={{ background: 'var(--proposed-color)' }} />Task priority (weight: 0.35)</li>
                <li><span className="feature-dot" style={{ background: 'var(--proposed-color)' }} />Deadline urgency (weight: 0.50)</li>
                <li><span className="feature-dot" style={{ background: 'var(--proposed-color)' }} />Waiting factor (weight: 0.15)</li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      {/* ── METRICS ── */}
      <section className="metrics-section">
        <div className="container">
          <div style={{ textAlign: 'center', marginBottom: 40 }}>
            <h2 className="section-heading">Performance Metrics Explained</h2>
            <p className="section-subheading">Understand what each measurement means</p>
          </div>

          <div className="metrics-explain-grid">
            {[
              { name: 'Makespan', desc: 'Total time required to complete the entire workload from start to finish.' },
              { name: 'Average Waiting Time', desc: 'Average time tasks spend waiting in the queue before execution begins.' },
              { name: 'Average Turnaround Time', desc: 'Average time from task arrival to task completion, including waiting and execution.' },
              { name: 'Throughput', desc: 'Number of tasks completed per unit of time during the simulation.' },
              { name: 'Deadline Miss Rate', desc: 'Percentage of tasks that completed after their specified deadline.' },
              { name: 'VM Resource Utilization', desc: 'Percentage of available VM execution capacity used during the simulation.' },
            ].map((m) => (
              <div className="metrics-explain-card" key={m.name}>
                <div className="metrics-explain-name">{m.name}</div>
                <div className="metrics-explain-desc">{m.desc}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── CTA ── */}
      <section className="cta-section">
        <div className="container">
          <div className="cta-box">
            <div className="cta-title">Ready to simulate a workload?</div>
            <div className="cta-subtitle">
              Load a demo workload and run both schedulers in seconds.
            </div>
            <Link to="/simulator" className="btn btn-primary btn-xl">
              Launch Simulator →
            </Link>
          </div>
        </div>
      </section>
    </main>
  );
};
