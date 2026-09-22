package web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import web.dto.CompareRequest;
import web.dto.SimulateRequest;
import web.dto.TaskDto;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SimulationController Web API Tests")
class SimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/health returns status UP")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /api/sample-workload returns 5 sample tasks")
    void testSampleWorkloadEndpoint() throws Exception {
        mockMvc.perform(get("/api/sample-workload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].taskId").value(0))
                .andExpect(jsonPath("$[0].priority").value(5));
    }

    @Test
    @DisplayName("POST /api/simulate succeeds for BASELINE algorithm")
    void testSimulateBaseline() throws Exception {
        SimulateRequest request = new SimulateRequest(
                "BASELINE",
                List.of(
                        new TaskDto(0L, 5, 0.0, 10.0, 20.0),
                        new TaskDto(1L, 8, 1.0, 5.0, 15.0)
                )
        );

        mockMvc.perform(post("/api/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("BASELINE"))
                .andExpect(jsonPath("$.tasks", hasSize(2)))
                .andExpect(jsonPath("$.metrics.makespan").isNumber())
                .andExpect(jsonPath("$.metrics.resourceUtilization").isNumber());
    }

    @Test
    @DisplayName("POST /api/simulate succeeds for PROPOSED algorithm")
    void testSimulateProposed() throws Exception {
        SimulateRequest request = new SimulateRequest(
                "PROPOSED",
                List.of(
                        new TaskDto(0L, 5, 0.0, 10.0, 20.0),
                        new TaskDto(1L, 8, 1.0, 5.0, 15.0)
                )
        );

        mockMvc.perform(post("/api/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("PROPOSED"))
                .andExpect(jsonPath("$.tasks", hasSize(2)))
                .andExpect(jsonPath("$.metrics.makespan").isNumber())
                .andExpect(jsonPath("$.metrics.resourceUtilization").isNumber());
    }

    @Test
    @DisplayName("POST /api/compare executes both schedulers and computes differences")
    void testCompareEndpoint() throws Exception {
        CompareRequest request = new CompareRequest(
                List.of(
                        new TaskDto(0L, 5, 0.0, 10.0, 25.0),
                        new TaskDto(1L, 8, 2.0, 8.0, 18.0)
                )
        );

        mockMvc.perform(post("/api/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseline.algorithm").value("BASELINE"))
                .andExpect(jsonPath("$.proposed.algorithm").value("PROPOSED"))
                .andExpect(jsonPath("$.comparison.makespanDifference").isNumber())
                .andExpect(jsonPath("$.comparison.waitingTimeDifference").isNumber());
    }

    @Test
    @DisplayName("POST /api/simulate rejects invalid priority (> 10 or < 1) with HTTP 400")
    void testInvalidPriority() throws Exception {
        SimulateRequest request = new SimulateRequest(
                "BASELINE",
                List.of(new TaskDto(0L, 15, 0.0, 10.0, 20.0)) // Priority 15 is invalid
        );

        mockMvc.perform(post("/api/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /api/simulate rejects invalid execution time (<= 0) with HTTP 400")
    void testInvalidExecutionTime() throws Exception {
        SimulateRequest request = new SimulateRequest(
                "BASELINE",
                List.of(new TaskDto(0L, 5, 0.0, -5.0, 20.0)) // Execution -5.0 is invalid
        );

        mockMvc.perform(post("/api/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /api/simulate rejects deadline earlier than arrival time with HTTP 400")
    void testDeadlineEarlierThanArrival() throws Exception {
        SimulateRequest request = new SimulateRequest(
                "BASELINE",
                List.of(new TaskDto(0L, 5, 10.0, 5.0, 5.0)) // Arrival 10.0 > Deadline 5.0
        );

        mockMvc.perform(post("/api/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Request"));
    }

    @Test
    @DisplayName("POST /api/simulate rejects unsupported algorithm with HTTP 400")
    void testInvalidAlgorithm() throws Exception {
        SimulateRequest request = new SimulateRequest(
                "RANDOM_HEURISTIC",
                List.of(new TaskDto(0L, 5, 0.0, 10.0, 20.0))
        );

        mockMvc.perform(post("/api/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /api/compare rejects empty task list with HTTP 400")
    void testEmptyTasks() throws Exception {
        CompareRequest request = new CompareRequest(Collections.emptyList());

        mockMvc.perform(post("/api/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }
}
