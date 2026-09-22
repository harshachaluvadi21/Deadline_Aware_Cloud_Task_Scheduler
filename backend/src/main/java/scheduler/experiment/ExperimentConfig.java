package scheduler.experiment;

import scheduler.model.CloudVmSpec;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable configuration defining all parameters for an experimental run.
 *
 * <p>Ensures that both the Baseline and Proposed schedulers are evaluated under
 * identical simulation parameters, VM topologies, and task workloads.
 */
public record ExperimentConfig(
        WorkloadScenarioType scenarioType,
        int taskCount,
        long seed,
        List<CloudVmSpec> vms,
        double referenceMips,
        Path outputDirectory
) {

    /** Default reference MIPS (1000.0). */
    public static final double DEFAULT_REFERENCE_MIPS = 1000.0;
    /** Default number of VMs (4). */
    public static final int DEFAULT_VM_COUNT = 4;
    /** Default output directory ("results"). */
    public static final Path DEFAULT_OUTPUT_DIR = Paths.get("results");

    /**
     * Compact constructor validating experimental parameters.
     */
    public ExperimentConfig {
        Objects.requireNonNull(scenarioType, "Scenario type cannot be null");
        if (taskCount <= 0) {
            throw new IllegalArgumentException("Task count must be positive: " + taskCount);
        }
        if (vms == null || vms.isEmpty()) {
            throw new IllegalArgumentException("VM specifications cannot be null or empty");
        }
        if (referenceMips <= 0.0) {
            throw new IllegalArgumentException("Reference MIPS must be positive: " + referenceMips);
        }
        Objects.requireNonNull(outputDirectory, "Output directory cannot be null");
        vms = Collections.unmodifiableList(new ArrayList<>(vms));
    }

    /**
     * Creates a configuration using default scenario seed and 4 homogeneous VMs.
     *
     * @param scenario  the workload scenario
     * @param taskCount the number of tasks
     * @return populated ExperimentConfig
     */
    public static ExperimentConfig of(WorkloadScenarioType scenario, int taskCount) {
        return of(scenario, taskCount, scenario.getDefaultSeed(), DEFAULT_VM_COUNT, DEFAULT_OUTPUT_DIR);
    }

    /**
     * Creates a configuration with a custom seed and default 4 homogeneous VMs.
     *
     * @param scenario  the workload scenario
     * @param taskCount the number of tasks
     * @param seed      pseudorandom seed
     * @return populated ExperimentConfig
     */
    public static ExperimentConfig of(WorkloadScenarioType scenario, int taskCount, long seed) {
        return of(scenario, taskCount, seed, DEFAULT_VM_COUNT, DEFAULT_OUTPUT_DIR);
    }

    /**
     * Creates a configuration with custom seed, VM count, and output directory.
     *
     * @param scenario   the workload scenario
     * @param taskCount  the number of tasks
     * @param seed       pseudorandom seed
     * @param vmCount    number of homogeneous VMs (1000 MIPS each)
     * @param outputDir  target output directory
     * @return populated ExperimentConfig
     */
    public static ExperimentConfig of(WorkloadScenarioType scenario, int taskCount, long seed, int vmCount, Path outputDir) {
        return new ExperimentConfig(
                scenario,
                taskCount,
                seed,
                createHomogeneousVms(vmCount, DEFAULT_REFERENCE_MIPS),
                DEFAULT_REFERENCE_MIPS,
                outputDir
        );
    }

    /**
     * Helper to create a list of homogeneous VM specifications.
     *
     * @param count VM count
     * @param mips  MIPS rating per VM
     * @return list of CloudVmSpec
     */
    public static List<CloudVmSpec> createHomogeneousVms(int count, double mips) {
        if (count <= 0) {
            throw new IllegalArgumentException("VM count must be positive: " + count);
        }
        List<CloudVmSpec> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(CloudVmSpec.of(i, mips, 2048, 1000, 10000));
        }
        return Collections.unmodifiableList(list);
    }
}
