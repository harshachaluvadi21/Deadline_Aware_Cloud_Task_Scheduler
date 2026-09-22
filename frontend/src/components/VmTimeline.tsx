import React from 'react';
import { TaskResultDto } from '../types/simulation';
import { classifyTaskDeadline } from './DeadlineAnalysisView';

interface VmTimelineProps {
  tasks: TaskResultDto[];
  makespan: number;
  title?: string;
  schedulerName?: string;
}

export const VmTimeline: React.FC<VmTimelineProps> = ({
  tasks,
  makespan,
  title,
  schedulerName
}) => {
  // Determine distinct VM IDs (at least 4 VMs: 0, 1, 2, 3)
  const maxVmId = Math.max(3, ...tasks.map(t => t.assignedVmId));
  const vmIds = Array.from({ length: maxVmId + 1 }, (_, i) => i);
  const safeMakespan = makespan > 0 ? makespan : 1;

  // Generate 5 tick marks
  const ticks = [0, 0.25, 0.5, 0.75, 1.0].map(pct => ({
    percent: pct * 100,
    time: (safeMakespan * pct).toFixed(2)
  }));

  return (
    <div className="card" style={{ marginTop: '24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
        <h3 style={{ margin: 0, fontSize: '1.15rem', color: '#f8fafc' }}>
          {title || 'Task Execution Timeline'}
        </h3>
        {schedulerName && (
          <span style={{ fontSize: '0.82rem', color: '#94a3b8', background: '#0f172a', padding: '2px 8px', borderRadius: '4px' }}>
            {schedulerName === 'PROPOSED' ? 'Deadline-Aware' : schedulerName === 'BASELINE' ? 'Standard Priority' : schedulerName}
          </span>
        )}
      </div>

      <p style={{ margin: '0 0 16px', fontSize: '0.84rem', color: '#94a3b8' }}>
        Each bar shows when a task was running on a virtual machine.
      </p>

      {/* Gantt Container with horizontal scroll */}
      <div style={{ overflowX: 'auto', paddingBottom: '12px' }}>
        <div style={{ minWidth: '650px' }}>
          {/* VM Rows */}
          {vmIds.map((vmId) => {
            const vmTasks = tasks.filter(t => t.assignedVmId === vmId);

            return (
              <div
                key={vmId}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  marginBottom: '10px',
                  height: '42px'
                }}
              >
                {/* VM Label */}
                <div style={{
                  width: '75px',
                  fontSize: '0.88rem',
                  fontWeight: 700,
                  color: '#38bdf8',
                  flexShrink: 0
                }}>
                  VM {vmId}
                </div>

                {/* VM Track */}
                <div style={{
                  flex: 1,
                  height: '100%',
                  background: '#0f172a',
                  border: '1px solid #334155',
                  borderRadius: '6px',
                  position: 'relative',
                  overflow: 'hidden'
                }}>
                  {/* Grid Lines */}
                  {ticks.map((t, idx) => (
                    <div
                      key={idx}
                      style={{
                        position: 'absolute',
                        left: `${t.percent}%`,
                        top: 0,
                        bottom: 0,
                        width: '1px',
                        background: 'rgba(255, 255, 255, 0.05)',
                        pointerEvents: 'none'
                      }}
                    />
                  ))}

                  {/* Task Blocks */}
                  {vmTasks.map((t) => {
                    const leftPercent = Math.max(0, Math.min(100, (t.startTime / safeMakespan) * 100));
                    const widthPercent = Math.max(1.8, Math.min(100 - leftPercent, ((t.completionTime - t.startTime) / safeMakespan) * 100));
                    const classification = classifyTaskDeadline(t.completionTime, t.deadline);

                    let blockBg = '#10b981'; // Before deadline
                    let borderCol = '#059669';
                    if (classification.category === 'AT_DEADLINE') {
                      blockBg = '#06b6d4'; // At deadline
                      borderCol = '#0891b2';
                    } else if (classification.category === 'MISSED_DEADLINE') {
                      blockBg = '#ef4444'; // Missed
                      borderCol = '#dc2626';
                    }

                    return (
                      <div
                        key={t.taskId}
                        style={{
                          position: 'absolute',
                          left: `${leftPercent}%`,
                          width: `${widthPercent}%`,
                          top: '4px',
                          bottom: '4px',
                          backgroundColor: blockBg,
                          border: `1px solid ${borderCol}`,
                          borderRadius: '4px',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          color: '#ffffff',
                          fontSize: '0.75rem',
                          fontWeight: 700,
                          cursor: 'pointer',
                          boxShadow: '0 2px 4px rgba(0, 0, 0, 0.2)',
                          transition: 'transform 0.1s ease',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                          padding: '0 2px'
                        }}
                        title={`Task T${t.taskId} (Priority ${t.priority})\nStart: ${t.startTime.toFixed(2)}s | End: ${t.completionTime.toFixed(2)}s\nDeadline: ${t.deadline.toFixed(2)}s\nStatus: ${classification.label}`}
                      >
                        T{t.taskId}
                      </div>
                    );
                  })}
                </div>
              </div>
            );
          })}

          {/* Time Axis */}
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            marginLeft: '75px',
            marginTop: '8px',
            paddingTop: '6px',
            borderTop: '1px solid #334155',
            fontSize: '0.78rem',
            color: '#94a3b8',
            fontFamily: 'monospace'
          }}>
            {ticks.map((t, idx) => (
              <span key={idx} style={{ textAlign: idx === 0 ? 'left' : idx === ticks.length - 1 ? 'right' : 'center' }}>
                {t.time}s
              </span>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
