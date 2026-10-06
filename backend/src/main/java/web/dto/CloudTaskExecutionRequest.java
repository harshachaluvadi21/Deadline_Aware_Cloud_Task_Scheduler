package web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload sent to the real worker node's POST /execute endpoint.
 */
public record CloudTaskExecutionRequest(
        @NotBlank(message = "Task ID cannot be blank")
        String taskId,

        @NotNull(message = "Execution time cannot be null")
        @DecimalMin(value = "0.0001", inclusive = true, message = "Execution time must be strictly positive (> 0)")
        Double executionTime,

        Integer priority,

        Double deadline
) {
    public static CloudTaskExecutionRequest of(String taskId, double executionTime, int priority, double deadline) {
        return new CloudTaskExecutionRequest(taskId, executionTime, priority, deadline);
    }
}
