package web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request payload for /api/compare.
 */
public record CompareRequest(
        @NotEmpty(message = "Task list cannot be empty")
        List<@Valid TaskDto> tasks
) {
    public void validate() {
        if (tasks != null) {
            tasks.forEach(TaskDto::validate);
        }
    }
}
