package web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * Request payload for /api/simulate.
 */
public record SimulateRequest(
        @NotBlank(message = "Algorithm cannot be blank")
        @Pattern(regexp = "(?i)^(BASELINE|PROPOSED)$", message = "Algorithm must be either 'BASELINE' or 'PROPOSED'")
        String algorithm,

        @NotEmpty(message = "Task list cannot be empty")
        List<@Valid TaskDto> tasks
) {
    public void validate() {
        if (tasks != null) {
            tasks.forEach(TaskDto::validate);
        }
    }
}
