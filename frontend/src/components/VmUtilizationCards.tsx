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
  tasks, makespan, overallClusterUtilization,
}) => {
  if (tasks.length === 0 || makespan <= 0) return null;

  const vmMap = new Map<number, { taskCount: number; busyTime: number }>();
  tasks.forEach(t => {
    const existing = vmMap.get(t.assignedVmId) || { taskCount: 0, busyTime: 0 };
    existing.taskCount += 1;
    existing.busyTime += t.executionTime;
    vmMap.set(t.assignedVmId, existing);
  });
  for (let i = 0; i < 4; i++) {
    if (!vmMap.has(i)) vmMap.set(i, { taskCount: 0, busyTime: 0 });
  }

  const vmStats: VmStat[] = Array.from(vmMap.entries())
    .sort(([a], [b]) => a - b)
    .map(([vmId, stat]) => ({
      vmId,
      taskCount: stat.taskCount,
      busyTime: stat.busyTime,
      busyPercentage: Math.min(100, Math.max(0, (stat.busyTime / makespan) * 100)),
    }));

  const totalBusy = vmStats.reduce((sum, s) => sum + s.busyTime, 0);
  const calculatedClusterUtil = (totalBusy / (vmStats.length * makespan)) * 100;
  const clusterUtilDisplay = overallClusterUtilization !== undefined
    ? overallClusterUtilization.toFixed(1)
    : calculatedClusterUtil.toFixed(1);

  return (
    <div className="card" style={{ marginBottom: 16 }}>
      <div className="card-title">
        <span>VM Utilization</span>
        <span className="badge badge-cyan">
          Cluster: {clusterUtilDisplay}%
        </span>
      </div>
      <p className="card-subtitle">Time each virtual machine spent executing tasks.</p>

      <div className="vm-util-grid">
        {vmStats.map(vm => (
          <div className="vm-util-card" key={vm.vmId}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
              <div className="vm-util-label">VM {vm.vmId}</div>
              <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>
                {vm.taskCount} {vm.taskCount === 1 ? 'task' : 'tasks'}
              </span>
            </div>
            <div className="vm-util-value">{vm.busyPercentage.toFixed(1)}%</div>
            <div className="vm-util-bar-wrap">
              <div className="vm-util-bar" style={{ width: `${vm.busyPercentage}%` }} />
            </div>
            <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>
              {vm.busyTime.toFixed(2)} s busy
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
