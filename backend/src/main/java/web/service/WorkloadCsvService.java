package web.service;

import org.springframework.stereotype.Service;
import web.dto.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service responsible for parsing, validating, and generating CSV representations
 * of workloads, task results, and simulation metrics.
 */
@Service
public class WorkloadCsvService {

    private static final String REQUIRED_HEADER = "taskid,priority,arrivaltime,executiontime,deadline";

    /**
     * Parses and validates a CSV workload from an InputStream.
     * Rejects the entire workload if any error is encountered.
     *
     * @param inputStream the CSV stream
     * @return validation response containing either parsed tasks or detailed errors
     */
    public WorkloadValidationResponse parseAndValidate(InputStream inputStream) {
        List<String> errors = new ArrayList<>();
        List<TaskDto> tasks = new ArrayList<>();
        Set<Long> seenTaskIds = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            boolean headerFound = false;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                if (!headerFound) {
                    // Check header
                    String normalizedHeader = trimmed.toLowerCase().replaceAll("\\s+", "");
                    if (!normalizedHeader.equals(REQUIRED_HEADER)) {
                        errors.add("Invalid CSV header at line 1. Expected 'taskId,priority,arrivalTime,executionTime,deadline' but found '" + trimmed + "'.");
                        return WorkloadValidationResponse.failure(errors);
                    }
                    headerFound = true;
                    continue;
                }

                // Process data row
                String[] tokens = trimmed.split(",", -1);
                if (tokens.length != 5) {
                    errors.add("Line " + lineNumber + ": Expected 5 comma-separated values but found " + tokens.length + ".");
                    continue;
                }

                Long taskId = null;
                Integer priority = null;
                Double arrivalTime = null;
                Double executionTime = null;
                Double deadline = null;

                // Validate taskId
                try {
                    taskId = Long.parseLong(tokens[0].trim());
                    if (taskId < 0) {
                        errors.add("Line " + lineNumber + ": Task ID (" + taskId + ") must be non-negative (>= 0).");
                    } else if (seenTaskIds.contains(taskId)) {
                        errors.add("Line " + lineNumber + ": Duplicate Task ID detected (" + taskId + ").");
                    } else {
                        seenTaskIds.add(taskId);
                    }
                } catch (NumberFormatException e) {
                    errors.add("Line " + lineNumber + ": Task ID '" + tokens[0].trim() + "' is not a valid integer.");
                }

                // Validate priority
                try {
                    priority = Integer.parseInt(tokens[1].trim());
                    if (priority < 1 || priority > 10) {
                        errors.add("Line " + lineNumber + ": Priority (" + priority + ") must be an integer between 1 and 10.");
                    }
                } catch (NumberFormatException e) {
                    errors.add("Line " + lineNumber + ": Priority '" + tokens[1].trim() + "' is not a valid integer.");
                }

                // Validate arrivalTime
                try {
                    arrivalTime = Double.parseDouble(tokens[2].trim());
                    if (arrivalTime < 0.0) {
                        errors.add("Line " + lineNumber + ": Arrival time (" + arrivalTime + ") must be non-negative (>= 0.0).");
                    }
                } catch (NumberFormatException e) {
                    errors.add("Line " + lineNumber + ": Arrival time '" + tokens[2].trim() + "' is not a valid number.");
                }

                // Validate executionTime
                try {
                    executionTime = Double.parseDouble(tokens[3].trim());
                    if (executionTime <= 0.0) {
                        errors.add("Line " + lineNumber + ": Execution time (" + executionTime + ") must be strictly positive (> 0.0).");
                    }
                } catch (NumberFormatException e) {
                    errors.add("Line " + lineNumber + ": Execution time '" + tokens[3].trim() + "' is not a valid number.");
                }

                // Validate deadline
                try {
                    deadline = Double.parseDouble(tokens[4].trim());
                    if (deadline < 0.0) {
                        errors.add("Line " + lineNumber + ": Deadline (" + deadline + ") must be non-negative (>= 0.0).");
                    }
                } catch (NumberFormatException e) {
                    errors.add("Line " + lineNumber + ": Deadline '" + tokens[4].trim() + "' is not a valid number.");
                }

                // Cross-field validation: deadline >= arrivalTime
                if (arrivalTime != null && deadline != null && deadline < arrivalTime) {
                    errors.add("Line " + lineNumber + ": Deadline (" + deadline + ") cannot be earlier than arrival time (" + arrivalTime + ").");
                }

                if (taskId != null && priority != null && arrivalTime != null && executionTime != null && deadline != null) {
                    tasks.add(new TaskDto(taskId, priority, arrivalTime, executionTime, deadline));
                }
            }

