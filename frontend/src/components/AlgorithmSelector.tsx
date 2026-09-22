import React, { useState } from 'react';
import { AlgorithmMode } from '../types/simulation';

interface AlgorithmSelectorProps {
  selected: AlgorithmMode;
  onChange: (mode: AlgorithmMode) => void;
  disabled?: boolean;
}

export const AlgorithmSelector: React.FC<AlgorithmSelectorProps> = ({ selected, onChange, disabled }) => {
  const [showExplanation, setShowExplanation] = useState(false);

  return (
    <div className="card">
      <div className="card-title">
        <span style={{ fontSize: '1.25rem', fontWeight: 700, color: '#f8fafc' }}>
          Step 2: Choose How Tasks Are Scheduled
        </span>
      </div>
      <p className="card-subtitle" style={{ margin: '4px 0 16px' }}>
        Choose one scheduling method or compare both methods using the same workload.
      </p>

      <div className="algo-selector-grid">
        <div
          className={`algo-option-card ${selected === 'BASELINE' ? 'selected' : ''}`}
          onClick={() => !disabled && onChange('BASELINE')}
          style={{ cursor: disabled ? 'not-allowed' : 'pointer' }}
        >
          <div className="title">
            <input
              type="radio"
              checked={selected === 'BASELINE'}
              onChange={() => {}}
              disabled={disabled}
            />
            <span>Standard Priority Scheduler</span>
          </div>
          <p className="desc">
            Schedules tasks using the existing priority-based approach and waiting-time information.
          </p>
        </div>

        <div
          className={`algo-option-card ${selected === 'PROPOSED' ? 'selected' : ''}`}
          onClick={() => !disabled && onChange('PROPOSED')}
          style={{ cursor: disabled ? 'not-allowed' : 'pointer' }}
        >
          <div className="title">
            <input
              type="radio"
              checked={selected === 'PROPOSED'}
              onChange={() => {}}
              disabled={disabled}
            />
            <span>Deadline-Aware Scheduler</span>
          </div>
          <p className="desc">
            Considers deadline urgency along with task priority and waiting time.
          </p>
        </div>

        <div
          className={`algo-option-card ${selected === 'COMPARE' ? 'selected' : ''}`}
          onClick={() => !disabled && onChange('COMPARE')}
          style={{ cursor: disabled ? 'not-allowed' : 'pointer' }}
        >
          <div className="title">
            <input
              type="radio"
              checked={selected === 'COMPARE'}
              onChange={() => {}}
              disabled={disabled}
            />
            <span>Compare Both</span>
          </div>
          <p className="desc">
            Runs both schedulers on identical tasks for a controlled comparison.
          </p>
        </div>
      </div>

      {/* Expandable "How does it work?" section */}
      <div style={{ marginTop: '16px', paddingTop: '12px', borderTop: '1px solid #334155' }}>
        <button
          type="button"
          onClick={() => setShowExplanation(!showExplanation)}
          style={{
            background: 'none',
            border: 'none',
            color: '#38bdf8',
            cursor: 'pointer',
            padding: 0,
            fontSize: '0.84rem',
            display: 'flex',
            alignItems: 'center',
            gap: '6px'
          }}
        >
          <span>{showExplanation ? '▼' : '▶'}</span>
          <span style={{ textDecoration: 'underline' }}>How does it work?</span>
        </button>

        {showExplanation && (
          <div style={{
            marginTop: '10px',
            padding: '12px 16px',
            backgroundColor: '#0f172a',
            borderRadius: '6px',
            fontSize: '0.84rem',
            color: '#cbd5e1',
            lineHeight: 1.6
          }}>
            <p style={{ margin: '0 0 8px' }}>
              <strong style={{ color: '#f8fafc' }}>Standard Priority Scheduler:</strong> Uses the baseline priority assignment and waiting-time information.
            </p>
            <p style={{ margin: 0 }}>
              <strong style={{ color: '#f8fafc' }}>Deadline-Aware Scheduler:</strong> Combines normalized task priority, deadline urgency, and waiting factor.
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
