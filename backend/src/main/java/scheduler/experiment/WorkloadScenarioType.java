package scheduler.experiment;

/**
 * Standard workload scenarios required by the controlled experimental framework.
 *
 * <p>Each scenario is characterized by deterministic initial seeds and neutral,
 * realistic task arrival, execution duration, and deadline slack parameter ranges.
 */
public enum WorkloadScenarioType {

    /**
     * Represents ordinary cloud workload conditions with moderate arrivals,
     * moderate durations, and comfortable deadline margins.
     */
    NORMAL_LOAD(1001L, 0.5, 2.5, 5.0, 20.0, 1.5, 3.5),

    /**
     * Stresses scheduling queues and VM capacity with frequent arrivals,
     * longer execution durations, and moderate deadline pressure.
     */
    HIGH_LOAD(2001L, 0.1, 0.8, 15.0, 40.0, 0.8, 2.0),

    /**
     * Emphasizes deadline urgency with tight scheduling slack buffers,
     * varied durations, and mixed priorities.
     */
    DEADLINE_SENSITIVE(3001L, 0.4, 2.0, 5.0, 30.0, 0.2, 1.2),

    /**
     * Combines relaxed, moderate, and urgent tasks with short and long execution durations.
     */
    MIXED(4001L, 0.2, 3.0, 3.0, 45.0, 0.2, 4.0);

    private final long defaultSeed;
    private final double minArrivalInterval;
    private final double maxArrivalInterval;
    private final double minExecutionTime;
    private final double maxExecutionTime;
    private final double minSlackMultiplier;
    private final double maxSlackMultiplier;

    WorkloadScenarioType(
            long defaultSeed,
            double minArrivalInterval, double maxArrivalInterval,
            double minExecutionTime, double maxExecutionTime,
            double minSlackMultiplier, double maxSlackMultiplier) {
        this.defaultSeed = defaultSeed;
        this.minArrivalInterval = minArrivalInterval;
        this.maxArrivalInterval = maxArrivalInterval;
        this.minExecutionTime = minExecutionTime;
        this.maxExecutionTime = maxExecutionTime;
        this.minSlackMultiplier = minSlackMultiplier;
        this.maxSlackMultiplier = maxSlackMultiplier;
    }

    public long getDefaultSeed() {
        return defaultSeed;
    }

    public double getMinArrivalInterval() {
        return minArrivalInterval;
    }

    public double getMaxArrivalInterval() {
        return maxArrivalInterval;
    }

    public double getMinExecutionTime() {
        return minExecutionTime;
    }

    public double getMaxExecutionTime() {
        return maxExecutionTime;
    }

    public double getMinSlackMultiplier() {
        return minSlackMultiplier;
    }

    public double getMaxSlackMultiplier() {
        return maxSlackMultiplier;
    }
}
