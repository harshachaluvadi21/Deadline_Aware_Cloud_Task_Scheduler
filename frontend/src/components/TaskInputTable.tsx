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
  tasks,
  onTasksChange,
  onLoadSample,
  onLoadDemo,
  onOpenUploadModal,
  onReset,
  disabled
}) => {
  const handleFieldChange = (index: number, field: keyof TaskDto, value: number) => {
    const updated = [...tasks];
    updated[index] = { ...updated[index], [field]: value };
    onTasksChange(updated);
  };

  const handleAddTask = () => {
    const nextId = tasks.length > 0 ? Math.max(...tasks.map(t => t.taskId)) + 1 : 0;
    const newTask: TaskDto = {
      taskId: nextId,
      priority: 5,
      arrivalTime: 0.0,
      executionTime: 10.0,
      deadline: 30.0
    };
    onTasksChange([...tasks, newTask]);
  };

  const handleRemoveTask = (index: number) => {
    const updated = tasks.filter((_, i) => i !== index);
    onTasksChange(updated);
  };

  return (
    <div className="card">
      <div className="card-title" style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'center', gap: '10px' }}>
        <div>
          <span style={{ fontSize: '1.25rem', fontWeight: 700, color: '#f8fafc' }}>
            Step 1: Add Your Tasks
          </span>
          <span style={{ fontSize: '0.85rem', color: '#38bdf8', marginLeft: '10px', fontWeight: 500 }}>
            ({tasks.length} {tasks.length === 1 ? 'task' : 'tasks'} configured)
          </span>
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
          {onOpenUploadModal && (
            <button
              className="btn btn-secondary"
              onClick={onOpenUploadModal}
              disabled={disabled}
              type="button"
              title="Upload CSV workload file with validation"
            >
              📁 Upload Tasks
            </button>
          )}

          {onLoadDemo && (
            <button
              className="btn btn-secondary"
              onClick={onLoadDemo}
              disabled={disabled}
              type="button"
              style={{ borderColor: 'rgba(56, 189, 248, 0.4)', color: '#38bdf8' }}
              title="Load 10-task deterministic demo workload"
            >
              ✨ Load Demo
            </button>
          )}

          <button
            className="btn btn-secondary"
            onClick={onLoadSample}
            disabled={disabled}
            type="button"
            title="Load default 5-task sample workload"
          >
            Load Sample
          </button>

          <button
            className="btn btn-primary"
            onClick={handleAddTask}
            disabled={disabled}
            type="button"
            title="Append a new task row"
          >
            + Add Task
          </button>

          {onReset && (
            <button
              className="btn btn-secondary"
              style={{ color: '#f87171', borderColor: 'rgba(239, 68, 68, 0.4)' }}
              onClick={onReset}
              disabled={disabled}
              type="button"
              title="Reset all tasks, results, and filters"
            >
              Reset
            </button>
          )}
        </div>
      </div>
      <p className="card-subtitle" style={{ margin: '4px 0 12px' }}>
        Enter the tasks you want to schedule in the cloud simulation.
      </p>

      {/* Beginner Helper Tip */}
      <div style={{
        padding: '8px 14px',
        backgroundColor: 'rgba(56, 189, 248, 0.08)',
        borderLeft: '3px solid #38bdf8',
        borderRadius: '4px',
        fontSize: '0.84rem',
        color: '#cbd5e1',
        marginBottom: '16px',
        display: 'flex',
        alignItems: 'center',
        gap: '8px'
      }}>
        <span>💡</span>
        <span>
          <strong>New here?</strong> Click <em>"Load Demo"</em> to try the scheduler without entering tasks manually.
        </span>
      </div>

      {tasks.length === 0 ? (
        <div style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)' }}>
          No tasks in workload. Click <strong>"✨ Load Demo"</strong>, <strong>"Load Sample"</strong>, or <strong>"+ Add Task"</strong> to start.
        </div>
      ) : (
        <div className="table-responsive">
          <table>
            <thead>
              <tr>
                <th style={{ width: '90px' }} title="Unique identifier for the task.">
                  Task ID ⓘ
                  <div style={{ fontSize: '0.7rem', fontWeight: 400, color: '#94a3b8' }}>Unique identifier</div>
                </th>
                <th style={{ width: '120px' }} title="Higher value means higher base priority. Range 1 to 10.">
                  Priority (1–10) ⓘ
                  <div style={{ fontSize: '0.7rem', fontWeight: 400, color: '#94a3b8' }}>Higher = more urgent</div>
                </th>
                <th style={{ width: '130px' }} title="When the task enters the system (in seconds).">
                  Arrival Time (s) ⓘ
                  <div style={{ fontSize: '0.7rem', fontWeight: 400, color: '#94a3b8' }}>When task enters</div>
                </th>
                <th style={{ width: '140px' }} title="Estimated time required to execute the task (in seconds).">
                  Execution Time (s) ⓘ
                  <div style={{ fontSize: '0.7rem', fontWeight: 400, color: '#94a3b8' }}>Execution duration</div>
                </th>
                <th style={{ width: '130px' }} title="Target time by which the task should finish (in seconds).">
                  Deadline (s) ⓘ
                  <div style={{ fontSize: '0.7rem', fontWeight: 400, color: '#94a3b8' }}>Target finish time</div>
                </th>
                <th style={{ width: '80px', textAlign: 'center' }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {tasks.map((task, index) => (
                <tr key={index}>
                  <td>
                    <input
                      type="number"
                      value={task.taskId}
                      onChange={(e) => handleFieldChange(index, 'taskId', parseInt(e.target.value) || 0)}
                      disabled={disabled}
                      min="0"
                    />
                  </td>
                  <td>
                    <input
                      type="number"
                      value={task.priority}
                      onChange={(e) => handleFieldChange(index, 'priority', parseInt(e.target.value) || 1)}
                      disabled={disabled}
                      min="1"
                      max="10"
                    />
                  </td>
                  <td>
                    <input
                      type="number"
                      step="0.1"
                      value={task.arrivalTime}
                      onChange={(e) => handleFieldChange(index, 'arrivalTime', parseFloat(e.target.value) || 0)}
                      disabled={disabled}
                      min="0"
                    />
                  </td>
                  <td>
                    <input
                      type="number"
                      step="0.1"
                      value={task.executionTime}
                      onChange={(e) => handleFieldChange(index, 'executionTime', parseFloat(e.target.value) || 0.1)}
                      disabled={disabled}
                      min="0.1"
                    />
                  </td>
                  <td>
                    <input
                      type="number"
                      step="0.1"
                      value={task.deadline}
                      onChange={(e) => handleFieldChange(index, 'deadline', parseFloat(e.target.value) || 0)}
                      disabled={disabled}
                      min="0"
                    />
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <button
                      className="btn btn-danger"
                      style={{ padding: '4px 10px', fontSize: '0.8rem' }}
                      onClick={() => handleRemoveTask(index)}
                      disabled={disabled}
                      type="button"
                    >
                      Delete
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
