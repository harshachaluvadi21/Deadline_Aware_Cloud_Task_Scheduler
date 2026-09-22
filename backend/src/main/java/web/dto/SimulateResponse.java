package web.dto;

import java.util.List;

/**
 * Response payload for /api/simulate.
 */
public record SimulateResponse(
        String algorithm,
        List<TaskResultDto> tasks,
        MetricsDto metrics
) {}
