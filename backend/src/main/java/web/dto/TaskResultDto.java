package web.dto;

/**
 * Data Transfer Object representing the completed execution of an individual task.
 */
public record TaskResultDto(
        long taskId,
        int priority,
        double arrivalTime,
        double executionTime,
        double deadline,
        long assignedVmId,
        double startTime,
        double completionTime,
        double waitingTime,
        double turnaroundTime,
        String status,
        boolean deadlineMissed
) {}
