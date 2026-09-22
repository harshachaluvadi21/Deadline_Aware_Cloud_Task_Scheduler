package web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import web.dto.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Workload CSV Import and Result Export Tests")
class WorkloadImportExportTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Valid CSV workload upload succeeds with HTTP 200 and parsed tasks")
    void testValidCsvUpload() throws Exception {
        String csvContent = "taskId,priority,arrivalTime,executionTime,deadline\n" +
                "0,5,0.0,15.65,47.31\n" +
                "1,8,1.0,8.0,12.0\n" +
                "2,3,4.0,12.5,40.0\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "workload.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/workloads/validate").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.taskCount").value(3))
                .andExpect(jsonPath("$.tasks", hasSize(3)))
                .andExpect(jsonPath("$.tasks[0].taskId").value(0))
                .andExpect(jsonPath("$.tasks[0].priority").value(5))
                .andExpect(jsonPath("$.tasks[1].taskId").value(1))
                .andExpect(jsonPath("$.tasks[1].priority").value(8))
                .andExpect(jsonPath("$.errors", empty()));
    }

    @Test
    @DisplayName("Upload rejects malformed/incorrect CSV header with HTTP 400")
    void testMalformedHeaderCsv() throws Exception {
        String csvContent = "id,prio,arrival,exec,dl\n" +
                "0,5,0.0,15.65,47.31\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "bad_header.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/workloads/validate").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0]", containsString("Invalid CSV header")));
    }

    @Test
    @DisplayName("Upload rejects empty CSV file with HTTP 400")
    void testEmptyCsv() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.csv", "text/csv", "".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/workloads/validate").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0]", containsString("empty")));
    }

    @Test
    @DisplayName("Upload rejects duplicate task IDs with HTTP 400")
    void testDuplicateTaskIds() throws Exception {
        String csvContent = "taskId,priority,arrivalTime,executionTime,deadline\n" +
                "0,5,0.0,15.65,47.31\n" +
                "0,8,1.0,8.0,12.0\n"; // duplicate ID 0

        MockMultipartFile file = new MockMultipartFile(
                "file", "duplicate.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/workloads/validate").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0]", containsString("Duplicate Task ID")));
    }

    @Test
    @DisplayName("Upload rejects priority outside 1-10 with HTTP 400")
    void testInvalidPriority() throws Exception {
        String csvContent = "taskId,priority,arrivalTime,executionTime,deadline\n" +
                "0,12,0.0,15.65,47.31\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "invalid_prio.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/workloads/validate").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0]", containsString("Priority (12) must be an integer between 1 and 10")));
    }

    @Test
    @DisplayName("Upload rejects non-positive execution time with HTTP 400")
    void testInvalidExecutionTime() throws Exception {
        String csvContent = "taskId,priority,arrivalTime,executionTime,deadline\n" +
                "0,5,0.0,0.0,47.31\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "zero_exec.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/workloads/validate").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0]", containsString("Execution time (0.0) must be strictly positive")));
    }

    @Test
    @DisplayName("Upload rejects deadline earlier than arrival time with HTTP 400")
    void testInvalidDeadlineEarlierThanArrival() throws Exception {
        String csvContent = "taskId,priority,arrivalTime,executionTime,deadline\n" +
                "0,5,10.0,5.0,8.0\n"; // deadline 8.0 < arrival 10.0

        MockMultipartFile file = new MockMultipartFile(
                "file", "early_deadline.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/workloads/validate").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0]", containsString("cannot be earlier than arrival time")));
    }

    @Test
    @DisplayName("Upload rejects negative arrival time with HTTP 400")
    void testInvalidNegativeArrival() throws Exception {
        String csvContent = "taskId,priority,arrivalTime,executionTime,deadline\n" +
                "0,5,-2.0,5.0,8.0\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "neg_arrival.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/workloads/validate").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0]", containsString("Arrival time (-2.0) must be non-negative")));
    }

    @Test
    @DisplayName("Upload rejects non-numeric fields with HTTP 400")
    void testNonNumericValues() throws Exception {
        String csvContent = "taskId,priority,arrivalTime,executionTime,deadline\n" +
                "abc,5,0.0,10.0,20.0\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "bad_type.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/workloads/validate").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0]", containsString("not a valid integer")));
    }

    @Test
    @DisplayName("POST /api/export/tasks returns valid CSV attachment")
    void testExportTasksCsv() throws Exception {
        List<TaskResultDto> results = List.of(
                new TaskResultDto(0, 5, 0.0, 10.0, 25.0, 1, 0.0, 10.0, 0.0, 10.0, "SUCCESS", false),
                new TaskResultDto(1, 8, 2.0, 12.0, 15.0, 2, 2.0, 14.0, 0.0, 12.0, "SUCCESS", false)
        );

        String response = mockMvc.perform(post("/api/export/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(results)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"tasks-result.csv\""))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn().getResponse().getContentAsString();

        assertTrue(response.startsWith("taskId,priority,arrivalTime,executionTime,deadline,vmId,startTime,completionTime,status"));
        assertTrue(response.contains("0,5,0.00,10.00,25.00,1,0.00,10.00,SUCCESS"));
        assertTrue(response.contains("1,8,2.00,12.00,15.00,2,2.00,14.00,SUCCESS"));
    }

    @Test
    @DisplayName("POST /api/export/metrics returns single algorithm CSV")
    void testExportSingleMetricsCsv() throws Exception {
        MetricsDto metrics = new MetricsDto(55.20, 12.40, 24.80, 0.3620, 0.00, 0, 20, 78.50);
        ExportMetricsRequest req = new ExportMetricsRequest("BASELINE", metrics, null, null);

        String response = mockMvc.perform(post("/api/export/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"metrics-summary.csv\""))
                .andReturn().getResponse().getContentAsString();

        assertTrue(response.startsWith("algorithm,makespan,averageWaitingTime,averageTurnaroundTime,throughput,deadlineMissRate,resourceUtilization"));
        assertTrue(response.contains("BASELINE,55.20,12.40,24.80,0.3620,0.00,78.50"));
    }

    @Test
    @DisplayName("POST /api/export/metrics returns comparison metrics CSV with descriptive difference (Proposed - Baseline)")
    void testExportComparisonMetricsCsv() throws Exception {
        MetricsDto m1 = new MetricsDto(60.00, 15.00, 30.00, 0.3333, 5.00, 1, 19, 70.00);
        MetricsDto m2 = new MetricsDto(55.00, 12.00, 27.00, 0.3636, 0.00, 0, 20, 75.00);

        SimulateResponse baseline = new SimulateResponse("BASELINE", List.of(), m1);
        SimulateResponse proposed = new SimulateResponse("PROPOSED", List.of(), m2);
        ExportMetricsRequest req = new ExportMetricsRequest(null, null, baseline, proposed);

        String response = mockMvc.perform(post("/api/export/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"metrics-summary.csv\""))
                .andReturn().getResponse().getContentAsString();

        assertTrue(response.startsWith("metric,baseline,proposed,difference"));
        // Makespan: 55.00 - 60.00 = -5.00
        assertTrue(response.contains("Makespan (s),60.00,55.00,-5.00"));
        // Deadline Miss Rate: 0.00 - 5.00 = -5.00
        assertTrue(response.contains("Deadline Miss Rate (%),5.00,0.00,-5.00"));
        // Resource Utilization: 75.00 - 70.00 = 5.00
        assertTrue(response.contains("VM Resource Utilization (%),70.00,75.00,5.00"));
    }
}
