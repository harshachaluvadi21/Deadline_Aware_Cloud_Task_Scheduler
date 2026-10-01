import React from 'react';
import { TaskDto, TaskResultDto } from '../types/simulation';

interface WorkloadSummaryCardsProps {
  tasks: TaskDto[];
  results?: TaskResultDto[];
}

export const WorkloadSummaryCards: React.FC<WorkloadSummaryCardsProps> = ({
  tasks,
  results
}) => {
  if (tasks.length === 0) return null;

  const totalTasks = tasks.length;
  const avgPriority = (tasks.reduce((sum, t) => sum + t.priority, 0) / totalTasks).toFixed(1);
  const avgExecution = (tasks.reduce((sum, t) => sum + t.executionTime, 0) / totalTasks).toFixed(2);
  const earliestArrival = Math.min(...tasks.map(t => t.arrivalTime)).toFixed(2);
  const latestDeadline = Math.max(...tasks.map(t => t.deadline)).toFixed(2);

  // Derive VM count from results if available, else derive from default 4
  let vmCount = 4;
  if (results && results.length > 0) {
    const uniqueVmIds = new Set(results.map(r => r.assignedVmId));
    vmCount = Math.max(uniqueVmIds.size, 4);
  }

  const cards = [
    { label: 'Total Tasks', value: totalTasks.toString(), description: 'Number of tasks' },
    { label: 'Virtual Machines', value: `${vmCount} VMs`, description: 'VMs available for execution' },
    { label: 'Average Priority', value: `${avgPriority} / 10`, description: 'Average task priority' },
    { label: 'Avg Execution Time', value: `${avgExecution} s`, description: 'Average execution duration' },
    { label: 'Earliest Arrival', value: `${earliestArrival} s`, description: 'First task arrival' },
    { label: 'Latest Deadline', value: `${latestDeadline} s`, description: 'Latest task deadline' },
  ];

  return (
    <div style={{ marginBottom: '24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
        <h3 style={{ fontSize: '1.05rem', color: '#94a3b8', margin: 0, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          Workload Summary
        </h3>
        <span style={{ fontSize: '0.78rem', color: '#64748b' }}>
          VM = Virtual Machine, a virtual computer used to execute tasks.
        </span>
      </div>

      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))',
        gap: '12px'
      }}>
        {cards.map((c, i) => (
          <div
            key={i}
            className="card"
            style={{
              padding: '12px 14px',
              borderLeft: '3px solid #38bdf8',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between'
            }}
          >
            <div style={{ marginBottom: '6px' }}>
              <span style={{ fontSize: '0.78rem', color: '#94a3b8', fontWeight: 500 }}>
                {c.label}
              </span>
            </div>
            <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#f8fafc', marginBottom: '2px' }}>
              {c.value}
            </div>
            <div style={{ fontSize: '0.72rem', color: '#64748b' }}>
              {c.description}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
