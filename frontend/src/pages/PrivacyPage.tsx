import React from 'react';
import { Link } from 'react-router-dom';

export const PrivacyPage: React.FC = () => {
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
            Privacy Policy
          </h1>
          <p style={{ fontSize: '0.92rem', color: 'var(--text-secondary)', lineHeight: 1.6 }}>
            Last updated: October 2026. This platform is an educational and research simulation tool.
          </p>
        </div>

        <section className="about-section">
          <h2 className="section-heading">1. Overview</h2>
          <div className="card">
            <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.7 }}>
              CloudSched is an open-source academic simulation platform designed to study cloud task
              scheduling algorithms. We prioritize user privacy and minimize data collection. This
              policy explains how information is handled when you use this website.
            </p>
          </div>
        </section>

        <section className="about-section">
          <h2 className="section-heading">2. Information Collection and Storage</h2>
          <div className="card">
            <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.7, marginBottom: 12 }}>
              <strong>Simulation Workload Data:</strong> Any task parameters, CSV files, or simulation
              configurations you submit are processed solely in-memory or by the simulation API to produce
              scheduling metrics and timeline outputs. We do not store or persist your workload data to permanent storage.
            </p>
            <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.7, marginBottom: 12 }}>
              <strong>Browser Storage:</strong> The application may use local browser storage (such as session storage)
              strictly to maintain UI preferences (such as selected algorithms or temporary table state).
            </p>
            <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.7 }}>
              <strong>Personal Data:</strong> We do not require registration, login, or any personally identifiable information (PII).
            </p>
          </div>
        </section>

        <section className="about-section">
          <h2 className="section-heading">3. Third-Party Services and Analytics</h2>
          <div className="card">
            <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.7 }}>
              This site does not employ invasive tracking cookies, third-party advertising networks,
              or tracking pixels. Web fonts are fetched from Google Fonts via standard CDN caching.
            </p>
          </div>
        </section>

        <section className="about-section">
          <h2 className="section-heading">4. Contact & Inquiries</h2>
          <div className="card">
            <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.7 }}>
              If you have questions regarding this privacy policy or the research platform, please visit our
              project repository on GitHub or review the documentation.
            </p>
            <div style={{ marginTop: 16 }}>
              <Link to="/about" className="btn btn-secondary btn-sm">
                View Documentation
              </Link>
            </div>
          </div>
        </section>
      </div>
    </main>
  );
};
