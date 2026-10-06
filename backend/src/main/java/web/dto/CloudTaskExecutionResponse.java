package web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Execution telemetry returned by the real worker node's POST /execute endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CloudTaskExecutionResponse(
        String taskId,
        String status,
        Double actualExecutionTimeSeconds,
        Double cpuUtilization,
        Double startTime,
        Double endTime
) {}
