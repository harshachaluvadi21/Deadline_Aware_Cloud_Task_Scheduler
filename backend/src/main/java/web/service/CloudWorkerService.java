package web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import scheduler.model.Task;
import web.dto.CloudTaskExecutionRequest;
import web.dto.CloudTaskExecutionResponse;
import web.dto.CloudWorkerHealthResponse;
import web.dto.TaskDto;
import web.exception.CloudWorkerException;

import java.time.Duration;
import java.util.Objects;

/**
 * Communication service between the Spring Boot application and the real execution worker node.
 *
 * <p><b>Architectural Boundary:</b>
 * This service is purely an execution dispatcher and telemetry client.
 * It contains NO scheduling heuristics, NO priority/deadline calculation,
 * NO Fibonacci Heap logic, and NO VM selection algorithms.
 * It strictly takes an already-selected task, dispatches it to the worker over HTTP,
 * and receives the actual execution result.
 */
@Service
public class CloudWorkerService {

    private static final Logger log = LoggerFactory.getLogger(CloudWorkerService.class);

    private final String workerUrl;
    private final RestClient restClient;

    public CloudWorkerService(
            @Value("${cloud.worker.url:${aws.ec2.worker.url:http://127.0.0.1:5000}}") String workerUrl,
            RestClient.Builder restClientBuilder) {

        this.workerUrl = workerUrl.replaceAll("/+$", "");

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));

        this.restClient = restClientBuilder
                .baseUrl(this.workerUrl)
                .requestFactory(requestFactory)
                .build();

        log.info("CloudWorkerService initialized with worker URL: {}", this.workerUrl);
    }

    /**
     * Checks the health and reachability of the real execution worker without throwing an exception.
     *
     * @return CloudWorkerHealthResponse containing worker telemetry or unreachable status
     */
    public CloudWorkerHealthResponse healthCheck() {
        try {
            CloudWorkerHealthResponse response = restClient.get()
                    .uri("/health")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(CloudWorkerHealthResponse.class);

            if (response != null) {
                return CloudWorkerHealthResponse.online(
                        response.status(),
                        response.worker(),
                        response.host(),
                        response.psutilAvailable()
                );
            }
            return CloudWorkerHealthResponse.unreachable("Empty response received from worker");
        } catch (Exception ex) {
            log.warn("Worker health check probe failed for URL {}: {}", workerUrl, ex.getMessage());
            return CloudWorkerHealthResponse.unreachable(ex.getMessage());
        }
    }

    /**
     * Convenience boolean probe for checking worker reachability.
     */
    public boolean isWorkerReachable() {
        return healthCheck().reachable();
    }

    /**
     * Dispatches an already-selected domain Task to the real worker node for actual CPU execution.
     *
     * @param task the domain task selected by the scheduler
     * @return the execution response with actual measured duration and CPU utilization
     * @throws CloudWorkerException if communication with the worker fails or times out
     */
    public CloudTaskExecutionResponse executeTask(Task task) {
        Objects.requireNonNull(task, "Task cannot be null");
        CloudTaskExecutionRequest request = new CloudTaskExecutionRequest(
                String.valueOf(task.getTaskId()),
                task.getExecutionTime(),
                task.getPriority(),
                task.getDeadline()
        );
        return executeTask(request);
    }

    /**
     * Dispatches a TaskDto to the real worker node for actual CPU execution.
     *
     * @param taskDto the task DTO specification
     * @return the execution response with actual measured duration and CPU utilization
     * @throws CloudWorkerException if communication with the worker fails or times out
     */
    public CloudTaskExecutionResponse executeTask(TaskDto taskDto) {
        Objects.requireNonNull(taskDto, "TaskDto cannot be null");
        CloudTaskExecutionRequest request = new CloudTaskExecutionRequest(
                String.valueOf(taskDto.taskId()),
                taskDto.executionTime(),
                taskDto.priority(),
                taskDto.deadline()
        );
        return executeTask(request);
    }

    /**
     * Dispatches a typed execution request to the real worker node.
     *
     * @param request the task payload containing taskId, executionTime, priority, deadline
     * @return the execution response with actual measured duration and CPU utilization
     * @throws CloudWorkerException if communication with the worker fails or times out
     */
    public CloudTaskExecutionResponse executeTask(CloudTaskExecutionRequest request) {
        Objects.requireNonNull(request, "Execution request cannot be null");
        log.info("Dispatching task {} (duration={}s) to real worker at {}",
                request.taskId(), request.executionTime(), workerUrl);

        try {
            CloudTaskExecutionResponse response = restClient.post()
                    .uri("/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(CloudTaskExecutionResponse.class);

            if (response == null) {
                throw new CloudWorkerException("Worker at " + workerUrl + " returned an empty response for task " + request.taskId());
            }

            log.info("Task {} completed on real worker in {}s (CPU: {}%)",
                    response.taskId(), response.actualExecutionTimeSeconds(), response.cpuUtilization());
            return response;

        } catch (RestClientException ex) {
            String errorMsg = "Worker unavailable at " + workerUrl + ": " + ex.getMessage();
            log.error("Failed to execute task {} on real worker: {}", request.taskId(), errorMsg);
            throw new CloudWorkerException(errorMsg, ex);
        } catch (Exception ex) {
            String errorMsg = "Unexpected error communicating with real worker at " + workerUrl + ": " + ex.getMessage();
            log.error(errorMsg, ex);
            throw new CloudWorkerException(errorMsg, ex);
        }
    }

    public String getWorkerUrl() {
        return workerUrl;
    }
}
