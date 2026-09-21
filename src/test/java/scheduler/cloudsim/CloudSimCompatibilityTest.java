package scheduler.cloudsim;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 2 Verification Test:
 * Verifies that CloudSim Plus 8.0.0 core classes can be instantiated
 * and run on JDK 21 without dependency conflicts or bytecode issues.
 *
 * CloudSim Plus 8.0.0 API Architecture:
 * - Concrete simulation engine: org.cloudsimplus.core.CloudSimPlus
 * - Simulation interface: org.cloudsimplus.core.Simulation
 * - Brokers and Datacenters take CloudSimPlus as the simulation parameter
 */
public class CloudSimCompatibilityTest {

    @Test
    @DisplayName("Verify CloudSim Plus 8.0.0 core API instantiation and basic simulation lifecycle")
    void testCloudSimPlusCoreApiCompatibility() {
        // 1. Simulation instance (CloudSimPlus is the concrete engine in 8.0.0, implementing Simulation)
        CloudSimPlus simulation = new CloudSimPlus();
        assertNotNull(simulation, "CloudSimPlus simulation instance must not be null");
        assertInstanceOf(org.cloudsimplus.core.Simulation.class, simulation, "CloudSimPlus must implement Simulation interface");
        assertFalse(simulation.isRunning(), "Simulation should not be running prior to start");

        // 2. Processing Element (PE) and Host
        List<Pe> peList = new ArrayList<>();
        peList.add(new PeSimple(1000));
        Host host = new HostSimple(2048, 10000, 100000, peList);
        assertNotNull(host, "Host instance must not be null");

        List<Host> hostList = new ArrayList<>();
        hostList.add(host);

        // 3. Datacenter
        Datacenter datacenter = new DatacenterSimple(simulation, hostList);
        assertNotNull(datacenter, "Datacenter instance must not be null");
        assertEquals(1, datacenter.getHostList().size(), "Datacenter must contain exactly 1 host");

        // 4. Broker
        DatacenterBroker broker = new DatacenterBrokerSimple(simulation);
        assertNotNull(broker, "Broker instance must not be null");

        // 5. Virtual Machine (VM)
        Vm vm = new VmSimple(1000, 1);
        vm.setRam(1024).setBw(1000).setSize(10000);
        assertNotNull(vm, "VM instance must not be null");

        // 6. Cloudlet / Task
        Cloudlet cloudlet = new CloudletSimple(1000, 1);
        assertNotNull(cloudlet, "Cloudlet instance must not be null");

        // 7. Wire minimal simulation and verify clean execution
        broker.submitVm(vm);
        broker.submitCloudlet(cloudlet);

        simulation.start();

        // 8. Assertions on completed execution
        assertTrue(cloudlet.isFinished(), "Cloudlet should be finished after simulation run");
        assertEquals(1.0, cloudlet.getActualCpuTime(), 0.001, "A 1000 MI cloudlet on a 1000 MIPS VM consumes 1.0s of CPU time");
        assertEquals(1.1, cloudlet.getFinishTime(), 0.001, "Finish time is 1.1s including broker submission delay");
    }
}
