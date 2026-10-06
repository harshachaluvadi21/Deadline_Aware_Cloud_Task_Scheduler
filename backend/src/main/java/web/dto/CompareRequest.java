package web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request payload for /api/compare.
 * Supports optional executionTarget defaulting to SIMULATION.
 */
public record CompareRequest(
        @NotEmpty(message = "Task list cannot be empty")
        List<@Valid TaskDto> tasks,

        ExecutionTarget executionTarget
) {
    public CompareRequest(List<TaskDto> tasks) {
        this(tasks, ExecutionTarget.SIMULATION);
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
