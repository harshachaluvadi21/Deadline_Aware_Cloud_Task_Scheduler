import React from 'react';
import { Link } from 'react-router-dom';

export const TermsPage: React.FC = () => {
  return (
    <main className="about-page">
      <div className="container-narrow">
        <div style={{ marginBottom: 40, paddingBottom: 24, borderBottom: '1px solid var(--border)' }}>
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              padding: '4px 10px',
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
            Legal
          </div>
          <h1 style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--text-primary)', letterSpacing: '-0.02em', marginBottom: 8 }}>
            Terms of Service
          </h1>
          <p style={{ fontSize: '0.92rem', color: 'var(--text-secondary)', lineHeight: 1.6 }}>
            Last updated: October 2026. Terms governing the use of the CloudSched simulation platform.
          </p>
        </div>

        <section className="about-section">
          <h2 className="section-heading">1. Acceptance of Terms</h2>
          <div className="card">
            <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.7 }}>
              By accessing and using CloudSched, you agree to these Terms of Service. If you do not agree
              with any part of these terms, please discontinue use of this research simulation platform.
            </p>
          </div>
        </section>

        <section className="about-section">
          <h2 className="section-heading">2. Intended Use and Purpose</h2>
          <div className="card">
            <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.7, marginBottom: 12 }}>
              CloudSched is provided strictly for academic research, education, benchmarking, and algorithm
              demonstration. The simulation environment is powered by CloudSim Plus and models virtual machine
              allocations in simulated execution environments.
            </p>
            <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.7 }}>
              The results produced are simulated performance metrics and should be interpreted within
              the context of the simulation parameters and model assumptions.
            </p>
          </div>
        </section>

        <section className="about-section">
          <h2 className="section-heading">3. Intellectual Property and Open Source</h2>
          <div className="card">
            <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.7 }}>
              The project source code is available under open-source terms. You are free to inspect,
              fork, and contribute in accordance with the repository license. Content and documentation
              are provided for educational evaluation.
            </p>
          </div>
        </section>

        <section className="about-section">
          <h2 className="section-heading">4. Disclaimer of Warranties</h2>
          <div className="card">
            <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.7 }}>
              The software is provided &quot;as is&quot;, without warranty of any kind, express or implied.
              In no event shall the authors or copyright holders be liable for any claim, damages,
              or other liability arising from the use of the software.
            </p>
            <div style={{ marginTop: 16 }}>
              <Link to="/simulator" className="btn btn-primary btn-sm">
                Return to Simulator
              </Link>
            </div>
          </div>
        </section>
      </div>
    </main>
  );
};
