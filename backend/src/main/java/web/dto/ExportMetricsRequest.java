package web.dto;

/**
 * Request payload for exporting simulation metrics to CSV.
 * Supports both single algorithm results and pairwise comparison results.
 */
public record ExportMetricsRequest(
        String algorithm,
        MetricsDto metrics,
        SimulateResponse baseline,
        SimulateResponse proposed
) {
    public boolean isComparison() {
        return baseline != null && proposed != null;
    }
}
