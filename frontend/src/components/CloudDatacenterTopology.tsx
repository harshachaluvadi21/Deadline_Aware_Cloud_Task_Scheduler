import React, { useState } from 'react';
import { TaskResultDto } from '../types/simulation';

interface CloudDatacenterTopologyProps {
  tasks?: TaskResultDto[];
  schedulerName?: string;
}

export const CloudDatacenterTopology: React.FC<CloudDatacenterTopologyProps> = ({ tasks, schedulerName }) => {
  const [isExpanded, setIsExpanded] = useState<boolean>(true);

  // Group tasks by assigned VM if available
  const vmTaskCounts: Record<number, number> = { 0: 0, 1: 0, 2: 0, 3: 0 };
  const vmBusyTimes: Record<number, number> = { 0: 0, 1: 0, 2: 0, 3: 0 };

  if (tasks && tasks.length > 0) {
    tasks.forEach(t => {
      const vmId = t.assignedVmId;
      if (vmTaskCounts[vmId] !== undefined) {
        vmTaskCounts[vmId] += 1;
        vmBusyTimes[vmId] += t.executionTime;
      }
    });
  }

  const hosts = [
    {
      id: 0,
      name: 'Physical Host 0',
      specs: '4 PEs @ 3,000 MIPS (12,000 Total MIPS) · 32 GB RAM · 10 Gbps SAN Interconnect',
      vms: [
        { id: 0, name: 'VM 0 (vm-us-east-1a)', mips: 1000, ram: '2 GB', pes: 1 },
        { id: 1, name: 'VM 1 (vm-us-east-1b)', mips: 1000, ram: '2 GB', pes: 1 },
      ],
    },
    {
      id: 1,
      name: 'Physical Host 1',
      specs: '4 PEs @ 3,000 MIPS (12,000 Total MIPS) · 32 GB RAM · 10 Gbps SAN Interconnect',
      vms: [
        { id: 2, name: 'VM 2 (vm-us-east-1c)', mips: 1000, ram: '2 GB', pes: 1 },
        { id: 3, name: 'VM 3 (vm-us-east-1d)', mips: 1000, ram: '2 GB', pes: 1 },
      ],
    },
  ];

  return (
    <div className="card" style={{ marginBottom: 20 }}>
      {/* Header */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          cursor: 'pointer',
          userSelect: 'none',
        }}
        onClick={() => setIsExpanded(!isExpanded)}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{
            width: 32,
            height: 32,
            borderRadius: 'var(--r-md)',
            background: 'rgba(56, 189, 248, 0.12)',
            border: '1px solid rgba(56, 189, 248, 0.3)',
            color: 'var(--cyan)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="2" y="2" width="20" height="8" rx="2" ry="2"/>
              <rect x="2" y="14" width="20" height="8" rx="2" ry="2"/>
              <line x1="6" y1="6" x2="6.01" y2="6"/>
              <line x1="6" y1="18" x2="6.01" y2="18"/>
            </svg>
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-bright)' }}>
                Cloud Datacenter Infrastructure & VM Topology
              </span>
              <span className="badge badge-cyan" style={{ fontSize: '0.68rem' }}>
                CloudSim Plus v8.0
              </span>
              {schedulerName && (
                <span className={`badge ${schedulerName === 'PROPOSED' ? 'badge-proposed' : 'badge-baseline'}`}>
                  {schedulerName === 'PROPOSED' ? 'Deadline-Aware Cluster' : 'Standard Cluster'}
                </span>
              )}
            </div>
            <p style={{ margin: 0, fontSize: '0.78rem', color: 'var(--text-muted)' }}>
              Physical datacenter nodes, CPU processing elements (PEs), and virtual machine allocations.
            </p>
          </div>
        </div>

        <button
          type="button"
          style={{
            background: 'none',
            border: 'none',
            color: 'var(--text-muted)',
            fontSize: '0.9rem',
            cursor: 'pointer',
            padding: 4,
          }}
          aria-label={isExpanded ? 'Collapse' : 'Expand'}
        >
          {isExpanded ? '▲' : '▼'}
        </button>
      </div>

      {/* Expanded Infrastructure Diagram */}
      {isExpanded && (
        <div style={{ marginTop: 18, display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* Datacenter Spec Bar */}
          <div style={{
            display: 'flex',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: 12,
            padding: '10px 14px',
            background: 'var(--bg-card-subtle, rgba(255, 255, 255, 0.02))',
            borderRadius: 'var(--r-md)',
            border: '1px solid var(--border)',
            fontSize: '0.75rem',
            color: 'var(--text-secondary)',
          }}>
            <span style={{ fontWeight: 600, color: 'var(--text-bright)' }}>
              Datacenter: <code>CloudSim-DC1</code>
            </span>
            <span>·</span>
            <span>Policy: <strong>VmAllocationPolicySimple</strong></span>
            <span>·</span>
            <span>Scheduler: <strong>CloudletSchedulerTimeShared</strong></span>
            <span>·</span>
            <span>Hypervisor: <strong>Xen CloudSim Engine</strong></span>
          </div>

          {/* Physical Hosts Grid */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(290px, 1fr))', gap: 14 }}>
            {hosts.map(host => (
              <div
                key={host.id}
                style={{
                  border: '1px solid var(--border)',
                  borderRadius: 'var(--r-md)',
                  background: 'rgba(255, 255, 255, 0.01)',
                  padding: '14px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 12,
                }}
              >
                {/* Host Title */}
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
                    <span style={{ fontWeight: 700, fontSize: '0.86rem', color: 'var(--text-bright)', display: 'flex', alignItems: 'center', gap: 6 }}>
                      <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#10b981' }} />
                      {host.name}
                    </span>
                    <span style={{ fontSize: '0.7rem', color: 'var(--cyan)', fontFamily: 'monospace' }}>
                      Host-ID #{host.id}
                    </span>
                  </div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', lineHeight: 1.4 }}>
                    {host.specs}
                  </div>
                </div>

                {/* VMs inside this host */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  <div style={{ fontSize: '0.7rem', fontWeight: 600, textTransform: 'uppercase', color: 'var(--text-muted)', letterSpacing: 0.5 }}>
                    Provisioned Virtual Machines:
                  </div>

                  {host.vms.map(vm => {
                    const taskCount = vmTaskCounts[vm.id] || 0;
                    const busySecs = vmBusyTimes[vm.id] || 0;
                    return (
                      <div
                        key={vm.id}
                        style={{
                          padding: '10px 12px',
                          borderRadius: 'var(--r-sm)',
                          background: 'var(--bg-glass, rgba(255, 255, 255, 0.03))',
                          border: '1px solid var(--border)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          gap: 10,
                        }}
                      >
                        <div>
                          <div style={{ fontWeight: 600, fontSize: '0.8rem', color: 'var(--text-primary)', marginBottom: 2 }}>
                            {vm.name}
                          </div>
                          <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
                            {vm.mips} MIPS · {vm.ram} RAM · {vm.pes} vCPU
                          </div>
                        </div>

                        {tasks && tasks.length > 0 && (
                          <div style={{ textAlign: 'right', flexShrink: 0 }}>
                            <div style={{ fontSize: '0.75rem', fontWeight: 700, color: taskCount > 0 ? 'var(--cyan)' : 'var(--text-muted)' }}>
                              {taskCount} {taskCount === 1 ? 'task' : 'tasks'}
                            </div>
                            <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>
                              {busySecs.toFixed(1)}s busy
                            </div>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
