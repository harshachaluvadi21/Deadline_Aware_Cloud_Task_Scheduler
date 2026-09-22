package scheduler.heap;

/**
 * Composite, deterministic comparison key for priority queue ordering.
 * Encapsulates:
 * <ul>
 *   <li><b>primaryScore:</b> Primary priority metric (e.g. deadline or priority score)</li>
 *   <li><b>secondaryScore:</b> Secondary tie-breaking metric (e.g. waiting time or arrival time)</li>
 *   <li><b>sequenceId:</b> Tertiary unique identifier (e.g. task ID) guaranteeing a strict total order</li>
 * </ul>
 *
 * <p>Designed to be completely generic and decoupled from any specific scheduling algorithms
 * or simulation frameworks.
 */
public record HeapKey(
    double primaryScore,
    double secondaryScore,
    long sequenceId
) implements Comparable<HeapKey> {

    /**
     * Compact constructor validating finite floating-point values.
     */
    public HeapKey {
        if (!Double.isFinite(primaryScore)) {
            throw new IllegalArgumentException("Primary score must be finite, got: " + primaryScore);
        }
        if (!Double.isFinite(secondaryScore)) {
            throw new IllegalArgumentException("Secondary score must be finite, got: " + secondaryScore);
        }
    }

    /**
     * Convenience factory method with default secondary score of 0.0.
     *
     * @param primaryScore primary ordering metric
     * @param sequenceId   tertiary sequence identifier
     * @return a new HeapKey instance
     */
    public static HeapKey of(double primaryScore, long sequenceId) {
        return new HeapKey(primaryScore, 0.0, sequenceId);
    }

    /**
     * Full factory method.
     *
     * @param primaryScore   primary ordering metric
     * @param secondaryScore secondary ordering metric
     * @param sequenceId     tertiary sequence identifier
     * @return a new HeapKey instance
     */
    public static HeapKey of(double primaryScore, double secondaryScore, long sequenceId) {
        return new HeapKey(primaryScore, secondaryScore, sequenceId);
    }

    /**
     * Strict deterministic natural comparison.
     * Evaluates primary score first, then secondary score, and finally sequenceId.
     *
     * @param other the other HeapKey to compare against
     * @return negative if this &lt; other, positive if this &gt; other, 0 if identical
     */
    @Override
    public int compareTo(HeapKey other) {
        if (other == null) {
            throw new NullPointerException("Cannot compare HeapKey to null");
        }
        int cmp1 = Double.compare(this.primaryScore, other.primaryScore);
        if (cmp1 != 0) {
            return cmp1;
        }
        int cmp2 = Double.compare(this.secondaryScore, other.secondaryScore);
        if (cmp2 != 0) {
            return cmp2;
        }
        return Long.compare(this.sequenceId, other.sequenceId);
    }
}
