package web.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import scheduler.model.Task;
import web.dto.CloudTaskExecutionRequest;
import web.dto.CloudWorkerHealthResponse;
import web.dto.TaskDto;
import web.exception.CloudWorkerException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CloudWorkerService Unit Tests")
class CloudWorkerServiceTest {

    @Test
    @DisplayName("Worker health check returns unreachable status gracefully when worker is down")
    void testHealthCheckWhenWorkerOffline() {
        // Pointing to a definitely closed port to verify graceful offline handling
        String unreachableUrl = "http://127.0.0.1:59999";
        CloudWorkerService service = new CloudWorkerService(unreachableUrl, RestClient.builder());

        CloudWorkerHealthResponse health = service.healthCheck();

        assertNotNull(health);
        assertFalse(health.reachable());
        assertEquals("DOWN", health.status());
        assertFalse(service.isWorkerReachable());
    }

    @Test
    @DisplayName("executeTask throws CloudWorkerException when worker is unreachable without crashing")
    void testExecuteTaskWhenWorkerOffline() {
        String unreachableUrl = "http://127.0.0.1:59999";
        CloudWorkerService service = new CloudWorkerService(unreachableUrl, RestClient.builder());

        CloudTaskExecutionRequest request = new CloudTaskExecutionRequest("TEST-1", 1.0, 8, 10.0);

        CloudWorkerException thrown = assertThrows(CloudWorkerException.class, () -> service.executeTask(request));
        assertTrue(thrown.getMessage().contains("Worker unavailable"));
    }

    @Test
    @DisplayName("executeTask throws NullPointerException for null inputs")
    void testNullGuards() {
        CloudWorkerService service = new CloudWorkerService("http://127.0.0.1:5000", RestClient.builder());

        assertThrows(NullPointerException.class, () -> service.executeTask((Task) null));
        assertThrows(NullPointerException.class, () -> service.executeTask((TaskDto) null));
        assertThrows(NullPointerException.class, () -> service.executeTask((CloudTaskExecutionRequest) null));
    }
}
