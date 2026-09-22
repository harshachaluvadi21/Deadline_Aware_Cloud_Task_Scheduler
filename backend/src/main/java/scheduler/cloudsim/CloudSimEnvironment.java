package scheduler.cloudsim;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;
import scheduler.model.CloudVmSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates the core CloudSim Plus 8.0.0 simulation infrastructure.
 *
 * Provides a managed, deterministic lifecycle for:
 * <pre>
 *   CloudSimPlus Simulation
 *           ↓
 *      Datacenter
 *           ↓
 *         Hosts
 *           ↓
 *   Processing Elements (PEs)
 *           ↓
 *    Virtual Machines (VMs)
 * </pre>
 *
 * Centralizes all hardware resource provisioning and isolates infrastructure setup
 * from higher-level scheduling algorithms.
 */
public class CloudSimEnvironment {

    private final SimulationScenario scenario;
    private final CloudSimPlus simulation;

    private Datacenter datacenter;
    private final List<Host> hostList = new ArrayList<>();
    private DatacenterBroker broker;
    private final List<Vm> vmList = new ArrayList<>();

    private boolean initialized = false;

    /**
     * Creates an environment configured with the default project simulation scenario.
     */
    public CloudSimEnvironment() {
        this(SimulationScenario.defaultScenario());
    }

    /**
     * Creates an environment configured with a specified simulation scenario.
     *
     * @param scenario the hardware and VM provisioning scenario to apply
     */
    public CloudSimEnvironment(SimulationScenario scenario) {
        this.scenario = Objects.requireNonNull(scenario, "Scenario must not be null");
        this.simulation = new CloudSimPlus();
    }

    /**
     * Initializes the simulation infrastructure:
     * 1. Constructs Hosts with configured PEs (MIPS, RAM, BW, Storage).
     * 2. Creates the Datacenter.
     * 3. Creates the DatacenterBroker.
     * 4. Provisions Virtual Machines from {@link CloudVmSpec} definitions.
     * 5. Submits provisioned VMs to the Broker.
     *
     * @return this initialized environment instance
     * @throws IllegalStateException if the environment has already been initialized
     */
    public CloudSimEnvironment initialize() {
        if (initialized) {
            throw new IllegalStateException("CloudSimEnvironment has already been initialized");
        }

        // 1. Build Hosts with PEs
        for (int i = 0; i < scenario.getHostCount(); i++) {
            List<Pe> peList = new ArrayList<>();
            for (int p = 0; p < scenario.getPesPerHost(); p++) {
                peList.add(new PeSimple(scenario.getPeMips()));
            }

            Host host = new HostSimple(
                scenario.getHostRam(),
                scenario.getHostBw(),
                scenario.getHostStorage(),
                peList
            );
            hostList.add(host);
        }

        // 2. Build Datacenter
        this.datacenter = new DatacenterSimple(this.simulation, this.hostList);
        this.datacenter.setSchedulingInterval(scenario.getSchedulingInterval());

        // 3. Build Broker
        this.broker = new DatacenterBrokerSimple(this.simulation);

        // 4. Provision VMs from specifications
        for (CloudVmSpec spec : scenario.getVmSpecs()) {
            Vm vm = new VmSimple(spec.id(), spec.mips(), spec.pesNumber());
            vm.setRam(spec.ram())
              .setBw(spec.bw())
              .setSize(spec.storage());
            vmList.add(vm);
        }

        // 5. Submit VMs to Broker for allocation
        this.broker.submitVmList(this.vmList);

        this.initialized = true;
        return this;
    }

    /**
     * Starts the CloudSim Plus simulation execution.
     * If the environment has not been initialized yet, {@link #initialize()} is automatically invoked.
     *
     * @return the simulation clock time at completion
     */
    public double start() {
        if (!initialized) {
            initialize();
        }
        return simulation.start();
    }

    /**
     * Checks whether the environment has been successfully initialized.
     *
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Checks if the underlying discrete-event simulation is currently running.
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return simulation.isRunning();
    }

    /**
     * Returns the current simulation clock time.
     *
     * @return current clock in simulation seconds
     */
    public double clock() {
        return simulation.clock();
    }

    // Accessors for infrastructure components
    public SimulationScenario getScenario() {
        return scenario;
    }

    public CloudSimPlus getSimulation() {
        return simulation;
    }

    public Datacenter getDatacenter() {
        return datacenter;
    }

    public List<Host> getHosts() {
        return Collections.unmodifiableList(hostList);
    }

    public DatacenterBroker getBroker() {
        return broker;
    }

    public List<Vm> getVms() {
        return Collections.unmodifiableList(vmList);
    }
}
