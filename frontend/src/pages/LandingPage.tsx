import React from 'react';
import { Link } from 'react-router-dom';

const features = [
  {
    icon: '⏰',
    bg: 'rgba(0,212,255,0.1)',
    border: 'rgba(0,212,255,0.2)',
    title: 'Deadline-Aware Scheduling',
    desc: 'Dynamically prioritizes tasks using deadline urgency alongside task priority and waiting-time factors for smarter, time-sensitive scheduling.',
  },
  {
    icon: '📊',
    bg: 'rgba(124,58,237,0.1)',
    border: 'rgba(124,58,237,0.2)',
    title: 'Priority-Based Baseline',
    desc: 'An IEEE-inspired priority scheduling approach serving as the controlled comparison baseline for the proposed method.',
  },
  {
    icon: '🌀',
    bg: 'rgba(192,38,211,0.1)',
    border: 'rgba(192,38,211,0.2)',
    title: 'Fibonacci Heap',
    desc: 'Both schedulers use a Fibonacci Heap priority queue, providing O(log n) task extraction for efficient scheduling.',
  },
  {
    icon: '☁️',
    bg: 'rgba(99,102,241,0.1)',
    border: 'rgba(99,102,241,0.2)',
    title: 'CloudSim Plus Engine',
    desc: 'Simulates a real cloud environment with 4 configurable virtual machines running your submitted task workload.',
  },
  {
    icon: '📈',
    bg: 'rgba(245,158,11,0.1)',
    border: 'rgba(245,158,11,0.2)',
    title: '6 Performance Metrics',
    desc: 'Analyze makespan, waiting time, turnaround time, throughput, deadline miss rate, and VM utilization.',
  },
  {
    icon: '⚖️',
    bg: 'rgba(16,185,129,0.1)',
    border: 'rgba(16,185,129,0.2)',
    title: 'Controlled Comparison',
    desc: 'Run both schedulers on identical workloads for a fair, side-by-side objective scientific analysis.',
  },
];

const steps = [
  { n: '01', title: 'Define Workload', desc: 'Enter tasks manually, load a demo, or upload a CSV with priority, arrival time, execution time, and deadline.' },
  { n: '02', title: 'Choose Scheduler', desc: 'Select Standard Priority, Deadline-Aware, or run both for a full side-by-side comparison.' },
  { n: '03', title: 'Run Simulation', desc: 'The CloudSim Plus engine executes your workload across 4 virtual machines in the cloud model.' },
  { n: '04', title: 'Analyze Results', desc: 'View per-VM task timelines, deadline classification, and resource utilization charts.' },
  { n: '05', title: 'Compare & Export', desc: 'Compare makespan, throughput, deadline miss rate, and export results as CSV.' },
];

const metrics = [
  { name: 'Makespan', desc: 'Total time from first task arrival to last task completion across the entire workload.' },
  { name: 'Avg. Waiting Time', desc: 'Average time tasks spend in the queue waiting for a VM to become available.' },
  { name: 'Avg. Turnaround Time', desc: 'Average time from task arrival to task completion, including wait and execution.' },
  { name: 'Throughput', desc: 'Number of tasks completed per unit of simulation time.' },
  { name: 'Deadline Miss Rate', desc: 'Percentage of tasks that completed after their specified deadline time.' },
  { name: 'VM Utilization', desc: 'Fraction of available VM execution capacity used across the simulation run.' },
];

