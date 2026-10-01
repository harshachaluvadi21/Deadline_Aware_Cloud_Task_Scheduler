import React from 'react';

export interface FilterCriteria {
  vmId: string;
  status: string;
  minPriority: number;
  maxPriority: number;
  deadlineCategory: string;
}

interface TaskFiltersProps {
  filters: FilterCriteria;
  onFilterChange: (filters: FilterCriteria) => void;
  onResetFilters: () => void;
  availableVmIds: number[];
}

export const TaskFilters: React.FC<TaskFiltersProps> = ({
  filters, onFilterChange, onResetFilters, availableVmIds,
}) => {
  const handleChange = (field: keyof FilterCriteria, value: string | number) => {
    onFilterChange({ ...filters, [field]: value });
  };

  return (
    <div className="filters-bar">
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexShrink: 0 }}>
        <span style={{ fontSize: '0.9rem' }}>🔍</span>
        <span style={{ fontWeight: 700, color: 'var(--text-primary)', fontSize: '0.85rem' }}>Filters</span>
      </div>

      <div className="filter-group">
        <span className="filter-label">VM</span>
        <select
          className="filter-select"
          value={filters.vmId}
          onChange={e => handleChange('vmId', e.target.value)}
        >
          <option value="ALL">All VMs</option>
          {availableVmIds.map(vm => (
            <option key={vm} value={vm.toString()}>VM {vm}</option>
          ))}
        </select>
      </div>

      <div className="filter-group">
        <span className="filter-label">Status</span>
        <select
          className="filter-select"
          value={filters.status}
          onChange={e => handleChange('status', e.target.value)}
        >
          <option value="ALL">All</option>
          <option value="SUCCESS">Success</option>
          <option value="MISSED_DEADLINE">Missed</option>
        </select>
      </div>

      <div className="filter-group">
        <span className="filter-label">Deadline</span>
        <select
          className="filter-select"
          value={filters.deadlineCategory}
          onChange={e => handleChange('deadlineCategory', e.target.value)}
        >
          <option value="ALL">All</option>
          <option value="BEFORE_DEADLINE">Before</option>
          <option value="AT_DEADLINE">At</option>
          <option value="MISSED_DEADLINE">Missed</option>
        </select>
      </div>

      <div className="filter-group">
        <span className="filter-label">Priority</span>
        <input
          type="number" min={1} max={10}
          value={filters.minPriority}
          onChange={e => handleChange('minPriority', Math.max(1, parseInt(e.target.value) || 1))}
          style={{ width: 42, padding: '4px 6px', fontSize: '0.8rem', borderRadius: 'var(--r-sm)' }}
        />
        <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>–</span>
        <input
          type="number" min={1} max={10}
          value={filters.maxPriority}
          onChange={e => handleChange('maxPriority', Math.min(10, parseInt(e.target.value) || 10))}
          style={{ width: 42, padding: '4px 6px', fontSize: '0.8rem', borderRadius: 'var(--r-sm)' }}
        />
      </div>

      <button className="btn btn-ghost btn-sm" onClick={onResetFilters}>
        Reset
      </button>
    </div>
  );
};