            if (!headerFound) {
                errors.add("CSV file is empty or missing required header: taskId,priority,arrivalTime,executionTime,deadline");
                return WorkloadValidationResponse.failure(errors);
            }

            if (tasks.isEmpty() && errors.isEmpty()) {
                errors.add("CSV file contains no task data rows.");
                return WorkloadValidationResponse.failure(errors);
            }

        } catch (IOException e) {
            errors.add("Failed to read CSV stream: " + e.getMessage());
            return WorkloadValidationResponse.failure(errors);
        }

        if (!errors.isEmpty()) {
            return WorkloadValidationResponse.failure(errors);
        }

        return WorkloadValidationResponse.success(tasks);
    }

    /**
     * Generates CSV content for task execution results.
     */
    public String exportTaskResultsCsv(List<TaskResultDto> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("taskId,priority,arrivalTime,executionTime,deadline,vmId,startTime,completionTime,status\n");
        if (tasks != null) {
            for (TaskResultDto t : tasks) {
                sb.append(t.taskId()).append(",")
                  .append(t.priority()).append(",")
                  .append(String.format(Locale.US, "%.2f", t.arrivalTime())).append(",")
                  .append(String.format(Locale.US, "%.2f", t.executionTime())).append(",")
                  .append(String.format(Locale.US, "%.2f", t.deadline())).append(",")
                  .append(t.assignedVmId()).append(",")
                  .append(String.format(Locale.US, "%.2f", t.startTime())).append(",")
                  .append(String.format(Locale.US, "%.2f", t.completionTime())).append(",")
                  .append(t.status()).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Generates CSV content for a single algorithm's performance metrics.
     */
    public String exportSingleMetricsCsv(String algorithm, MetricsDto metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("algorithm,makespan,averageWaitingTime,averageTurnaroundTime,throughput,deadlineMissRate,resourceUtilization\n");
        if (metrics != null) {
            sb.append(algorithm).append(",")
              .append(String.format(Locale.US, "%.2f", metrics.makespan())).append(",")
              .append(String.format(Locale.US, "%.2f", metrics.averageWaitingTime())).append(",")
              .append(String.format(Locale.US, "%.2f", metrics.averageTurnaroundTime())).append(",")
              .append(String.format(Locale.US, "%.4f", metrics.throughput())).append(",")
              .append(String.format(Locale.US, "%.2f", metrics.deadlineMissRate())).append(",")
              .append(String.format(Locale.US, "%.2f", metrics.resourceUtilization())).append("\n");
        }
        return sb.toString();
    }

    /**
     * Generates CSV content for pairwise comparison metrics with descriptive difference (Proposed - Baseline).
     */
    public String exportComparisonMetricsCsv(CompareResponse compareResponse) {
        StringBuilder sb = new StringBuilder();
        sb.append("metric,baseline,proposed,difference\n");

        if (compareResponse != null && compareResponse.baseline() != null && compareResponse.proposed() != null) {
            MetricsDto b = compareResponse.baseline().metrics();
            MetricsDto p = compareResponse.proposed().metrics();

            appendComparisonRow(sb, "Makespan (s)", b.makespan(), p.makespan(), "%.2f");
            appendComparisonRow(sb, "Average Waiting Time (s)", b.averageWaitingTime(), p.averageWaitingTime(), "%.2f");
            appendComparisonRow(sb, "Average Turnaround Time (s)", b.averageTurnaroundTime(), p.averageTurnaroundTime(), "%.2f");
            appendComparisonRow(sb, "Throughput (tasks/s)", b.throughput(), p.throughput(), "%.4f");
            appendComparisonRow(sb, "Deadline Miss Rate (%)", b.deadlineMissRate(), p.deadlineMissRate(), "%.2f");
            appendComparisonRow(sb, "VM Resource Utilization (%)", b.resourceUtilization(), p.resourceUtilization(), "%.2f");
        }

        return sb.toString();
    }

    private void appendComparisonRow(StringBuilder sb, String metricName, double baseline, double proposed, String format) {
        double diff = proposed - baseline;
        sb.append(metricName).append(",")
          .append(String.format(Locale.US, format, baseline)).append(",")
          .append(String.format(Locale.US, format, proposed)).append(",")
          .append(String.format(Locale.US, format, diff)).append("\n");
    }
}