export const LandingPage: React.FC = () => {
  return (
    <main className="landing-page">
      {/* ── HERO ── */}
      <section className="hero">
        {/* Decorative orb */}
        <div className="hero-orb">
          <div className="hero-orb-inner" />
        </div>

        <div className="container">
          <div className="hero-content">
            <div className="hero-eyebrow">
              <span className="eyebrow">⚡ Cloud Scheduling Research Platform</span>
            </div>

            <h1 className="hero-title">
              <span className="gradient-text">Deadline-Aware</span>
              <br />
              Cloud Task Scheduler
            </h1>

            <p className="hero-subtitle">
              Simulate cloud workloads, intelligently prioritize tasks with deadline urgency,
              and analyze scheduling performance across virtual machines in real time.
            </p>

            <div className="hero-cta">
              <Link to="/simulator" className="btn btn-primary btn-xl">
                🚀 Launch Simulator
              </Link>
              <Link to="/about" className="btn btn-secondary btn-lg">
                How It Works →
              </Link>
            </div>

            {/* Pipeline visualization */}
            <div className="pipeline-visual fade-in">
              <div className="pipeline-label">Scheduling Pipeline</div>
              <div className="pipeline-nodes">
                {[
                  { icon: '📋', label: 'Tasks', bg: 'rgba(0,212,255,0.1)', border: 'rgba(0,212,255,0.25)' },
                  { icon: '🧠', label: 'Priority Engine', bg: 'rgba(124,58,237,0.1)', border: 'rgba(124,58,237,0.25)' },
                  { icon: '⏰', label: 'Deadline Score', bg: 'rgba(192,38,211,0.1)', border: 'rgba(192,38,211,0.25)' },
                  { icon: '🌀', label: 'Fibonacci Heap', bg: 'rgba(99,102,241,0.1)', border: 'rgba(99,102,241,0.25)' },
                  { icon: '🖥️', label: 'VM Cluster', bg: 'rgba(16,185,129,0.1)', border: 'rgba(16,185,129,0.25)' },
                ].map((node, i, arr) => (
                  <React.Fragment key={node.label}>
                    <div className="pipeline-node">
                      <div className="pipeline-node-icon" style={{ background: node.bg, borderColor: node.border }}>
                        {node.icon}
                      </div>
                      <span className="pipeline-node-label">{node.label}</span>
                    </div>
                    {i < arr.length - 1 && <span className="pipeline-arrow">→</span>}
                  </React.Fragment>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── FEATURES ── */}
      <section className="features-section">
        <div className="container">
          <div style={{ textAlign: 'center' }}>
            <div className="section-label">Platform Capabilities</div>
            <h2 className="section-title" style={{ margin: '0 auto 12px' }}>
              Built for Cloud Scheduling Analysis
            </h2>
            <p className="section-subtitle" style={{ margin: '0 auto' }}>
              A complete simulation platform for studying and comparing task scheduling approaches.
            </p>
          </div>

          <div className="features-grid">
            {features.map((f, i) => (
              <div className="feature-card fade-in-delay-1" key={f.title} style={{ animationDelay: `${i * 0.07}s` }}>
                <div className="feature-icon" style={{ background: f.bg, borderColor: f.border }}>
                  {f.icon}
                </div>
                <div className="feature-title">{f.title}</div>
                <div className="feature-desc">{f.desc}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── HOW IT WORKS ── */}
      <section className="how-section">
        <div className="container">
          <div style={{ textAlign: 'center' }}>
            <div className="section-label">Workflow</div>
            <h2 className="section-title" style={{ margin: '0 auto 12px' }}>How It Works</h2>
            <p className="section-subtitle" style={{ margin: '0 auto' }}>Five simple steps from workload definition to scheduling insights</p>
          </div>

          <div className="how-steps">
            {steps.map((step, i, arr) => (
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

      {/* ── SCHEDULER COMPARE ── */}
      <section className="compare-section">
        <div className="container">
          <div style={{ textAlign: 'center' }}>
            <div className="section-label">Scheduling Algorithms</div>
            <h2 className="section-title" style={{ margin: '0 auto 12px' }}>Two Approaches, One Workload</h2>
            <p className="section-subtitle" style={{ margin: '0 auto' }}>Study how different strategies handle identical cloud task workloads</p>
          </div>

          <div className="scheduler-compare-grid">
            {/* Baseline */}
            <div className="scheduler-card baseline-card">
              <div className="scheduler-card-header">
                <div className="scheduler-card-dot" style={{ background: 'var(--baseline-color)' }} />
                <div className="scheduler-card-name">Standard Priority</div>
                <span className="badge badge-baseline">Baseline</span>
              </div>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: 20, lineHeight: 1.65 }}>
                An existing IEEE-inspired priority-based scheduling approach that uses task
                priority and waiting-time information for job ordering across VMs.
              </p>
              <ul className="scheduler-feature-list">
                <li><span className="feature-dot" style={{ background: 'var(--baseline-color)' }} />Priority-weighted task ordering</li>
                <li><span className="feature-dot" style={{ background: 'var(--baseline-color)' }} />Waiting-time influence factor</li>
                <li><span className="feature-dot" style={{ background: 'var(--baseline-color)' }} />Fibonacci Heap extraction</li>
                <li><span className="feature-dot" style={{ background: 'var(--baseline-color)' }} />Controlled scientific baseline</li>
              </ul>
            </div>

            {/* Proposed */}
            <div className="scheduler-card proposed-card">
              <div className="scheduler-card-header">
                <div className="scheduler-card-dot" style={{ background: 'var(--proposed-color)' }} />
                <div className="scheduler-card-name">Deadline-Aware</div>
                <span className="badge badge-proposed">Proposed</span>
              </div>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: 20, lineHeight: 1.65 }}>
                The proposed approach that incorporates deadline urgency into the scoring formula,
                dynamically adjusting task priority based on how close a task is to its deadline.
              </p>
              <ul className="scheduler-feature-list">
                <li><span className="feature-dot" style={{ background: 'var(--proposed-color)' }} />Task priority (weight: 0.35)</li>
                <li><span className="feature-dot" style={{ background: 'var(--proposed-color)' }} />Deadline urgency (weight: 0.50)</li>
                <li><span className="feature-dot" style={{ background: 'var(--proposed-color)' }} />Waiting factor (weight: 0.15)</li>
                <li><span className="feature-dot" style={{ background: 'var(--proposed-color)' }} />Fibonacci Heap extraction</li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      {/* ── METRICS ── */}
      <section className="metrics-section">
        <div className="container">
          <div style={{ textAlign: 'center' }}>
            <div className="section-label">Evaluation</div>
            <h2 className="section-title" style={{ margin: '0 auto 12px' }}>Performance Metrics</h2>
            <p className="section-subtitle" style={{ margin: '0 auto' }}>Six quantitative measures for a complete scheduling performance analysis</p>
          </div>
          <div className="metrics-explain-grid">
            {metrics.map(m => (
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
            <div className="cta-title">
              Ready to simulate a <span className="gradient-text">cloud workload?</span>
            </div>
            <div className="cta-subtitle">
              Load the 10-task demo workload, run both schedulers, and explore full metric analysis in seconds.
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
