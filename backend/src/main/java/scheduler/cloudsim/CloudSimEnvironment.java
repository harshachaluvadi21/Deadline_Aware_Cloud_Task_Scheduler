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
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import scheduler.baseline.VmAssignment;
import scheduler.experiment.TaskExecutionRecord;
import scheduler.model.CloudVmSpec;
import scheduler.model.Task;

import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * Immutable outcome of a CloudSim Plus simulation execution run.
     */
    public record CloudSimExecutionResult(
            double simulationClock,
            List<Task> tasks,
            List<TaskExecutionRecord> records
    ) {}

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
     * Creates an environment configured for a specified list of VM specifications.
     *
     * @param vmSpecs the VM specifications to provision
     */
    public CloudSimEnvironment(List<CloudVmSpec> vmSpecs) {
        this(SimulationScenario.forVmSpecs(vmSpecs));
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
     * 4. Provisions Virtual Machines from {@link CloudVmSpec} definitions using {@link CloudletSchedulerSpaceShared}.
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
        if (scenario.getSchedulingInterval() > 0.0) {
            this.datacenter.setSchedulingInterval(scenario.getSchedulingInterval());
        }

        // 3. Build Broker
        this.broker = new DatacenterBrokerSimple(this.simulation);

        // 4. Provision VMs from specifications with non-preemptive space-shared scheduling
        for (CloudVmSpec spec : scenario.getVmSpecs()) {
            Vm vm = new VmSimple(spec.id(), spec.mips(), spec.pesNumber());
            vm.setRam(spec.ram())
              .setBw(spec.bw())
              .setSize(spec.storage());
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
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
     * Executes the scheduling plan using CloudSim Plus 8.0.0:
     * 1. Translates domain {@link Task} definitions into CloudSim {@link CloudletSimple} instances via {@link TaskCloudletAdapter}.
     * 2. Binds each Cloudlet to its scheduled VM according to the scheduler's {@link VmAssignment} plan.
     * 3. Submits Cloudlets to the broker in the algorithm's dispatch sequence.
     * 4. Executes {@code simulation.start()} to run the discrete-event datacenter simulation.
     * 5. Harvests actual CloudSim Plus execution timestamps (start, finish, VM ID, actual CPU time).
     * 6. Updates domain task lifecycle and packages populated {@link TaskExecutionRecord} records.
     *
     * @param tasks         domain tasks to execute
     * @param assignments   scheduled VM assignments produced by the priority algorithm
     * @param referenceMips reference MIPS for Cloudlet length scaling
     * @return CloudSimExecutionResult containing actual simulation results
     */
    public CloudSimExecutionResult execute(List<Task> tasks, List<VmAssignment> assignments, double referenceMips) {
        Objects.requireNonNull(tasks, "Tasks cannot be null");
        Objects.requireNonNull(assignments, "Assignments cannot be null");

        if (!initialized) {
            initialize();
        }

        Map<Long, Vm> vmMap = vmList.stream().collect(Collectors.toMap(Vm::getId, v -> v));
        Map<Long, Task> taskMap = tasks.stream().collect(Collectors.toMap(Task::getTaskId, t -> t));
        Map<Long, VmAssignment> assignmentMap = assignments.stream().collect(Collectors.toMap(VmAssignment::taskId, a -> a));

        List<CloudletSimple> cloudletsToSubmit = new ArrayList<>(tasks.size());

        // Process in the algorithm's scheduled dispatch sequence
        for (VmAssignment assignment : assignments) {
            Task task = taskMap.get(assignment.taskId());
            if (task != null) {
                CloudletSimple cloudlet = TaskCloudletAdapter.toCloudlet(task, referenceMips);
                Vm targetVm = vmMap.get(assignment.vmId());
                if (targetVm == null) {
                    throw new IllegalStateException("Assigned VM ID " + assignment.vmId() + " not found in provisioned CloudSim VMs");
                }
                broker.bindCloudletToVm(cloudlet, targetVm);
                cloudletsToSubmit.add(cloudlet);
            }
        }

        // Also handle any remaining tasks not present in assignments (if any)
        for (Task task : tasks) {
            if (!assignmentMap.containsKey(task.getTaskId())) {
                CloudletSimple cloudlet = TaskCloudletAdapter.toCloudlet(task, referenceMips);
                cloudletsToSubmit.add(cloudlet);
            }
        }

        broker.submitCloudletList(cloudletsToSubmit);

        // Execute discrete-event simulation
        double finishClock = simulation.start();

        // Harvest actual CloudSim Plus execution results
        Map<Long, Cloudlet> finishedMap = broker.getCloudletFinishedList().stream()
                .collect(Collectors.toMap(Cloudlet::getId, c -> c));

        List<TaskExecutionRecord> records = new ArrayList<>(tasks.size());
        for (Task task : tasks) {
            Cloudlet cloudlet = finishedMap.get(task.getTaskId());
            if (cloudlet == null) {
                throw new IllegalStateException("Cloudlet for task ID " + task.getTaskId() + " did not finish in CloudSim Plus");
            }
            TaskCloudletAdapter.updateTaskFromCloudlet(task, cloudlet);
            records.add(TaskCloudletAdapter.toTaskExecutionRecord(task, cloudlet, task.getDynamicPriority()));
        }

        return new CloudSimExecutionResult(finishClock, tasks, records);
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
