package web.dto;

import java.util.List;

/**
 * Data Transfer Object representing the result of a workload CSV validation attempt.
 */
public record WorkloadValidationResponse(
        boolean valid,
        int taskCount,
        List<TaskDto> tasks,
        List<String> errors
) {
    public static WorkloadValidationResponse success(List<TaskDto> tasks) {
        return new WorkloadValidationResponse(true, tasks.size(), tasks, List.of());
    }

    public static WorkloadValidationResponse failure(List<String> errors) {
        return new WorkloadValidationResponse(false, 0, List.of(), errors);
    }
}
