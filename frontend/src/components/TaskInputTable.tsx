import React from 'react';
import { TaskDto } from '../types/simulation';

interface TaskInputTableProps {
  tasks: TaskDto[];
  onTasksChange: (tasks: TaskDto[]) => void;
  onLoadSample: () => void;
  onLoadDemo?: () => void;
  onOpenUploadModal?: () => void;
  onReset?: () => void;
  disabled?: boolean;
}

export const TaskInputTable: React.FC<TaskInputTableProps> = ({
  tasks, onTasksChange, onLoadSample, onLoadDemo, onOpenUploadModal, onReset, disabled,
}) => {
  const handleFieldChange = (index: number, field: keyof TaskDto, value: number) => {
    const updated = [...tasks];
    updated[index] = { ...updated[index], [field]: value };
    onTasksChange(updated);
  };

  const handleAddTask = () => {
    const nextId = tasks.length > 0 ? Math.max(...tasks.map(t => t.taskId)) + 1 : 0;
    onTasksChange([...tasks, { taskId: nextId, priority: 5, arrivalTime: 0.0, executionTime: 10.0, deadline: 30.0 }]);
  };

  const handleRemoveTask = (index: number) => {
    onTasksChange(tasks.filter((_, i) => i !== index));
  };

  return (
    <div>
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

      {/* Hint */}
      <div className="alert alert-info" style={{ marginBottom: 16 }}>
        <span className="alert-icon">ℹ</span>
        <span>
          <strong>Reference workload available:</strong> Click <em>"Load Demo Workload"</em> to load the pre-configured 10-task benchmark and compare schedulers immediately.
        </span>
      </div>

      {tasks.length === 0 ? (
        <div style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)', background: 'var(--bg-glass)', borderRadius: 'var(--r-lg)', border: '1px dashed var(--border-md)' }}>
          No tasks defined. Use the buttons above to load a demo or add tasks manually.
        </div>
      ) : (
        <div className="data-table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th title="Unique identifier for the task">Task ID</th>
                <th title="Task priority from 1 (lowest) to 10 (highest)">Priority (1–10)</th>
                <th title="When the task enters the system (seconds)">Arrival Time (s)</th>
                <th title="Time required to run the task (seconds)">Exec. Time (s)</th>
                <th title="Target completion time (seconds)">Deadline (s)</th>
                <th style={{ textAlign: 'center' }}>Remove</th>
              </tr>
            </thead>
            <tbody>
              {tasks.map((task, index) => (
                <tr key={index}>
                  <td>
                    <input
                      type="number" className="task-input" style={{ width: 80 }}
                      value={task.taskId}
                      onChange={e => handleFieldChange(index, 'taskId', parseInt(e.target.value) || 0)}
                      disabled={disabled} min="0"
                    />
                  </td>
                  <td>
                    <input
                      type="number" className="task-input" style={{ width: 80 }}
                      value={task.priority}
                      onChange={e => handleFieldChange(index, 'priority', parseInt(e.target.value) || 1)}
                      disabled={disabled} min="1" max="10"
                    />
                  </td>
                  <td>
                    <input
                      type="number" className="task-input" style={{ width: 100 }} step="0.1"
                      value={task.arrivalTime}
                      onChange={e => handleFieldChange(index, 'arrivalTime', parseFloat(e.target.value) || 0)}
                      disabled={disabled} min="0"
                    />
                  </td>
                  <td>
                    <input
                      type="number" className="task-input" style={{ width: 100 }} step="0.1"
                      value={task.executionTime}
                      onChange={e => handleFieldChange(index, 'executionTime', parseFloat(e.target.value) || 0.1)}
                      disabled={disabled} min="0.1"
                    />
                  </td>
                  <td>
                    <input
                      type="number" className="task-input" style={{ width: 100 }} step="0.1"
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
                    >
                      ✕
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};
