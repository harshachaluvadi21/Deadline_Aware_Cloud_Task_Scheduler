package web.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import web.dto.CloudTaskExecutionRequest;
import web.dto.CloudTaskExecutionResponse;
import web.dto.CloudWorkerHealthResponse;
import web.service.CloudWorkerService;

import java.util.Map;

/**
 * Controller exposing diagnostic and health check endpoints for the real execution worker node.
 * Supports both /api/worker and legacy /api/ec2 for backward compatibility.
 */
@RestController
@RequestMapping({"/api/worker", "/api/ec2"})
public class CloudWorkerController {

    private final CloudWorkerService cloudWorkerService;

    public CloudWorkerController(CloudWorkerService cloudWorkerService) {
        this.cloudWorkerService = cloudWorkerService;
    }

    /**
     * Probes the health and connectivity of the configured worker node.
     */
    @GetMapping("/health")
    public ResponseEntity<CloudWorkerHealthResponse> checkWorkerHealth() {
        CloudWorkerHealthResponse health = cloudWorkerService.healthCheck();
        return ResponseEntity.ok(health);
    }

    /**
     * Dispatches an individual task directly to the worker node for diagnostic execution.
     */
    @PostMapping("/execute")
    public ResponseEntity<CloudTaskExecutionResponse> executeOnWorker(
            @Valid @RequestBody CloudTaskExecutionRequest request) {
        CloudTaskExecutionResponse response = cloudWorkerService.executeTask(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the currently configured worker target URL.
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getWorkerConfig() {
        return ResponseEntity.ok(Map.of("workerUrl", cloudWorkerService.getWorkerUrl()));
    }
}
