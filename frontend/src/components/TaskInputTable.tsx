import React from 'react';
import { TaskDto } from '../types/simulation';
import { PRESET_SCENARIOS, PresetScenario } from '../data/presetWorkloads';

interface TaskInputTableProps {
  tasks: TaskDto[];
  onTasksChange: (tasks: TaskDto[]) => void;
  onLoadSample: () => void;
  onLoadDemo?: () => void;
  onSelectPreset?: (preset: PresetScenario) => void;
  onOpenUploadModal?: () => void;
  onReset?: () => void;
  disabled?: boolean;
}

export const CLOUD_TASK_CATEGORIES: string[] = [
  'Image Processing',
  'Video Transcoding',
  'Database Query',
  'ML Model Inference',
  'Log Analysis',
  'File Compression',
  'Data Analytics',
  'Backup Processing',
  'Web Request Processing',
  'ETL/Data Pipeline',
  'Report Generation',
  'Document Processing',
];

export function getTaskCategory(task: TaskDto, index: number): string {
  if (task.taskType && typeof task.taskType === 'string' && task.taskType.trim().length > 0) {
    return task.taskType.trim();
  }
  const safeId = Number.isInteger(task.taskId) && task.taskId >= 0 ? task.taskId : index;
  return CLOUD_TASK_CATEGORIES[Math.abs(safeId) % CLOUD_TASK_CATEGORIES.length];
}

