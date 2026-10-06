import React from 'react';
import { Link } from 'react-router-dom';

/* ── Feature data — no emoji icons ── */
const features = [
  {
    title: 'Deadline-Aware Scheduling',
    desc: 'Prioritizes tasks using a composite score: deadline urgency (50%), task priority (35%), and waiting time (15%). Time-critical jobs are scheduled before their deadlines are breached.',
  },
  {
    title: 'Priority-Based Baseline',
    desc: 'A priority and waiting-time scheduler used as a controlled comparison. Follows existing IEEE-referenced approaches for fair evaluation against the proposed method.',
  },
  {
    title: 'Fibonacci Heap Queue',
    desc: 'Both schedulers use a Fibonacci Heap for task ordering, giving O(log n) extraction time for the highest-priority job in the ready queue.',
  },
  {
    title: 'Dual Execution Engines',
    desc: 'Choose between CloudSim Plus 8.0.0 (simulating 4 cloud datacenter VMs) or a live FastAPI cloud worker on Render Free that executes genuine CPU-bound cryptographic workloads.',
  },
  {
    title: 'Six Performance Metrics',
    desc: 'Reports makespan, average waiting time, average turnaround time, throughput, deadline miss rate, and host/VM utilization for every run.',
  },
  {
    title: 'Side-by-Side Comparison',
    desc: 'Submit one workload and run both schedulers on it simultaneously. Results appear in a parallel view with a pairwise difference table for each metric.',
  },
];

const steps = [
  { n: '1', title: 'Define tasks', desc: 'Enter tasks manually, load the built-in demo workload, or upload a CSV file. Each task has a priority, arrival time, execution time, and deadline.' },
  { n: '2', title: 'Choose engine & scheduler', desc: 'Select Standard Priority, Deadline-Aware, or both. Choose between discrete-event simulation or real CPU execution on the Render cloud worker.' },
  { n: '3', title: 'Execute workload', desc: 'The scheduler orders tasks first, then runs in-memory across 4 VMs or dispatches real CPU workloads to the cloud worker over HTTPS.' },
  { n: '4', title: 'Analyze results', desc: 'View Gantt execution timelines, deadline compliance bars, CPU/VM utilization, and a full task results table.' },
  { n: '5', title: 'Export', desc: 'Download task results and metric summaries as CSV for offline analysis or academic reporting.' },
];

const metricsInfo = [
  { name: 'Makespan', desc: 'Time from the first task arriving until the last task finishes.' },
  { name: 'Avg. Waiting Time', desc: 'Average time each task spent in the queue before a VM picked it up.' },
  { name: 'Avg. Turnaround Time', desc: 'Average time from task arrival to task completion, including both wait and execution.' },
  { name: 'Throughput', desc: 'Number of tasks completed per unit of simulation time.' },
  { name: 'Deadline Miss Rate', desc: 'Percentage of tasks that completed after their stated deadline.' },
  { name: 'VM Utilization', desc: 'Share of total available VM time actually spent executing tasks.' },
];

