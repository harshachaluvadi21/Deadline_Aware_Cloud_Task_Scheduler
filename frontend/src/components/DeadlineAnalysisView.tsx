import React from 'react';
import { TaskResultDto } from '../types/simulation';

interface DeadlineAnalysisViewProps {
  tasks: TaskResultDto[];
  title?: string;
  schedulerName?: string;
}

export function classifyTaskDeadline(
  completionTime: number,
  deadline: number,
  deadlineMissed?: boolean,
  status?: string
): {
  category: 'BEFORE_DEADLINE' | 'AT_DEADLINE' | 'MISSED_DEADLINE';
  label: string;
  badgeClass: string;
} {
  // If task status or flag explicitly marks deadline missed, respect as ground truth
  if (deadlineMissed === true || status === 'MISSED_DEADLINE') {
    return {
      category: 'MISSED_DEADLINE',
      label: 'Missed Deadline',
      badgeClass: 'badge-missed-deadline'
    };
  }

  // Using tolerance for floating point comparison
  const diff = completionTime - deadline;
  if (Math.abs(diff) < 0.0001) {
    return {
      category: 'AT_DEADLINE',
      label: 'Completed Exactly At Deadline',
      badgeClass: 'badge-at-deadline'
    };
  } else if (diff < 0) {
    return {
      category: 'BEFORE_DEADLINE',
      label: 'Completed Before Deadline',
      badgeClass: 'badge-before-deadline'
    };
  } else {
    return {
      category: 'MISSED_DEADLINE',
      label: 'Missed Deadline',
      badgeClass: 'badge-missed-deadline'
    };
  }
}

export function computeDeadlineStats(tasks: TaskResultDto[]) {
  let beforeCount = 0;
  let atCount = 0;
  let missedCount = 0;

  tasks.forEach((t) => {
    const classification = classifyTaskDeadline(t.completionTime, t.deadline, t.deadlineMissed, t.status);
    if (classification.category === 'BEFORE_DEADLINE') beforeCount++;
    else if (classification.category === 'AT_DEADLINE') atCount++;
    else missedCount++;
  });

  const total = tasks.length > 0 ? tasks.length : 1;
  const missedRate = (missedCount / total) * 100;
  const beforePct = (beforeCount / total) * 100;
  const atPct = (atCount / total) * 100;

  return {
    beforeCount,
    atCount,
    missedCount,
    total: tasks.length,
    missedRate,
    beforePct,
    atPct
  };
}


