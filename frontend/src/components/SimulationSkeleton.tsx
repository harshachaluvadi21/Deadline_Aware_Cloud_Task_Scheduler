import React from 'react';

export const SimulationSkeleton: React.FC = () => (
  <div className="fade-in" style={{ marginTop: 16 }}>
    {/* Overview skeleton */}
    <div className="skeleton-card" style={{ marginBottom: 16 }}>
      <div className="skeleton skeleton-line h-lg w-40" />
      <div className="skeleton skeleton-line w-75" />
      <div className="skeleton skeleton-line w-50" />
      <div className="skeleton-grid">
        <div className="skeleton skeleton-metric-card" />
        <div className="skeleton skeleton-metric-card" />
        <div className="skeleton skeleton-metric-card" />
      </div>
    </div>

    {/* Comparison table skeleton */}
    <div className="skeleton-card" style={{ marginBottom: 16 }}>
      <div className="skeleton skeleton-line h-lg w-50" />
      <div className="skeleton skeleton-line w-90" />
      <div className="skeleton skeleton-line w-90" />
      <div className="skeleton skeleton-line w-75" />
      <div className="skeleton skeleton-line w-90" />
      <div className="skeleton skeleton-line w-75" />
      <div className="skeleton skeleton-line w-90" />
    </div>

    {/* Charts skeleton */}
    <div className="skeleton-card">
      <div className="skeleton skeleton-line h-lg w-40" />
      <div className="skeleton-grid" style={{ gridTemplateColumns: 'repeat(2, 1fr)' }}>
        <div className="skeleton skeleton-metric-card" style={{ height: 140 }} />
        <div className="skeleton skeleton-metric-card" style={{ height: 140 }} />
      </div>
    </div>
  </div>
);
