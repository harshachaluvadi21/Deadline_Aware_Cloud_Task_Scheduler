import React from 'react';
import type { AlgorithmMode } from '../types/simulation';

interface Props {
  selected: AlgorithmMode;
  onChange: (a: AlgorithmMode) => void;
  disabled?: boolean;
}

const OPTIONS: { value: AlgorithmMode; title: string; desc: string; badge: string }[] = [
  {
    value: 'BASELINE',
    title: '📊 Standard Priority',
    desc: 'Run only the baseline IEEE priority-based scheduler.',
    badge: 'Baseline',
  },
  {
    value: 'PROPOSED',
    title: '⏰ Deadline-Aware',
    desc: 'Run only the proposed deadline-urgency scheduler.',
    badge: 'Proposed',
  },
  {
    value: 'COMPARE',
    title: '⚖️ Compare Both',
    desc: 'Run both schedulers on identical workloads for side-by-side analysis.',
    badge: 'Recommended',
  },
];

export const AlgorithmSelector: React.FC<Props> = ({ selected, onChange, disabled }) => {
  const getClass = (val: AlgorithmMode) => {
    if (val !== selected) return 'algo-option';
    if (val === 'BASELINE') return 'algo-option selected-baseline';
    if (val === 'PROPOSED') return 'algo-option selected-proposed';
    return 'algo-option selected-compare';
  };

  return (
    <div className="algo-selector">
      {OPTIONS.map(opt => (
        <button
          key={opt.value}
          className={getClass(opt.value)}
          onClick={() => onChange(opt.value)}
          disabled={disabled}
          style={{ cursor: disabled ? 'not-allowed' : 'pointer' }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
            <div className="algo-title">{opt.title}</div>
            <span className={`badge ${opt.value === 'BASELINE' ? 'badge-baseline' : opt.value === 'PROPOSED' ? 'badge-proposed' : 'badge-cyan'}`}>
              {opt.badge}
            </span>
          </div>
          <div className="algo-desc">{opt.desc}</div>
        </button>
      ))}
    </div>
  );
};
