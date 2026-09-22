import React from 'react';
import { TaskResultDto } from '../types/simulation';

interface TaskResultsTableProps {
  tasks: TaskResultDto[];
  totalTasksCount?: number;
  title?: string;
  schedulerName?: string;
}

export const TaskResultsTable: React.FC<TaskResultsTableProps> = ({
  tasks,
  totalTasksCount,
  title,
  schedulerName
}) => {
  const isFiltered = totalTasksCount !== undefined && totalTasksCount !== tasks.length;

  return (
    <div className="card" style={{ marginTop: '24px' }}>
      <div className="card-title" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <span style={{ fontSize: '1.15rem', fontWeight: 700, color: '#f8fafc' }}>
            {title || 'Detailed Task Results'}
          </span>
          {isFiltered && (
            <span style={{ fontSize: '0.8rem', color: '#38bdf8', marginLeft: '10px' }}>
              (Showing {tasks.length} of {totalTasksCount} tasks)
            </span>
          )}
        </div>
        {schedulerName && (
          <span className="badge badge-info" style={{ textTransform: 'uppercase' }}>
            {schedulerName === 'PROPOSED' ? 'Deadline-Aware' : schedulerName === 'BASELINE' ? 'Standard Priority' : schedulerName}
          </span>
        )}
      </div>
      <p className="card-subtitle" style={{ margin: '4px 0 14px' }}>
        See when each task started, which VM executed it, and when it finished.
      </p>

      {tasks.length === 0 ? (
        <div style={{ padding: '24px', textAlign: 'center', color: '#94a3b8' }}>
          No tasks match the selected filter criteria.
        </div>
      ) : (
        <div className="table-responsive" style={{ maxHeight: '350px', overflowY: 'auto' }}>
          <table>
            <thead>
              <tr style={{ position: 'sticky', top: 0, zIndex: 1 }}>
                <th>Task ID</th>
                <th>Priority</th>
                <th>Arrival (s)</th>
                <th>Execution (s)</th>
                <th>Deadline (s)</th>
                <th>Allocated VM</th>
                <th>Start Time (s)</th>
                <th>Completion (s)</th>
                <th>Waiting Time (s)</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {tasks.map((t) => (
                <tr key={t.taskId}>
                  <td style={{ fontWeight: 600, fontFamily: 'var(--font-mono)' }}>T{t.taskId}</td>
                  <td>{t.priority}</td>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>{t.arrivalTime.toFixed(2)}</td>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>{t.executionTime.toFixed(2)}</td>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>{t.deadline.toFixed(2)}</td>
                  <td>
                    <span className="badge badge-info" style={{ padding: '2px 8px' }}>
                      VM {t.assignedVmId}
                    </span>
                  </td>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>{t.startTime.toFixed(2)}</td>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>{t.completionTime.toFixed(2)}</td>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>{t.waitingTime.toFixed(2)}</td>
                  <td>
                    <span className={`badge ${t.deadlineMissed ? 'badge-offline' : 'badge-online'}`}>
                      {t.status}
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
