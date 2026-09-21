# Simulation Environment & Resource Provisioning Specification

## 1. Overview & Architectural Scope
Phase 3 establishes the centralized, deterministic infrastructure layer for the project using **CloudSim Plus 8.0.0** on **JDK 21**.

The infrastructure models a hierarchical cloud datacenter:
```
CloudSimPlus Simulation Engine
        │
        ▼
   Datacenter (DatacenterSimple)
        │
        ▼
Physical Hosts (HostSimple)
        │
        ▼
Processing Elements / Cores (PeSimple)
        │
        ▼
Virtual Machines (VmSimple provisioned via CloudVmSpec)
```

In accordance with strict modularity rules, this layer provides pure hardware provisioning and lifecycle management. It contains **zero** scheduling heuristics, priority logic, or task models.

---

## 2. Resource Configuration Details

### 2.1 Distinction: Project Simulation Configuration vs. IEEE Paper
> [!IMPORTANT]
> **Provenance Distinction:**
> The underlying paper (*Lipsa, Dash, Ivkovic, Cengiz, IEEE Access 2023*) models an abstract queuing system ($M/M/n$) parameterized by mathematical Poisson arrival rate $\lambda$ and exponential service rate $\mu$. The paper does **not** specify physical datacenter hardware parameters (such as Host RAM, network bandwidth, or storage).
> 
> Therefore, the values documented below are **Project Simulation Environment Configurations**, selected to provide realistic heterogeneous cloud execution, non-oversubscribed stable host capacity, and fully deterministic simulation reproducibility.

---

### 2.2 Datacenter Configuration
* **Implementation Class:** `org.cloudsimplus.datacenters.DatacenterSimple`
* **Host Count:** 2 physical hosts
* **Scheduling Interval:** `0.1` seconds (ensures high temporal resolution for event processing and latency calculation)
* **VM Allocation Policy:** `VmAllocationPolicySimple` (first-fit allocation of VMs onto available physical hosts)
* **Datacenter Storage:** Local host storage management

---

### 2.3 Physical Host Configuration
Each of the 2 physical hosts is provisioned with identical server-grade capacity:

| Resource | Value per Host | Total Datacenter Capacity (2 Hosts) | Rationale |
|---|---|---|---|
| **Cores (PEs)** | 4 PEs | 8 PEs | Enables concurrent VM execution without physical core oversubscription. |
| **PE Capacity (MIPS)** | 3,000 MIPS | 24,000 MIPS | High-capacity baseline core speed allowing granular virtualization. |
| **RAM** | 32,768 MB (32 GB) | 65,536 MB (64 GB) | Ample memory to host multiple concurrent VMs without swapping or allocation failure. |
| **Bandwidth (BW)** | 10,000 Mbps (10 Gbps) | 20,000 Mbps | Standard 10GbE enterprise datacenter interconnect. |
| **Storage** | 1,000,000 MB (1 TB) | 2,000,000 MB (2 TB) | Standard SSD capacity for VM images and cloudlet IO. |

---

### 2.4 Virtual Machine (VM) Configuration
To model real-world cloud service tiers and heterogeneous queuing ($M/M/n$ with varying service rates $\mu_j$), the default scenario provisions **5 heterogeneous VMs**:

| VM ID | Processing Speed (MIPS) | Cores (PEs) | RAM (MB) | Bandwidth (Mbps) | Storage (MB) | Service Profile / Tier |
|---|---|---|---|---|---|---|
| **VM 0** | 500 MIPS | 1 | 1,024 MB | 1,000 Mbps | 10,000 MB | Economy / Low-Tier |
| **VM 1** | 1,000 MIPS | 1 | 2,048 MB | 1,000 Mbps | 10,000 MB | Standard / General Purpose |
| **VM 2** | 1,500 MIPS | 1 | 2,048 MB | 1,000 Mbps | 10,000 MB | Balanced Computing |
| **VM 3** | 2,000 MIPS | 1 | 4,096 MB | 1,000 Mbps | 10,000 MB | High Performance |
| **VM 4** | 2,500 MIPS | 1 | 4,096 MB | 1,000 Mbps | 10,000 MB | Compute Optimized / Premium |

#### Why These VM Values Were Selected:
1. **Heterogeneity for Queuing & PAT Evaluation:** A core premise of the IEEE Access 2023 PAT algorithm and our proposed deadline scheduler is that task execution time varies across VMs ($E_{ij} = \frac{\text{Task Length}_i}{\text{VM MIPS}_j}$). Homogeneous VMs would eliminate execution variance and collapse the Waiting Time Matrix (WTM). The 5-to-1 speed ratio ($500$ to $2500$ MIPS) creates realistic variance.
2. **Resource Feasibility (No Oversubscription):**
   - Total VM MIPS: $500 + 1000 + 1500 + 2000 + 2500 = 7,500$ MIPS.
   - Total Host Capacity: $2 \times 12,000 = 24,000$ MIPS.
   - Total VM RAM: $1024 + 2048 + 2048 + 4096 + 4096 = 13,312$ MB.
   - Total Host RAM: $65,536$ MB.
   - Total VM Cores: 5 PEs mapped across 8 physical Host PEs.
   All VMs are guaranteed 100% allocation without resource starvation or rejection.
3. **Execution Determinism:** Dedicated, single-core VMs eliminate intra-VM core contention in CloudSim Plus, ensuring reproducible experiment results across repeated simulation runs.

---

## 3. Implementation Classes

1. **`scheduler.model.CloudVmSpec`**:
   - Modern Java `record` defining immutable VM resource specifications.
   - Enforces strict input validation (all resource dimensions must be strictly positive).
2. **`scheduler.cloudsim.SimulationScenario`**:
   - Centralized scenario configuration holding host counts, hardware limits, and VM specifications.
   - Provides `SimulationScenario.defaultScenario()` for project-wide consistency and supports custom scenario construction for testing.
3. **`scheduler.cloudsim.CloudSimEnvironment`**:
   - Manages simulation lifecycle (`initialize()`, `start()`, `clock()`, `isRunning()`).
   - Instantiates physical hosts, PEs, Datacenter, Broker, and provisions VMs deterministically.
   - Prevents duplicate initialization through `IllegalStateException` guards.

---

## 4. Verification Results

Automated unit and integration tests are implemented in `src/test/java/scheduler/cloudsim/CloudSimEnvironmentTest.java`:
- **Default Environment Tests:** Verifies all 2 hosts, 8 PEs, 5 VMs, and broker bindings match specifications.
- **Lifecycle Execution Tests:** Verifies clean execution of `start()`, proper destruction of VMs at shutdown, and zero allocation failures in `broker.getVmFailedList()`.
- **Custom Scenario Tests:** Verifies adaptability to user-defined host/VM capacities.
- **Input Validation Tests:** Verifies rejection of invalid or non-positive hardware configurations.

All tests pass with `0` failures and `0` errors under `mvn clean test`.
