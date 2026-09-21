package scheduler.cloudsim;

import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.vms.Vm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import scheduler.model.CloudVmSpec;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 3 Unit and Integration Tests:
 * Validates initialization, resource hierarchy, and lifecycle execution of
 * {@link CloudSimEnvironment}, {@link SimulationScenario}, and {@link CloudVmSpec}.
 */
public class CloudSimEnvironmentTest {

    @Nested
    @DisplayName("Default Environment Tests")
    class DefaultEnvironmentTests {

        @Test
        @DisplayName("Verify default environment initialization and resource hierarchy")
        void testDefaultEnvironmentInitialization() {
            CloudSimEnvironment env = new CloudSimEnvironment();
            assertFalse(env.isInitialized(), "Environment should not be initialized upon construction");

            env.initialize();
            assertTrue(env.isInitialized(), "Environment should be marked as initialized");

            // 1. Verify Simulation Engine
            assertNotNull(env.getSimulation(), "Simulation engine instance must not be null");
            assertFalse(env.isRunning(), "Simulation should not be running prior to start()");

            // 2. Verify Datacenter
            assertNotNull(env.getDatacenter(), "Datacenter instance must not be null");

            // 3. Verify Hosts & PEs
            List<Host> hosts = env.getHosts();
            assertEquals(2, hosts.size(), "Default scenario must provision exactly 2 physical hosts");

            for (Host host : hosts) {
                assertEquals(32768, host.getRam().getCapacity(), "Host RAM should be 32,768 MB");
                assertEquals(10000, host.getBw().getCapacity(), "Host BW should be 10,000 Mbps");
                assertEquals(1000000, host.getStorage().getCapacity(), "Host storage should be 1,000,000 MB");

                List<Pe> pes = host.getPeList();
                assertEquals(4, pes.size(), "Each host must contain 4 PEs");
                for (Pe pe : pes) {
                    assertEquals(3000.0, pe.getCapacity(), 0.001, "Each PE must have 3,000 MIPS");
                }
            }

            // 4. Verify Broker
            assertNotNull(env.getBroker(), "Broker instance must not be null");

            // 5. Verify VMs
            List<Vm> vms = env.getVms();
            assertEquals(5, vms.size(), "Default scenario must provision 5 VMs");

            // Check heterogeneous MIPS values: 500, 1000, 1500, 2000, 2500
            double[] expectedMips = {500.0, 1000.0, 1500.0, 2000.0, 2500.0};
            for (int i = 0; i < vms.size(); i++) {
                Vm vm = vms.get(i);
                assertEquals(i, vm.getId(), "VM ID should match index");
                assertEquals(expectedMips[i], vm.getMips(), 0.001, "VM MIPS should match specification");
                assertEquals(1, vm.getPesNumber(), "Default VMs should have 1 PE");
            }
        }

        @Test
        @DisplayName("Verify that duplicate initialize() calls throw IllegalStateException")
        void testPreventDuplicateInitialization() {
            CloudSimEnvironment env = new CloudSimEnvironment();
            env.initialize();

            IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                env::initialize,
                "Subsequent initialize() calls must be prevented"
            );
            assertTrue(ex.getMessage().contains("already been initialized"));
        }

        @Test
        @DisplayName("Verify simulation lifecycle execution runs cleanly to completion")
        void testSimulationLifecycleExecution() {
            CloudSimEnvironment env = new CloudSimEnvironment();
            env.initialize();

            // Run simulation lifecycle
            double completionTime = env.start();

            assertTrue(completionTime >= 0.0, "Simulation completion time must be non-negative");
            assertFalse(env.isRunning(), "Simulation should no longer be running after completion");

            // Verify VMs were successfully allocated to hosts and none failed
            assertEquals(5, env.getBroker().getVmCreatedList().size(), "All 5 VMs must be successfully created on hosts");
            assertTrue(env.getBroker().getVmFailedList().isEmpty(), "No VMs should fail allocation");
        }
    }

    @Nested
    @DisplayName("Custom Scenario & Validation Tests")
    class CustomScenarioAndValidationTests {

        @Test
        @DisplayName("Verify custom scenario environment provisioning")
        void testCustomScenarioProvisioning() {
            List<CloudVmSpec> customVmSpecs = List.of(
                CloudVmSpec.of(0, 1200.0, 1024, 500, 5000),
                CloudVmSpec.of(1, 2400.0, 2048, 1000, 10000)
            );

            SimulationScenario customScenario = new SimulationScenario(
                1,         // 1 host
                2,         // 2 PEs per host
                4000.0,    // 4000 MIPS per PE
                16384,     // 16 GB RAM
                5000,      // 5 Gbps BW
                500000,    // 500 GB Storage
                0.5,       // 0.5s scheduling interval
                customVmSpecs
            );

            CloudSimEnvironment env = new CloudSimEnvironment(customScenario);
            env.initialize();

            assertEquals(1, env.getHosts().size());
            assertEquals(2, env.getHosts().get(0).getPeList().size());
            assertEquals(4000.0, env.getHosts().get(0).getPeList().get(0).getCapacity(), 0.001);
            assertEquals(2, env.getVms().size());

            double finishTime = env.start();
            assertTrue(finishTime >= 0.0);
        }

        @Test
        @DisplayName("Verify CloudVmSpec parameter validation")
        void testCloudVmSpecValidation() {
            assertThrows(IllegalArgumentException.class, () -> new CloudVmSpec(-1, 1000, 1, 1024, 1000, 10000));
            assertThrows(IllegalArgumentException.class, () -> new CloudVmSpec(0, 0, 1, 1024, 1000, 10000));
            assertThrows(IllegalArgumentException.class, () -> new CloudVmSpec(0, 1000, 0, 1024, 1000, 10000));
            assertThrows(IllegalArgumentException.class, () -> new CloudVmSpec(0, 1000, 1, 0, 1000, 10000));
            assertThrows(IllegalArgumentException.class, () -> new CloudVmSpec(0, 1000, 1, 1024, 0, 10000));
            assertThrows(IllegalArgumentException.class, () -> new CloudVmSpec(0, 1000, 1, 1024, 1000, 0));
        }

        @Test
        @DisplayName("Verify SimulationScenario parameter validation")
        void testSimulationScenarioValidation() {
            List<CloudVmSpec> vms = List.of(CloudVmSpec.of(0, 1000, 1024, 1000, 10000));

            assertThrows(IllegalArgumentException.class, () -> new SimulationScenario(0, 4, 3000, 32768, 10000, 1000000, 0.1, vms));
            assertThrows(IllegalArgumentException.class, () -> new SimulationScenario(2, 0, 3000, 32768, 10000, 1000000, 0.1, vms));
            assertThrows(IllegalArgumentException.class, () -> new SimulationScenario(2, 4, 0, 32768, 10000, 1000000, 0.1, vms));
            assertThrows(IllegalArgumentException.class, () -> new SimulationScenario(2, 4, 3000, 0, 10000, 1000000, 0.1, vms));
            assertThrows(IllegalArgumentException.class, () -> new SimulationScenario(2, 4, 3000, 32768, 0, 1000000, 0.1, vms));
            assertThrows(IllegalArgumentException.class, () -> new SimulationScenario(2, 4, 3000, 32768, 10000, 0, 0.1, vms));
            assertThrows(IllegalArgumentException.class, () -> new SimulationScenario(2, 4, 3000, 32768, 10000, 1000000, 0.1, null));
            assertThrows(IllegalArgumentException.class, () -> new SimulationScenario(2, 4, 3000, 32768, 10000, 1000000, 0.1, List.of()));
        }
    }
}
