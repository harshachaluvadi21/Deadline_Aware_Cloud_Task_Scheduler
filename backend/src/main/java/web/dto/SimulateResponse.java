package web.dto;

import java.util.List;

/**
 * Response payload for /api/simulate.
 * Includes the executionTarget (SIMULATION or EC2) for transparent telemetry attribution.
 */
public record SimulateResponse(
        String algorithm,
        List<TaskResultDto> tasks,
        MetricsDto metrics,
        ExecutionTarget executionTarget
) {
    public SimulateResponse(String algorithm, List<TaskResultDto> tasks, MetricsDto metrics) {
        this(algorithm, tasks, metrics, ExecutionTarget.SIMULATION);
    }
}