export const DeadlineAnalysisView: React.FC<DeadlineAnalysisViewProps> = ({
  tasks,
  title = 'Did Tasks Meet Their Deadlines?',
  schedulerName
}) => {
  if (tasks.length === 0) return null;

  const stats = computeDeadlineStats(tasks);
  const { beforeCount, atCount, missedCount } = stats;
  const beforePct = stats.beforePct.toFixed(1);
  const atPct = stats.atPct.toFixed(1);
  const missedPct = stats.missedRate.toFixed(1);

  return (
    <div className="card" style={{ marginTop: '24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
        <h3 style={{ margin: 0, fontSize: '1.15rem', color: '#f8fafc' }}>
          {title}
        </h3>
        {schedulerName && (
          <span style={{ fontSize: '0.82rem', color: '#94a3b8', background: '#0f172a', padding: '2px 8px', borderRadius: '4px' }}>
            {schedulerName === 'PROPOSED' ? 'Deadline-Aware' : schedulerName === 'BASELINE' ? 'Standard Priority' : schedulerName}
          </span>
        )}
      </div>

      <p style={{ margin: '0 0 16px', fontSize: '0.85rem', color: '#94a3b8' }}>
        Deadline status is based on each task’s completion time compared with its deadline.
      </p>

      {/* Breakdown Summary Bar */}
      <div style={{ marginBottom: '20px' }}>
        <div style={{
          display: 'flex',
          height: '24px',
          borderRadius: '6px',
          overflow: 'hidden',
          backgroundColor: '#1e293b',
          marginBottom: '8px'
        }}>
          {beforeCount > 0 && (
            <div
              style={{
                width: `${beforePct}%`,
                backgroundColor: '#10b981',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#ffffff',
                fontSize: '0.75rem',
                fontWeight: 600
              }}
              title={`Completed Before Deadline: ${beforeCount} tasks (${beforePct}%)`}
            >
              {beforePct}%
            </div>
          )}
          {atCount > 0 && (
            <div
              style={{
                width: `${atPct}%`,
                backgroundColor: '#38bdf8',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#ffffff',
                fontSize: '0.75rem',
                fontWeight: 600
              }}
              title={`Completed At Deadline: ${atCount} tasks (${atPct}%)`}
            >
              {atPct}%
            </div>
          )}
          {missedCount > 0 && (
            <div
              style={{
                width: `${missedPct}%`,
                backgroundColor: '#ef4444',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#ffffff',
                fontSize: '0.75rem',
                fontWeight: 600
              }}
              title={`Missed Deadline: ${missedCount} tasks (${missedPct}%)`}
            >
              {missedPct}%
            </div>
          )}
        </div>

        {/* Legend */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '20px', fontSize: '0.83rem', color: '#cbd5e1' }}>
          <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ width: '10px', height: '10px', backgroundColor: '#10b981', borderRadius: '2px' }} />
            Completed Before Deadline: <strong>{beforeCount}</strong> ({beforePct}%)
          </span>
          <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ width: '10px', height: '10px', backgroundColor: '#38bdf8', borderRadius: '2px' }} />
            Completed Exactly At Deadline: <strong>{atCount}</strong> ({atPct}%)
          </span>
          <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ width: '10px', height: '10px', backgroundColor: '#ef4444', borderRadius: '2px' }} />
            Missed Deadline: <strong>{missedCount}</strong> ({missedPct}%)
          </span>
        </div>
      </div>

      {/* Detail Table */}
      <div style={{ maxHeight: '280px', overflowY: 'auto', border: '1px solid #334155', borderRadius: '8px' }}>
        <table style={{ width: '100%', fontSize: '0.83rem', borderCollapse: 'collapse', textAlign: 'left' }}>
          <thead>
            <tr style={{ background: '#0f172a', borderBottom: '1px solid #334155', position: 'sticky', top: 0 }}>
              <th style={{ padding: '8px 12px' }}>Task ID</th>
              <th style={{ padding: '8px 12px' }}>Assigned VM</th>
              <th style={{ padding: '8px 12px' }}>Arrival (s)</th>
              <th style={{ padding: '8px 12px' }}>Deadline (s)</th>
              <th style={{ padding: '8px 12px' }}>Completion (s)</th>
              <th style={{ padding: '8px 12px' }}>Slack (Deadline − Completion)</th>
              <th style={{ padding: '8px 12px' }}>Status</th>
            </tr>
          </thead>
          <tbody>
            {tasks.map((t) => {
              const classification = classifyTaskDeadline(t.completionTime, t.deadline, t.deadlineMissed, t.status);
              const slack = t.deadline - t.completionTime;

              return (
                <tr key={t.taskId} style={{ borderBottom: '1px solid #1e293b' }}>
                  <td style={{ padding: '8px 12px', fontWeight: 600 }}>T{t.taskId}</td>
                  <td style={{ padding: '8px 12px' }}>VM {t.assignedVmId}</td>
                  <td style={{ padding: '8px 12px' }}>{t.arrivalTime.toFixed(2)}</td>
                  <td style={{ padding: '8px 12px' }}>{t.deadline.toFixed(2)}</td>
                  <td style={{ padding: '8px 12px', fontWeight: 600 }}>{t.completionTime.toFixed(2)}</td>
                  <td style={{
                    padding: '8px 12px',
                    fontWeight: 600,
                    color: slack > 0 ? '#10b981' : slack === 0 ? '#38bdf8' : '#ef4444'
                  }}>
                    {slack >= 0 ? `+${slack.toFixed(2)}` : slack.toFixed(2)} s
                  </td>
                  <td style={{ padding: '8px 12px' }}>
                    <span style={{
                      padding: '3px 8px',
                      borderRadius: '4px',
                      fontSize: '0.78rem',
                      fontWeight: 600,
                      backgroundColor:
                        classification.category === 'BEFORE_DEADLINE'
                          ? 'rgba(16, 185, 129, 0.15)'
                          : classification.category === 'AT_DEADLINE'
                          ? 'rgba(56, 189, 248, 0.15)'
                          : 'rgba(239, 68, 68, 0.15)',
                      color:
                        classification.category === 'BEFORE_DEADLINE'
                          ? '#34d399'
                          : classification.category === 'AT_DEADLINE'
                          ? '#38bdf8'
                          : '#f87171'
                    }}>
                      {classification.label}
                    </span>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};
