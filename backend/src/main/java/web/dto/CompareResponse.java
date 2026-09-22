package web.dto;

/**
 * Response payload for /api/compare.
 */
public record CompareResponse(
        SimulateResponse baseline,
        SimulateResponse proposed,
        PairwiseComparisonDto comparison
) {}