export const TaskInputTable: React.FC<TaskInputTableProps> = ({
  tasks, onTasksChange, onLoadSample, onLoadDemo, onSelectPreset, onOpenUploadModal, onReset, disabled,
}) => {
  const handleFieldChange = (index: number, field: keyof TaskDto, value: number) => {
    const updated = [...tasks];
    updated[index] = { ...updated[index], [field]: value };
    onTasksChange(updated);
  };

  const handleAddTask = () => {
    const nextId = tasks.length > 0 ? Math.max(...tasks.map(t => t.taskId)) + 1 : 0;
    const nextCategory = CLOUD_TASK_CATEGORIES[nextId % CLOUD_TASK_CATEGORIES.length];
    onTasksChange([...tasks, {
      taskId: nextId,
      priority: 5,
      arrivalTime: 0.0,
      executionTime: 10.0,
      deadline: 30.0,
      taskType: nextCategory,
    }]);
  };

  const handleRemoveTask = (index: number) => {
    onTasksChange(tasks.filter((_, i) => i !== index));
  };

  return (
    <div>
      {/* Research Presets Bar */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: 8,
        marginBottom: 14,
        padding: '8px 12px',
        background: 'var(--bg-card-subtle, rgba(255, 255, 255, 0.02))',
        borderRadius: 'var(--r-md)',
        border: '1px solid var(--border)',
      }}>
        <span style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: 6 }}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--cyan)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polygon points="12 2 2 7 12 12 22 7 12 2"/>
            <polyline points="2 17 12 22 22 17"/>
            <polyline points="2 12 12 17 22 12"/>
          </svg>
          Presets:
        </span>
        {PRESET_SCENARIOS.map(preset => (
          <button
            key={preset.id}
            type="button"
            className="btn btn-ghost btn-xs"
            onClick={() => onSelectPreset ? onSelectPreset(preset) : onTasksChange(preset.tasks)}
            disabled={disabled}
            title={preset.description}
            style={{
              fontSize: '0.75rem',
              padding: '4px 10px',
              display: 'inline-flex',
              alignItems: 'center',
              borderRadius: 'var(--r-sm)',
            }}
          >
            <span style={{
              width: 6,
              height: 6,
              borderRadius: '50%',
              backgroundColor: (preset.badge === 'High Urgency' || preset.badge === 'Urgency Stress') ? '#f59e0b'
                : preset.badge === 'Preemption' ? '#a855f7'
                : preset.badge === 'Scale-Up' ? '#38bdf8'
                : preset.badge === 'Control' ? '#94a3b8'
                : preset.badge === 'Benchmark' ? '#6366f1'
                : preset.badge === 'Heavy Load' ? '#ef4444' : '#10b981',
              display: 'inline-block',
              marginRight: 6,
            }} />
            {preset.name}
          </button>
        ))}
        <span style={{ marginLeft: 'auto', fontSize: '0.72rem', color: 'var(--text-muted)' }}>
          <kbd style={{ background: 'var(--bg-glass)', border: '1px solid var(--border)', borderRadius: 3, padding: '1px 5px' }}>Ctrl+Enter</kbd> to run
        </span>
      </div>

      {/* Action bar */}
      <div className="task-table-actions">
        {onLoadDemo && (
          <button className="btn btn-secondary btn-sm" onClick={onLoadDemo} disabled={disabled} type="button">
            Load Demo Workload
          </button>
        )}
        <button className="btn btn-ghost btn-sm" onClick={onLoadSample} disabled={disabled} type="button">
          Load Sample
        </button>
        {onOpenUploadModal && (
          <button className="btn btn-ghost btn-sm" onClick={onOpenUploadModal} disabled={disabled} type="button">
            Upload CSV
          </button>
        )}
        <button className="btn btn-primary btn-sm" onClick={handleAddTask} disabled={disabled} type="button">
          + Add Task
        </button>
        {onReset && (
          <button className="btn btn-danger btn-sm" onClick={onReset} disabled={disabled} type="button">
            Reset
          </button>
        )}
      </div>

      {/* Hint — only show when no tasks loaded */}
      {tasks.length === 0 && (
        <div className="alert alert-info" style={{ marginBottom: 14 }}>
          <span className="alert-icon">ℹ</span>
          <span>
            <strong>Reference workload ready:</strong> Click <em>"Load Demo Workload"</em> to load the 10-task benchmark or pick a preset above.
          </span>
        </div>
      )}

      {tasks.length === 0 ? (
        <div style={{ padding: '40px 20px', textAlign: 'center', color: 'var(--text-muted)', background: 'var(--bg-glass)', borderRadius: 'var(--r-lg)', border: '1px dashed var(--border-md)' }}>
          <p style={{ margin: '0 0 10px', fontSize: '0.9rem' }}>No cloud tasks loaded in current workload.</p>
          {onLoadDemo && (
            <button className="btn btn-secondary btn-sm" onClick={onLoadDemo} disabled={disabled} type="button">
              Load 10-Task Demo Workload
            </button>
          )}
        </div>
      ) : (
        <>
          <div className="workload-table-container">
            <table className="data-table workload-data-table">
              <thead>
                <tr>
                  <th title="Unique identifier for the task" style={{ width: 85 }}>Task ID</th>
                  <th title="Task priority from 1 (lowest) to 10 (highest)" style={{ width: 100 }}>Priority (1–10)</th>
                  <th title="When the task enters the system (seconds)" style={{ width: 110 }}>Arrival (s)</th>
                  <th title="Time required to run the task (seconds)" style={{ width: 120 }}>BURST TIME (S)</th>
                  <th title="Target completion time (seconds)" style={{ width: 110 }}>Deadline (s)</th>
                  <th style={{ textAlign: 'center', width: 60 }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {tasks.map((task, index) => (
                  <tr key={index}>
                    <td>
                      <input
                        type="number" className="task-input" style={{ width: '100%' }}
                        value={task.taskId}
                        onChange={e => handleFieldChange(index, 'taskId', parseInt(e.target.value) || 0)}
                        disabled={disabled} min="0"
                      />
                      <div
                        style={{
                          fontSize: '0.68rem',
                          color: 'var(--cyan)',
                          marginTop: 3,
                          whiteSpace: 'nowrap',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          fontWeight: 500,
                        }}
                        title={getTaskCategory(task, index)}
                      >
                        {getTaskCategory(task, index)}
                      </div>
                    </td>
                    <td>
                      <input
                        type="number" className="task-input" style={{ width: '100%' }}
                        value={task.priority}
                        onChange={e => handleFieldChange(index, 'priority', parseInt(e.target.value) || 1)}
                        disabled={disabled} min="1" max="10"
                      />
                    </td>
                    <td>
                      <input
                        type="number" className="task-input" style={{ width: '100%' }} step="0.1"
                        value={task.arrivalTime}
                        onChange={e => handleFieldChange(index, 'arrivalTime', parseFloat(e.target.value) || 0)}
                        disabled={disabled} min="0"
                      />
                    </td>
                    <td>
                      <input
                        type="number" className="task-input" style={{ width: '100%' }} step="0.1"
                        value={task.executionTime}
                        onChange={e => handleFieldChange(index, 'executionTime', parseFloat(e.target.value) || 0.1)}
                        disabled={disabled} min="0.1"
                      />
                    </td>
                    <td>
                      <input
                        type="number" className="task-input" style={{ width: '100%' }} step="0.1"
                        value={task.deadline}
                        onChange={e => handleFieldChange(index, 'deadline', parseFloat(e.target.value) || 0)}
                        disabled={disabled} min="0"
                      />
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <button
                        className="btn btn-danger btn-xs"
                        onClick={() => handleRemoveTask(index)}
                        disabled={disabled} type="button"
                        title="Delete task"
                      >
                        ✕
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Workload Stats Summary Footer */}
          <div className="workload-summary-bar">
            <span><strong>{tasks.length}</strong> Tasks</span>
            <span className="summary-dot">•</span>
            <span>Avg Exec: <strong>{(tasks.reduce((a, b) => a + b.executionTime, 0) / tasks.length).toFixed(1)}s</strong></span>
            <span className="summary-dot">•</span>
            <span>Total Compute: <strong>{tasks.reduce((a, b) => a + b.executionTime, 0).toFixed(1)}s</strong></span>
            <span className="summary-dot">•</span>
            <span>Min Deadline: <strong>{Math.min(...tasks.map(t => t.deadline)).toFixed(1)}s</strong></span>
          </div>
        </>
      )}
    </div>
  );
};
