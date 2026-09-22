package web.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import web.dto.*;
import web.service.SimulationService;

import java.util.List;
import java.util.Map;

/**
 * REST Controller providing endpoints for health probes, sample workload retrieval,
 * single-algorithm simulations, and dual-algorithm comparisons.
 */
@RestController
@RequestMapping("/api")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/sample-workload")
    public ResponseEntity<List<TaskDto>> getSampleWorkload() {
        return ResponseEntity.ok(simulationService.getSampleWorkload());
    }

    @PostMapping("/simulate")
    public ResponseEntity<SimulateResponse> simulate(@Valid @RequestBody SimulateRequest request) {
        request.validate();
        SimulateResponse response = simulationService.simulate(request.algorithm(), request.tasks());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/compare")
    public ResponseEntity<CompareResponse> compare(@Valid @RequestBody CompareRequest request) {
        request.validate();
        CompareResponse response = simulationService.compare(request.tasks());
        return ResponseEntity.ok(response);
    }
}
