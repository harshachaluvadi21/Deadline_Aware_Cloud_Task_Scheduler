package scheduler.proposed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PriorityNormalization} covering priority 1, 10, intermediate values,
 * custom bounds, and validation checks.
 */
class PriorityNormalizationTest {

    @Test
    @DisplayName("Normalizes minimum base priority (1) to 0.1")
    void testPriorityMin() {
        assertEquals(0.1, PriorityNormalization.normalize(1), 1e-9);
    }

    @Test
    @DisplayName("Normalizes maximum base priority (10) to 1.0")
    void testPriorityMax() {
        assertEquals(1.0, PriorityNormalization.normalize(10), 1e-9);
    }

    @Test
    @DisplayName("Normalizes intermediate base priorities linearly")
    void testIntermediatePriorities() {
        assertEquals(0.5, PriorityNormalization.normalize(5), 1e-9);
        assertEquals(0.2, PriorityNormalization.normalize(2), 1e-9);
        assertEquals(0.7, PriorityNormalization.normalize(7), 1e-9);
    }

    @Test
    @DisplayName("Supports custom maximum priority bounds")
    void testCustomMaxPriority() {
        assertEquals(0.5, PriorityNormalization.normalize(50, 100), 1e-9);
        assertEquals(1.0, PriorityNormalization.normalize(100, 100), 1e-9);
        assertEquals(0.01, PriorityNormalization.normalize(1, 100), 1e-9);
    }

    @Test
    @DisplayName("Throws IllegalArgumentException on invalid priority inputs")
    void testValidation() {
        assertThrows(IllegalArgumentException.class, () -> PriorityNormalization.normalize(0));
        assertThrows(IllegalArgumentException.class, () -> PriorityNormalization.normalize(-1));
        assertThrows(IllegalArgumentException.class, () -> PriorityNormalization.normalize(11));
        assertThrows(IllegalArgumentException.class, () -> PriorityNormalization.normalize(5, 0));
        assertThrows(IllegalArgumentException.class, () -> PriorityNormalization.normalize(5, 4));
    }
}
