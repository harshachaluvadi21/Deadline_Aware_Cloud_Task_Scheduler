import React from 'react';

interface HeaderProps {
  backendConnected: boolean | null;
  isRunning: boolean;
}

export const Header: React.FC<HeaderProps> = ({ backendConnected, isRunning }) => {
  return (
    <div>
      <header className="header-wrapper">
        <div>
          <h1 className="header-title">
            Deadline-Aware Cloud Task Scheduler
          </h1>
          <p className="header-subtitle">
            Simulate cloud tasks, assign them to virtual machines, and compare scheduling approaches.
          </p>
        </div>

        <div className="header-badges">
          {isRunning && (
            <span className="badge badge-info">
              <span className="status-dot"></span>
              Simulating...
            </span>
          )}
          
          {backendConnected === true && (
            <span className="badge badge-online" title="Connected to Spring Boot backend server on port 8080">
              <span className="status-dot"></span>
              Backend Connected (:8080)
            </span>
          )}

          {backendConnected === false && (
            <span className="badge badge-offline" title="Backend server is not reachable. Ensure Spring Boot is running on port 8080.">
              <span className="status-dot"></span>
              Backend Offline
            </span>
          )}

          {backendConnected === null && (
            <span className="badge badge-info">
              <span className="status-dot"></span>
              Connecting...
            </span>
          )}
        </div>
      </header>

      {/* Visual Workflow Steps Bar */}
      <div style={{
        margin: '16px 0 24px',
        padding: '12px 20px',
        background: '#0f172a',
        border: '1px solid #334155',
        borderRadius: '8px',
        display: 'flex',
        flexWrap: 'wrap',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: '12px',
        fontSize: '0.88rem',
        color: '#94a3b8'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#38bdf8', fontWeight: 600 }}>
          <span>① Add Tasks</span>
        </div>
        <span style={{ color: '#475569' }}>→</span>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#e2e8f0', fontWeight: 600 }}>
          <span>② Choose Scheduler</span>
        </div>
        <span style={{ color: '#475569' }}>→</span>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#e2e8f0', fontWeight: 600 }}>
          <span>③ Run Simulation</span>
        </div>
        <span style={{ color: '#475569' }}>→</span>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#e2e8f0', fontWeight: 600 }}>
          <span>④ View Results</span>
        </div>
      </div>
    </div>
  );
};
