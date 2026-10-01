import React from 'react';
import { TaskResultDto } from '../types/simulation';

interface TaskResultsTableProps {
  tasks: TaskResultDto[];
  totalTasksCount?: number;
  title?: string;
  schedulerName?: string;
}

export const TaskResultsTable: React.FC<TaskResultsTableProps> = ({
  tasks, totalTasksCount, title, schedulerName,
}) => {
  const isFiltered = totalTasksCount !== undefined && totalTasksCount !== tasks.length;

  return (
    <div className="card" style={{ marginBottom: 16 }}>
      <div className="card-title">
        <span>
          {title || 'Task Results'}
          {isFiltered && (
            <span style={{ fontSize: '0.75rem', color: 'var(--cyan)', marginLeft: 8, fontWeight: 500 }}>
              ({tasks.length} of {totalTasksCount})
            </span>
          )}
        </span>
        {schedulerName && (
          <span className={`badge ${schedulerName === 'BASELINE' ? 'badge-baseline' : 'badge-proposed'}`}>
            {schedulerName === 'PROPOSED' ? 'Deadline-Aware' : 'Standard Priority'}
          </span>
        )}
      </div>
      <p className="card-subtitle">Start time, VM assignment, completion time, and deadline status per task.</p>

      {tasks.length === 0 ? (
        <div style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)' }}>
          No tasks match the selected filters.
        </div>
      ) : (
        <div className="data-table-wrap" style={{ maxHeight: 340, overflowY: 'auto' }}>
          <table className="data-table">
            <thead>
              <tr style={{ position: 'sticky', top: 0, zIndex: 1, background: 'var(--bg-secondary)' }}>
                <th>ID</th>
                <th>Pri</th>
                <th>Arrival</th>
                <th>Exec</th>
                <th>Deadline</th>
                <th>VM</th>
                <th>Start</th>
                <th>Done</th>
                <th>Wait</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {tasks.map(t => (
                <tr key={t.taskId}>
                  <td className="mono">T{t.taskId}</td>
                  <td>{t.priority}</td>
                  <td className="mono">{t.arrivalTime.toFixed(2)}</td>
                  <td className="mono">{t.executionTime.toFixed(2)}</td>
                  <td className="mono">{t.deadline.toFixed(2)}</td>
                  <td>
                    <span className="badge badge-cyan" style={{ fontSize: '0.65rem', padding: '2px 6px' }}>
                      VM {t.assignedVmId}
                    </span>
                  </td>
                  <td className="mono">{t.startTime.toFixed(2)}</td>
                  <td className="mono">{t.completionTime.toFixed(2)}</td>
                  <td className="mono">{t.waitingTime.toFixed(2)}</td>
                  <td>
                    <span className={`badge ${t.deadlineMissed ? 'badge-danger' : 'badge-success'}`} style={{ fontSize: '0.65rem', padding: '2px 6px' }}>
                      {t.deadlineMissed ? 'MISSED' : 'OK'}
                    </span>
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
