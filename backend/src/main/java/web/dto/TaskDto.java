package web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object representing an individual task specification input.
 */
public record TaskDto(
        @NotNull(message = "Task ID cannot be null")
        @Min(value = 0, message = "Task ID must be non-negative (>= 0)")
        Long taskId,

        @NotNull(message = "Priority cannot be null")
        @Min(value = 1, message = "Priority must be at least 1")
        @Max(value = 10, message = "Priority cannot exceed 10")
        Integer priority,

        @NotNull(message = "Arrival time cannot be null")
        @DecimalMin(value = "0.0", message = "Arrival time must be non-negative (>= 0)")
        Double arrivalTime,

        @NotNull(message = "Execution time cannot be null")
        @DecimalMin(value = "0.0001", inclusive = true, message = "Execution time must be strictly positive (> 0)")
        Double executionTime,

        @NotNull(message = "Deadline cannot be null")
        @DecimalMin(value = "0.0", message = "Deadline must be non-negative (>= 0)")
        Double deadline
) {
    public void validate() {
        if (deadline != null && arrivalTime != null && deadline < arrivalTime) {
            throw new IllegalArgumentException("Deadline (" + deadline + ") cannot be earlier than arrival time (" + arrivalTime + ") for Task " + taskId);
        }
    }
}
