package scheduler.cloudsim;

import scheduler.model.CloudVmSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the centralized hardware and environment configuration for a CloudSim Plus simulation.
 * Holds Datacenter, Host, PE, and VM provisioning parameters.
 *
 * NOTE ON ORIGIN:
 * These resource specifications represent the PROJECT SIMULATION ENVIRONMENT configuration,
 * selected to model realistic heterogeneous cloud computing infrastructure with bounded, deterministic
 * execution. The underlying IEEE Access 2023 paper uses abstract queuing parameters (lambda, mu)
 * rather than concrete physical server/VM hardware configurations.
 */
public class SimulationScenario {

    // Datacenter / Host specifications (Project Simulation Configuration)
    private final int hostCount;
    private final int pesPerHost;
    private final double peMips;
    private final long hostRam;      // in MB
    private final long hostBw;       // in Mbps
    private final long hostStorage;  // in MB
    private final double schedulingInterval; // in seconds

    // VM specifications
    private final List<CloudVmSpec> vmSpecs;

    /**
     * Full constructor for custom simulation scenarios.
     */
    public SimulationScenario(
            int hostCount,
            int pesPerHost,
            double peMips,
            long hostRam,
            long hostBw,
            long hostStorage,
            double schedulingInterval,
            List<CloudVmSpec> vmSpecs) {

        if (hostCount <= 0) throw new IllegalArgumentException("Host count must be positive: " + hostCount);
        if (pesPerHost <= 0) throw new IllegalArgumentException("PEs per host must be positive: " + pesPerHost);
        if (peMips <= 0) throw new IllegalArgumentException("PE MIPS must be positive: " + peMips);
        if (hostRam <= 0) throw new IllegalArgumentException("Host RAM must be positive: " + hostRam);
        if (hostBw <= 0) throw new IllegalArgumentException("Host BW must be positive: " + hostBw);
        if (hostStorage <= 0) throw new IllegalArgumentException("Host Storage must be positive: " + hostStorage);
        if (vmSpecs == null || vmSpecs.isEmpty()) throw new IllegalArgumentException("VM specifications cannot be null or empty");

        this.hostCount = hostCount;
        this.pesPerHost = pesPerHost;
        this.peMips = peMips;
        this.hostRam = hostRam;
        this.hostBw = hostBw;
        this.hostStorage = hostStorage;
        this.schedulingInterval = schedulingInterval;
        this.vmSpecs = Collections.unmodifiableList(new ArrayList<>(vmSpecs));
    }

    /**
     * Generates the default project simulation scenario with:
     * - 2 physical hosts (each with 4 PEs @ 3000 MIPS, 32GB RAM, 10 Gbps BW, 1 TB storage)
     * - 5 heterogeneous VMs (500, 1000, 1500, 2000, 2500 MIPS)
     *
     * @return the standard baseline/proposed project simulation scenario
     */
    public static SimulationScenario defaultScenario() {
        List<CloudVmSpec> defaultVms = List.of(
            CloudVmSpec.of(0, 500,  1024, 1000, 10000),
            CloudVmSpec.of(1, 1000, 2048, 1000, 10000),
            CloudVmSpec.of(2, 1500, 2048, 1000, 10000),
            CloudVmSpec.of(3, 2000, 4096, 1000, 10000),
            CloudVmSpec.of(4, 2500, 4096, 1000, 10000)
        );

        return new SimulationScenario(
            2,            // 2 physical hosts
            4,            // 4 PEs per host
            3000.0,       // 3000 MIPS per PE (12,000 MIPS per host)
            32768,        // 32 GB RAM per host
            10000,        // 10 Gbps Bandwidth
            1000000,      // 1 TB Storage
            0.1,          // 0.1s scheduling update interval
            defaultVms
        );
    }

    // Getters
    public int getHostCount() { return hostCount; }
    public int getPesPerHost() { return pesPerHost; }
    public double getPeMips() { return peMips; }
    public long getHostRam() { return hostRam; }
    public long getHostBw() { return hostBw; }
    public long getHostStorage() { return hostStorage; }
    public double getSchedulingInterval() { return schedulingInterval; }
    public List<CloudVmSpec> getVmSpecs() { return vmSpecs; }
    public int getVmCount() { return vmSpecs.size(); }
}
