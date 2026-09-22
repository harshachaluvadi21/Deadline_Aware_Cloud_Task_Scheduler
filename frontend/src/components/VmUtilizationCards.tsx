import React from 'react';
import { TaskResultDto } from '../types/simulation';

interface VmUtilizationCardsProps {
  tasks: TaskResultDto[];
  makespan: number;
  overallClusterUtilization?: number;
}

interface VmStat {
  vmId: number;
  taskCount: number;
  busyTime: number;
  busyPercentage: number;
}

export const VmUtilizationCards: React.FC<VmUtilizationCardsProps> = ({
  tasks,
  makespan,
  overallClusterUtilization
}) => {
  if (tasks.length === 0 || makespan <= 0) return null;

  // Aggregate stats per VM
  const vmMap = new Map<number, { taskCount: number; busyTime: number }>();

  // Ensure VMs 0, 1, 2, 3 exist at minimum, or collect all unique VMs
  tasks.forEach(t => {
    const existing = vmMap.get(t.assignedVmId) || { taskCount: 0, busyTime: 0 };
    existing.taskCount += 1;
    existing.busyTime += t.executionTime;
    vmMap.set(t.assignedVmId, existing);
  });

  // Make sure at least VMs 0..3 are initialized if none assigned
  for (let i = 0; i < 4; i++) {
    if (!vmMap.has(i)) {
      vmMap.set(i, { taskCount: 0, busyTime: 0 });
    }
  }

  const vmStats: VmStat[] = Array.from(vmMap.entries())
    .sort(([a], [b]) => a - b)
    .map(([vmId, stat]) => {
      const busyPercentage = (stat.busyTime / makespan) * 100;
      return {
        vmId,
        taskCount: stat.taskCount,
        busyTime: stat.busyTime,
        busyPercentage: Math.min(100, Math.max(0, busyPercentage))
      };
    });

  const totalBusy = vmStats.reduce((sum, s) => sum + s.busyTime, 0);
  const calculatedClusterUtil = (totalBusy / (vmStats.length * makespan)) * 100;
  const clusterUtilDisplay = overallClusterUtilization !== undefined
    ? overallClusterUtilization.toFixed(1)
    : calculatedClusterUtil.toFixed(1);

  return (
    <div className="card" style={{ marginTop: '24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
        <div>
          <h3 style={{ margin: 0, fontSize: '1.15rem', color: '#f8fafc' }}>
            Virtual Machine Usage
          </h3>
          <p style={{ margin: '4px 0 0', fontSize: '0.84rem', color: '#94a3b8' }}>
            This shows how much time each virtual machine spent executing tasks.
          </p>
        </div>
        <div style={{
          background: 'rgba(56, 189, 248, 0.1)',
          border: '1px solid rgba(56, 189, 248, 0.25)',
          padding: '6px 14px',
          borderRadius: '8px',
          textAlign: 'right'
        }}>
          <div style={{ fontSize: '0.75rem', color: '#94a3b8', textTransform: 'uppercase' }}>
            Overall Cluster Usage
          </div>
          <div style={{ fontSize: '1.2rem', fontWeight: 700, color: '#38bdf8' }}>
            {clusterUtilDisplay}%
          </div>
        </div>
      </div>

      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
        gap: '16px',
        marginTop: '14px'
      }}>
        {vmStats.map((vm) => (
          <div
            key={vm.vmId}
            style={{
              background: '#0f172a',
              border: '1px solid #1e293b',
              borderRadius: '8px',
              padding: '14px 16px',
              display: 'flex',
              flexDirection: 'column',
              gap: '10px'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontWeight: 700, color: '#f8fafc', fontSize: '0.95rem' }}>
                🖥️ VM {vm.vmId}
              </span>
              <span style={{
                fontSize: '0.8rem',
                color: '#38bdf8',
                backgroundColor: 'rgba(56, 189, 248, 0.1)',
                padding: '2px 8px',
                borderRadius: '4px'
              }}>
                {vm.taskCount} {vm.taskCount === 1 ? 'task' : 'tasks'}
              </span>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', color: '#94a3b8' }}>
              <span>Busy Time:</span>
              <strong style={{ color: '#e2e8f0' }}>{vm.busyTime.toFixed(2)} s</strong>
            </div>

            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: '#94a3b8', marginBottom: '4px' }}>
                <span>Usage:</span>
                <strong style={{ color: '#38bdf8' }}>{vm.busyPercentage.toFixed(1)}%</strong>
              </div>
              <div style={{ height: '8px', backgroundColor: '#1e293b', borderRadius: '4px', overflow: 'hidden' }}>
                <div
                  style={{
                    height: '100%',
                    width: `${vm.busyPercentage}%`,
                    backgroundColor: '#38bdf8',
                    transition: 'width 0.4s ease'
                  }}
                />
              </div>
            </div>
          </div>
        ))}
      </div>

      <p style={{ margin: '14px 0 0', fontSize: '0.78rem', color: '#64748b' }}>
        💡 Higher usage means the VM spent more of the simulation executing tasks.
      </p>
    </div>
  );
};
