package web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Health probe status returned by the real worker node's GET /health endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CloudWorkerHealthResponse(
        String status,
        String worker,
        String host,
        Boolean psutilAvailable,
        boolean reachable
) {
    public static CloudWorkerHealthResponse unreachable(String error) {
        return new CloudWorkerHealthResponse("DOWN", "unknown", error, false, false);
    }

    public static CloudWorkerHealthResponse online(String status, String worker, String host, Boolean psutilAvailable) {
        return new CloudWorkerHealthResponse(status, worker, host, psutilAvailable, true);
    }
}
