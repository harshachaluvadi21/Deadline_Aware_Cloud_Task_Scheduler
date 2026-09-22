package web.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import web.dto.*;
import web.service.WorkloadCsvService;

import java.io.IOException;
import java.util.List;

/**
 * Controller handling CSV workload uploading/validation and simulation results export.
 */
@RestController
@RequestMapping("/api")
public class WorkloadController {

    private final WorkloadCsvService workloadCsvService;

    public WorkloadController(WorkloadCsvService workloadCsvService) {
        this.workloadCsvService = workloadCsvService;
    }

    @PostMapping(value = "/workloads/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WorkloadValidationResponse> validateCsvWorkload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    WorkloadValidationResponse.failure(List.of("Uploaded CSV file is empty or missing."))
            );
        }

        try {
            WorkloadValidationResponse response = workloadCsvService.parseAndValidate(file.getInputStream());
            if (!response.valid()) {
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(
                    WorkloadValidationResponse.failure(List.of("Error reading uploaded file: " + e.getMessage()))
            );
        }
    }

    @PostMapping(value = "/export/tasks", produces = "text/csv")
    public ResponseEntity<String> exportTasks(@RequestBody List<TaskResultDto> tasks) {
        String csv = workloadCsvService.exportTaskResultsCsv(tasks);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tasks-result.csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csv);
    }

    @PostMapping(value = "/export/metrics", produces = "text/csv")
    public ResponseEntity<String> exportMetrics(@RequestBody ExportMetricsRequest request) {
        String csv;
        if (request.isComparison()) {
            CompareResponse comp = new CompareResponse(request.baseline(), request.proposed(), null);
            csv = workloadCsvService.exportComparisonMetricsCsv(comp);
        } else {
            String algo = request.algorithm() != null ? request.algorithm() : "SIMULATION";
            csv = workloadCsvService.exportSingleMetricsCsv(algo, request.metrics());
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"metrics-summary.csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csv);
    }
}
