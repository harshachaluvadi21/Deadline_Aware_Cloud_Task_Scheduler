package scheduler.model;

/**
 * Immutable specification for Virtual Machine (VM) resource allocation.
 * Encapsulates computing capacity (MIPS), processing element count (PEs),
 * memory (RAM in MB), network bandwidth (Mbps), and disk storage (MB).
 *
 * This specification is part of the project simulation configuration
 * and is used to instantiate concrete CloudSim Plus {@link org.cloudsimplus.vms.Vm} instances.
 */
public record CloudVmSpec(
    long id,
    double mips,
    long pesNumber,
    long ram,
    long bw,
    long storage
) {
    /**
     * Compact constructor validating positive hardware resource constraints.
     */
    public CloudVmSpec {
        if (id < 0) {
            throw new IllegalArgumentException("VM ID must be non-negative, got: " + id);
        }
        if (mips <= 0) {
            throw new IllegalArgumentException("MIPS must be positive, got: " + mips);
        }
        if (pesNumber <= 0) {
            throw new IllegalArgumentException("PEs count must be positive, got: " + pesNumber);
        }
        if (ram <= 0) {
            throw new IllegalArgumentException("RAM must be positive, got: " + ram);
        }
        if (bw <= 0) {
            throw new IllegalArgumentException("Bandwidth must be positive, got: " + bw);
        }
        if (storage <= 0) {
            throw new IllegalArgumentException("Storage must be positive, got: " + storage);
        }
    }

    /**
     * Convenience factory method for standard single-core VM specifications.
     *
     * @param id VM identifier
     * @param mips Processing capacity in MIPS
     * @param ram RAM in MB
     * @param bw Bandwidth in Mbps
     * @param storage Storage capacity in MB
     * @return a new CloudVmSpec with 1 PE
     */
    public static CloudVmSpec of(long id, double mips, long ram, long bw, long storage) {
        return new CloudVmSpec(id, mips, 1, ram, bw, storage);
    }
}
