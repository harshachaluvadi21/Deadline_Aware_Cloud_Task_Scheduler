package web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * Request payload for /api/simulate.
 * Supports optional executionTarget defaulting to SIMULATION.
 */
public record SimulateRequest(
        @NotBlank(message = "Algorithm cannot be blank")
        @Pattern(regexp = "(?i)^(BASELINE|PROPOSED)$", message = "Algorithm must be either 'BASELINE' or 'PROPOSED'")
        String algorithm,

        @NotEmpty(message = "Task list cannot be empty")
        List<@Valid TaskDto> tasks,

        ExecutionTarget executionTarget
) {
    public SimulateRequest(String algorithm, List<TaskDto> tasks) {
        this(algorithm, tasks, ExecutionTarget.SIMULATION);
    }

    public ExecutionTarget target() {
        return executionTarget != null ? executionTarget : ExecutionTarget.SIMULATION;
    }

    public void validate() {
        if (tasks != null) {
            tasks.forEach(TaskDto::validate);
        }
    }
}
