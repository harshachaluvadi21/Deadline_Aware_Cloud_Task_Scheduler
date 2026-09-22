package scheduler.baseline;

import scheduler.model.CloudVmSpec;

import java.util.List;

/**
 * Architectural alias for {@link BaselinePriorityScheduler}, mapping directly to the class name
 * defined in the project architecture and development plan.
 *
 * <p>Traceability: [Clearly implied by paper / Architectural naming]
 * Extends {@link BaselinePriorityScheduler} to ensure 100% backward and architectural compatibility.
 */
public class BaselineScheduler extends BaselinePriorityScheduler {

    /**
     * Constructs BaselineScheduler with the specified VM specifications.
     *
     * @param vms list of VM specifications
     */
    public BaselineScheduler(List<CloudVmSpec> vms) {
        super(vms);
    }

    /**
     * Constructs BaselineScheduler with the specified VM specifications and reference MIPS.
     *
     * @param vms           list of VM specifications
     * @param referenceMips reference MIPS
     */
    public BaselineScheduler(List<CloudVmSpec> vms, double referenceMips) {
        super(vms, referenceMips);
    }

    /**
     * Constructs BaselineScheduler with a fixed count of homogeneous VMs.
     *
     * @param vmCount number of homogeneous VMs
     */
    public BaselineScheduler(int vmCount) {
        super(vmCount);
    }
}