/* ── Scheduler preview card — shows realistic simulation output ── */
const HeroPreview: React.FC = () => {
  // Representative output from the demo 10-task workload
  const metrics = [
    { label: 'Makespan', value: '28.40 s', sub: 'total time' },
    { label: 'Miss Rate', value: '0.0%', sub: 'deadlines met', color: 'var(--success)' },
    { label: 'Utilization', value: '71.3%', sub: 'VM usage', color: 'var(--cyan)' },
  ];

  // Simplified timeline: 4 VMs, task blocks as % of makespan
  const vms = [
    { id: 'VM 0', blocks: [{ left: '0%', width: '55%', color: '#3b82f6', label: 'T0' }] },
    { id: 'VM 1', blocks: [
      { left: '3.5%', width: '28%', color: '#10b981', label: 'T1' },
      { left: '36%', width: '23%', color: '#6366f1', label: 'T3' },
    ]},
    { id: 'VM 2', blocks: [
      { left: '8.8%', width: '49%', color: '#f59e0b', label: 'T2' },
    ]},
    { id: 'VM 3', blocks: [
      { left: '21%', width: '22.5%', color: '#a855f7', label: 'T5' },
      { left: '35%', width: '17.5%', color: '#0ea5e9', label: 'T7' },
    ]},
  ];

  return (
    <div className="hero-preview">
      <div className="hero-preview-card">
        {/* Title bar */}
        <div className="hero-preview-bar">
          <div className="hero-preview-dot" style={{ background: '#ef4444' }} />
          <div className="hero-preview-dot" style={{ background: '#f59e0b' }} />
          <div className="hero-preview-dot" style={{ background: '#10b981' }} />
          <span className="hero-preview-title">Simulation Results: Deadline-Aware</span>
        </div>

        <div className="hero-preview-body">
          {/* Metric row */}
          <div className="hero-metric-row">
            {metrics.map(m => (
              <div className="hero-metric" key={m.label}>
                <div className="hero-metric-label">{m.label}</div>
                <div className="hero-metric-value" style={m.color ? { color: m.color } : {}}>
                  {m.value}
                </div>
                <div className="hero-metric-sub">{m.sub}</div>
              </div>
            ))}
          </div>

          {/* VM Timeline */}
          <div className="hero-timeline-label">Execution Timeline</div>
          {vms.map(vm => (
            <div className="hero-vm-row" key={vm.id}>
              <span className="hero-vm-id">{vm.id}</span>
              <div className="hero-vm-track">
                {vm.blocks.map(b => (
                  <div
                    key={b.label}
                    className="hero-vm-block"
                    style={{
                      left: b.left,
                      width: b.width,
                      background: b.color,
                      opacity: 0.85,
                    }}
                  >
                    {b.label}
                  </div>
                ))}
              </div>
            </div>
          ))}

          {/* Legend */}
          <div className="hero-badges-row">
            <div className="hero-badge-item">
              <div className="hero-badge-dot" style={{ background: 'var(--success)' }} />
              <span>5 tasks completed before deadline</span>
            </div>
            <div className="hero-badge-item">
              <div className="hero-badge-dot" style={{ background: 'var(--danger)' }} />
              <span>0 missed</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export const LandingPage: React.FC = () => {
  return (
    <main className="landing-page">
      {/* ── HERO ── */}
      <section className="hero">
        <div className="container" style={{ display: 'contents' }}>
          {/* Left content */}
          <div className="hero-content" style={{ paddingLeft: 24, paddingRight: 12 }}>
            <div className="hero-eyebrow">
              <span className="eyebrow">Cloud Scheduling Research Tool</span>
            </div>

            <h1 className="hero-title">
              Deadline-Aware<br />
              Cloud Task Scheduler
            </h1>

            <p className="hero-subtitle">
              A simulation tool for studying how deadline-urgency scheduling compares to
              standard priority scheduling in a cloud environment. Submit a task workload,
              run both algorithms, and compare six performance metrics side by side.
            </p>

            <div className="hero-cta">
              <Link to="/simulator" className="btn btn-primary btn-lg">
                Open Simulator
              </Link>
              <Link to="/about" className="btn btn-secondary btn-lg">
                How It Works
              </Link>
            </div>
          </div>

          {/* Right: live preview card */}
          <div style={{ paddingRight: 24, paddingLeft: 12 }}>
            <HeroPreview />
          </div>
        </div>
      </section>

      {/* ── WHAT IT DOES ── */}
      <section className="features-section">
        <div className="container">
          <div style={{ textAlign: 'center' }}>
            <div className="section-label">Capabilities</div>
            <h2 className="section-title" style={{ margin: '0 auto 12px' }}>
              What the simulator covers
            </h2>
            <p className="section-subtitle" style={{ margin: '0 auto' }}>
              Built for academic evaluation of cloud scheduling algorithms using CloudSim Plus.
            </p>
          </div>

          <div className="features-grid">
            {features.map(f => (
              <div className="feature-card" key={f.title}>
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
            <h2 className="section-title" style={{ margin: '0 auto 12px' }}>How to use it</h2>
            <p className="section-subtitle" style={{ margin: '0 auto' }}>
              From workload definition to exported results in five steps.
            </p>
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

      {/* ── ALGORITHMS ── */}
      <section className="compare-section">
        <div className="container">
          <div style={{ textAlign: 'center' }}>
            <div className="section-label">Scheduling Algorithms</div>
            <h2 className="section-title" style={{ margin: '0 auto 12px' }}>Two algorithms, same workload</h2>
            <p className="section-subtitle" style={{ margin: '0 auto' }}>
              Both schedulers receive identical task sets so results are directly comparable.
            </p>
          </div>

          <div className="scheduler-compare-grid">
            <div className="scheduler-card baseline-card">
              <div className="scheduler-card-header">
                <div className="scheduler-card-dot" style={{ background: 'var(--baseline-color)' }} />
                <div className="scheduler-card-name">Standard Priority</div>
                <span className="badge badge-baseline">Baseline</span>
              </div>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: 20, lineHeight: 1.65 }}>
                Schedules tasks using weighted task priority and accumulated waiting time.
                Based on existing IEEE-referenced scheduling approaches for cloud systems.
              </p>
              <ul className="scheduler-feature-list">
                <li><span className="feature-dot" style={{ background: 'var(--baseline-color)' }} />Priority-weighted ordering</li>
                <li><span className="feature-dot" style={{ background: 'var(--baseline-color)' }} />Waiting-time influence factor</li>
                <li><span className="feature-dot" style={{ background: 'var(--baseline-color)' }} />Fibonacci Heap priority queue</li>
                <li><span className="feature-dot" style={{ background: 'var(--baseline-color)' }} />Controlled comparison baseline</li>
              </ul>
            </div>

            <div className="scheduler-card proposed-card">
              <div className="scheduler-card-header">
                <div className="scheduler-card-dot" style={{ background: 'var(--proposed-color)' }} />
                <div className="scheduler-card-name">Deadline-Aware</div>
                <span className="badge badge-proposed">Proposed</span>
              </div>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: 20, lineHeight: 1.65 }}>
                Adds deadline urgency to the scheduling score. Tasks approaching their
                deadline receive a higher composite score and are dispatched sooner.
              </p>
              <ul className="scheduler-feature-list">
                <li><span className="feature-dot" style={{ background: 'var(--proposed-color)' }} />Task priority (weight: 0.35)</li>
                <li><span className="feature-dot" style={{ background: 'var(--proposed-color)' }} />Deadline urgency (weight: 0.50)</li>
                <li><span className="feature-dot" style={{ background: 'var(--proposed-color)' }} />Waiting factor (weight: 0.15)</li>
                <li><span className="feature-dot" style={{ background: 'var(--proposed-color)' }} />Fibonacci Heap priority queue</li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      {/* ── METRICS ── */}
      <section className="metrics-section">
        <div className="container">
          <div style={{ textAlign: 'center' }}>
            <div className="section-label">Evaluation Metrics</div>
            <h2 className="section-title" style={{ margin: '0 auto 12px' }}>What gets measured</h2>
            <p className="section-subtitle" style={{ margin: '0 auto' }}>
              Six metrics are computed for every simulation run and included in CSV exports.
            </p>
          </div>
          <div className="metrics-explain-grid">
            {metricsInfo.map(m => (
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
            <div className="cta-title">Try it with the demo workload</div>
            <div className="cta-subtitle">
              The built-in 10-task demo runs both schedulers on a pre-configured workload
              so you can see results immediately without entering any data.
            </div>
            <Link to="/simulator" className="btn btn-primary btn-lg">
              Open Simulator
            </Link>
          </div>
        </div>
      </section>
    </main>
  );
};
