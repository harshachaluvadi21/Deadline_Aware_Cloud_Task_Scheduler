import React from 'react';

export interface FilterCriteria {
  vmId: string; // 'ALL' or '0', '1', '2', etc.
  status: string; // 'ALL' or 'SUCCESS' or 'MISSED_DEADLINE'
  minPriority: number;
  maxPriority: number;
  deadlineCategory: string; // 'ALL' | 'BEFORE_DEADLINE' | 'AT_DEADLINE' | 'MISSED_DEADLINE'
}

interface TaskFiltersProps {
  filters: FilterCriteria;
  onFilterChange: (filters: FilterCriteria) => void;
  onResetFilters: () => void;
  availableVmIds: number[];
}

export const TaskFilters: React.FC<TaskFiltersProps> = ({
  filters,
  onFilterChange,
  onResetFilters,
  availableVmIds
}) => {
  const handleChange = (field: keyof FilterCriteria, value: any) => {
    onFilterChange({
      ...filters,
      [field]: value
    });
  };

  return (
    <div style={{
      background: '#0f172a',
      border: '1px solid #334155',
      borderRadius: '8px',
      padding: '14px 18px',
      margin: '20px 0 16px',
      display: 'flex',
      flexDirection: 'column',
      gap: '10px'
    }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'center', gap: '8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '1.1rem' }}>🔍</span>
          <span style={{ fontWeight: 700, color: '#f8fafc', fontSize: '0.95rem' }}>
            Filter Results
          </span>
        </div>
        <p style={{ margin: 0, fontSize: '0.78rem', color: '#94a3b8' }}>
          Use these filters to inspect specific tasks. Filters only change what is displayed and do not rerun the simulation.
        </p>
      </div>

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '14px', alignItems: 'center', paddingTop: '4px' }}>
        {/* VM Filter */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <label style={{ fontSize: '0.8rem', color: '#94a3b8' }}>VM:</label>
          <select
            value={filters.vmId}
            onChange={(e) => handleChange('vmId', e.target.value)}
            style={{
              background: '#1e293b',
              color: '#f8fafc',
              border: '1px solid #475569',
              borderRadius: '4px',
              padding: '4px 8px',
              fontSize: '0.82rem'
            }}
          >
            <option value="ALL">All VMs</option>
            {availableVmIds.map(vm => (
              <option key={vm} value={vm.toString()}>VM {vm}</option>
            ))}
          </select>
        </div>

        {/* Status Filter */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <label style={{ fontSize: '0.8rem', color: '#94a3b8' }}>Status:</label>
          <select
            value={filters.status}
            onChange={(e) => handleChange('status', e.target.value)}
            style={{
              background: '#1e293b',
              color: '#f8fafc',
              border: '1px solid #475569',
              borderRadius: '4px',
              padding: '4px 8px',
              fontSize: '0.82rem'
            }}
          >
            <option value="ALL">All Statuses</option>
            <option value="SUCCESS">Completed (Success)</option>
            <option value="MISSED_DEADLINE">Missed Deadline</option>
          </select>
        </div>

        {/* Deadline Category Filter */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <label style={{ fontSize: '0.8rem', color: '#94a3b8' }}>Deadline Status:</label>
          <select
            value={filters.deadlineCategory}
            onChange={(e) => handleChange('deadlineCategory', e.target.value)}
            style={{
              background: '#1e293b',
              color: '#f8fafc',
              border: '1px solid #475569',
              borderRadius: '4px',
              padding: '4px 8px',
              fontSize: '0.82rem'
            }}
          >
            <option value="ALL">All Categories</option>
            <option value="BEFORE_DEADLINE">Before Deadline</option>
            <option value="AT_DEADLINE">At Deadline</option>
            <option value="MISSED_DEADLINE">Missed Deadline</option>
          </select>
        </div>

        {/* Priority Filter */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <label style={{ fontSize: '0.8rem', color: '#94a3b8' }}>Priority:</label>
          <input
            type="number"
            min={1}
            max={10}
            value={filters.minPriority}
            onChange={(e) => handleChange('minPriority', Math.max(1, parseInt(e.target.value) || 1))}
            style={{
              width: '45px',
              background: '#1e293b',
              color: '#f8fafc',
              border: '1px solid #475569',
              borderRadius: '4px',
              padding: '3px 6px',
              fontSize: '0.82rem'
            }}
          />
          <span style={{ fontSize: '0.8rem', color: '#94a3b8' }}>to</span>
          <input
            type="number"
            min={1}
            max={10}
            value={filters.maxPriority}
            onChange={(e) => handleChange('maxPriority', Math.min(10, parseInt(e.target.value) || 10))}
            style={{
              width: '45px',
              background: '#1e293b',
              color: '#f8fafc',
              border: '1px solid #475569',
              borderRadius: '4px',
              padding: '3px 6px',
              fontSize: '0.82rem'
            }}
          />
        </div>

        {/* Reset button */}
        <button
          className="btn btn-secondary"
          onClick={onResetFilters}
          style={{ padding: '4px 10px', fontSize: '0.78rem' }}
        >
          Reset Filters
        </button>
      </div>
    </div>
  );
};
