package web.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Execution target environment for task processing.
 *
 * <ul>
 *   <li><b>SIMULATION (Default):</b> Executes tasks in-memory using the deterministic
 *       discrete-event simulation model (CloudSim Plus compatible).</li>
 *   <li><b>REAL_WORKER:</b> Dispatches scheduled tasks to an active real CPU worker node
 *       for genuine CPU computation and wall-clock telemetry capture.</li>
 * </ul>
 */
public enum ExecutionTarget {
    SIMULATION,
    REAL_WORKER;

    @JsonCreator
    public static ExecutionTarget fromString(String val) {
        if (val == null || val.isBlank()) {
            return SIMULATION;
        }
        String upper = val.trim().toUpperCase();
        // Backward-compatibility alias: accept "EC2" and "WORKER" and map them to REAL_WORKER
        if ("EC2".equals(upper) || "REAL_WORKER".equals(upper) || "WORKER".equals(upper)) {
            return REAL_WORKER;
        }
        if ("SIMULATION".equals(upper) || "SIMULATED".equals(upper)) {
            return SIMULATION;
        }
        throw new IllegalArgumentException("Invalid executionTarget: '" + val + "'. Expected 'SIMULATION' or 'REAL_WORKER'.");
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
