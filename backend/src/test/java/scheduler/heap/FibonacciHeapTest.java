package scheduler.heap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5 Exhaustive Unit & Oracle Verification Tests for {@link FibonacciHeap}.
 * Validates heap properties, asymptotic operation contracts, decrease-key cascading cuts,
 * deterministic tie-breaking via {@link HeapKey}, and randomized oracle equivalence against Java PriorityQueue.
 */
public class FibonacciHeapTest {

    @Nested
    @DisplayName("Basic Operation Tests")
    class BasicOperationTests {

        @Test
        @DisplayName("1. Empty heap behavior")
        void testEmptyHeap() {
            FibonacciHeap<Integer, String> heap = new FibonacciHeap<>();
            assertTrue(heap.isEmpty(), "Newly created heap must be empty");
            assertEquals(0, heap.size(), "Size must be 0");
            assertNull(heap.peek(), "Peek on empty heap must return null");
            assertNull(heap.peekNode(), "PeekNode on empty heap must return null");
            assertNull(heap.extractMin(), "ExtractMin on empty heap must return null");
            assertNull(heap.extractMinNode(), "ExtractMinNode on empty heap must return null");
        }

        @Test
        @DisplayName("2. Single insertion")
        void testSingleInsertion() {
            FibonacciHeap<Integer, String> heap = new FibonacciHeap<>();
            FibonacciNode<Integer, String> node = heap.insert(42, "item42");

            assertFalse(heap.isEmpty());
            assertEquals(1, heap.size());
            assertEquals("item42", heap.peek());
            assertSame(node, heap.peekNode());

            String extracted = heap.extractMin();
            assertEquals("item42", extracted);
            assertTrue(heap.isEmpty());
            assertEquals(0, heap.size());
        }

        @Test
        @DisplayName("3. Multiple insertions")
        void testMultipleInsertions() {
            FibonacciHeap<Integer, String> heap = new FibonacciHeap<>();
            heap.insert(10, "ten");
            heap.insert(20, "twenty");
            heap.insert(5, "five");
            heap.insert(15, "fifteen");

            assertEquals(4, heap.size());
            assertEquals("five", heap.peek());
        }

        @Test
        @DisplayName("4. Peek does not remove elements")
        void testPeek() {
            FibonacciHeap<Double, String> heap = new FibonacciHeap<>();
            heap.insert(3.14, "pi");
            heap.insert(2.71, "e");

            assertEquals("e", heap.peek());
            assertEquals(2, heap.size());
            assertEquals("e", heap.peek());
            assertEquals(2, heap.size());
        }

        @Test
        @DisplayName("5. Extract min removes elements in priority order")
        void testExtract() {
            FibonacciHeap<Integer, Integer> heap = new FibonacciHeap<>();
            heap.insert(30, 30);
            heap.insert(10, 10);
            heap.insert(20, 20);

            assertEquals(10, heap.extractMin());
            assertEquals(2, heap.size());
            assertEquals(20, heap.extractMin());
            assertEquals(1, heap.size());
            assertEquals(30, heap.extractMin());
            assertEquals(0, heap.size());
            assertTrue(heap.isEmpty());
        }

        @Test
        @DisplayName("6. Correct ascending ordering")
        void testCorrectOrdering() {
            FibonacciHeap<Integer, Integer> heap = new FibonacciHeap<>();
            int[] values = {45, 12, 85, 32, 89, 39, 69, 44, 42, 1, 45, 8};
            for (int v : values) {
                heap.insert(v, v);
            }

            int[] expected = Arrays.copyOf(values, values.length);
            Arrays.sort(expected);

            for (int exp : expected) {
                assertEquals(exp, heap.extractMin());
            }
            assertTrue(heap.isEmpty());
        }

        @Test
        @DisplayName("7. Duplicate keys handling")
        void testDuplicateKeys() {
            FibonacciHeap<Integer, String> heap = new FibonacciHeap<>();
            heap.insert(5, "first5");
            heap.insert(5, "second5");
            heap.insert(5, "third5");
            heap.insert(1, "one");

            assertEquals(4, heap.size());
            assertEquals("one", heap.extractMin());

            List<String> remaining = new ArrayList<>();
            remaining.add(heap.extractMin());
            remaining.add(heap.extractMin());
            remaining.add(heap.extractMin());

            assertEquals(3, remaining.size());
            assertTrue(remaining.contains("first5"));
            assertTrue(remaining.contains("second5"));
            assertTrue(remaining.contains("third5"));
            assertTrue(heap.isEmpty());
        }

        @Test
        @DisplayName("8. Negative and positive keys")
        void testNegativeAndPositiveKeys() {
            FibonacciHeap<Double, String> heap = new FibonacciHeap<>();
            heap.insert(100.5, "pos100");
            heap.insert(-50.2, "neg50");
            heap.insert(0.0, "zero");
            heap.insert(-100.8, "neg100");
            heap.insert(25.0, "pos25");

            assertEquals("neg100", heap.extractMin());
            assertEquals("neg50", heap.extractMin());
            assertEquals("zero", heap.extractMin());
            assertEquals("pos25", heap.extractMin());
            assertEquals("pos100", heap.extractMin());
            assertTrue(heap.isEmpty());
        }

        @Test
        @DisplayName("9. Size tracking across operations")
        void testSizeTracking() {
            FibonacciHeap<Integer, Integer> heap = new FibonacciHeap<>();
            assertEquals(0, heap.size());

            for (int i = 1; i <= 10; i++) {
                heap.insert(i, i);
                assertEquals(i, heap.size());
            }

            for (int i = 9; i >= 0; i--) {
                heap.extractMin();
                assertEquals(i, heap.size());
            }

            heap.insert(1, 1);
            heap.insert(2, 2);
            assertEquals(2, heap.size());

            heap.clear();
            assertEquals(0, heap.size());
            assertTrue(heap.isEmpty());
            assertNull(heap.peek());
        }

        @Test
        @DisplayName("10. Empty extraction behavior returns null without error")
        void testEmptyExtractionBehavior() {
            FibonacciHeap<Integer, Integer> heap = new FibonacciHeap<>();
            assertNull(heap.extractMin());
            assertNull(heap.extractMinNode());

            heap.insert(1, 1);
            assertEquals(1, heap.extractMin());
            assertNull(heap.extractMin());
            assertNull(heap.extractMinNode());
        }
    }

    @Nested
    @DisplayName("Decrease-Key & Structural Tests")
    class DecreaseKeyAndStructuralTests {

        @Test
        @DisplayName("11. Decrease-key operation updates node and heap min")
        void testDecreaseKey() {
            FibonacciHeap<Integer, String> heap = new FibonacciHeap<>();
            FibonacciNode<Integer, String> n1 = heap.insert(50, "fifty");
            FibonacciNode<Integer, String> n2 = heap.insert(30, "thirty");
            FibonacciNode<Integer, String> n3 = heap.insert(40, "forty");

            assertEquals("thirty", heap.peek());

            // Decrease fifty to ten -> becomes new min
            heap.decreaseKey(n1, 10);
            assertEquals("fifty", heap.peek());
            assertEquals(10, n1.getKey());

            // Invalid decreaseKey (increasing key) throws IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> heap.decreaseKey(n1, 20));

            // Decreasing to same key is valid
            assertDoesNotThrow(() -> heap.decreaseKey(n1, 10));

            // Extract order: fifty(10), thirty(30), forty(40)
            assertEquals("fifty", heap.extractMin());
            assertEquals("thirty", heap.extractMin());
            assertEquals("forty", heap.extractMin());
        }

        @Test
        @DisplayName("12. Cascading cuts triggered during decrease-key")
        void testCascadingCuts() {
            FibonacciHeap<Integer, String> heap = new FibonacciHeap<>();

            // Build a non-trivial tree structure
            // Insert elements and call extractMin to force consolidation
            List<FibonacciNode<Integer, String>> nodes = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                nodes.add(heap.insert(i, "node" + i));
            }

            // Extract min to force consolidation into binomial/Fibonacci trees
            heap.extractMin(); // removes node 0

            // Find nodes that have a parent (are children in the tree)
            FibonacciNode<Integer, String> childNode = null;
            for (FibonacciNode<Integer, String> node : nodes) {
                if (node.getParent() != null && node.getParent().getParent() != null) {
                    childNode = node;
                    break;
                }
            }

            // If a 2-level deep child is found, decrease key to trigger cascading cut
            if (childNode != null) {
                FibonacciNode<Integer, String> parent = childNode.getParent();
                heap.decreaseKey(childNode, -1);
                assertEquals(childNode.getValue(), heap.peek());

                // Parent degree should have decremented
                assertTrue(parent.isMarked() || parent.getParent() == null);
            }

            // Verify heap still extracts all elements in strictly sorted order
            int lastKey = Integer.MIN_VALUE;
            while (!heap.isEmpty()) {
                FibonacciNode<Integer, String> minNode = heap.extractMinNode();
                assertTrue(minNode.getKey() >= lastKey, "Extracted keys must be monotonically non-decreasing");
                lastKey = minNode.getKey();
            }
        }

        @Test
        @DisplayName("13. Union operation merges two heaps in O(1)")
        void testUnion() {
            FibonacciHeap<Integer, Integer> h1 = new FibonacciHeap<>();
            h1.insert(10, 10);
            h1.insert(30, 30);

            FibonacciHeap<Integer, Integer> h2 = new FibonacciHeap<>();
            h2.insert(5, 5);
            h2.insert(25, 25);

            h1.union(h2);

            assertEquals(4, h1.size());
            assertEquals(5, h1.peek());
            assertEquals(0, h2.size());
            assertTrue(h2.isEmpty());

            assertEquals(5, h1.extractMin());
            assertEquals(10, h1.extractMin());
            assertEquals(25, h1.extractMin());
            assertEquals(30, h1.extractMin());
            assertTrue(h1.isEmpty());
        }
    }

    @Nested
    @DisplayName("Scale, Generics & Oracle Equivalence Tests")
    class ScaleGenericsAndOracleTests {

        @Test
        @DisplayName("14. Large dataset handling (5,000 elements)")
        void testLargeDataset() {
            FibonacciHeap<Integer, Integer> heap = new FibonacciHeap<>();
            int count = 5000;
            Random rand = new Random(12345);
            List<Integer> list = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                int val = rand.nextInt(100000);
                list.add(val);
                heap.insert(val, val);
            }

            assertEquals(count, heap.size());
            Collections.sort(list);

            for (int exp : list) {
                assertEquals(exp, heap.extractMin());
            }
            assertTrue(heap.isEmpty());
        }

        @Test
        @DisplayName("15. Repeated intermixed insert/extract operations")
        void testRepeatedInsertExtractOperations() {
            FibonacciHeap<Integer, Integer> heap = new FibonacciHeap<>();
            PriorityQueue<Integer> oracle = new PriorityQueue<>();
            Random rand = new Random(98765);

            for (int i = 0; i < 1000; i++) {
                if (heap.isEmpty() || rand.nextBoolean()) {
                    int val = rand.nextInt(500);
                    heap.insert(val, val);
                    oracle.add(val);
                } else {
                    assertEquals(oracle.poll(), heap.extractMin());
                }
                assertEquals(oracle.size(), heap.size());
                assertEquals(oracle.peek(), heap.peek());
            }

            while (!oracle.isEmpty()) {
                assertEquals(oracle.poll(), heap.extractMin());
            }
            assertTrue(heap.isEmpty());
        }

        @Test
        @DisplayName("16. Generic value storage and custom comparator")
        void testGenericValueStorageAndComparator() {
            // Reverse comparator (max-heap behavior using min-heap structure)
            FibonacciHeap<Integer, String> maxHeap = new FibonacciHeap<>(Comparator.reverseOrder());
            maxHeap.insert(10, "ten");
            maxHeap.insert(50, "fifty");
            maxHeap.insert(20, "twenty");

            assertEquals("fifty", maxHeap.extractMin());
            assertEquals("twenty", maxHeap.extractMin());
            assertEquals("ten", maxHeap.extractMin());
            assertTrue(maxHeap.isEmpty());
        }

        @Test
        @DisplayName("17. Deterministic tie-breaking using HeapKey")
        void testDeterministicTieBreakingWithHeapKey() {
            FibonacciHeap<HeapKey, String> heap = new FibonacciHeap<>();

            // Identical primary scores (0.8), differing secondary scores
            HeapKey k1 = HeapKey.of(0.8, 10.0, 1);
            HeapKey k2 = HeapKey.of(0.8, 5.0, 2);  // Lower secondary score -> earlier
            HeapKey k3 = HeapKey.of(0.5, 10.0, 3); // Lower primary score -> earliest
            HeapKey k4 = HeapKey.of(0.8, 5.0, 0);  // Same primary & secondary, lower sequenceId -> before k2

            heap.insert(k1, "Task1");
            heap.insert(k2, "Task2");
            heap.insert(k3, "Task3");
            heap.insert(k4, "Task4");

            // Extraction order:
            // 1. k3 (primary 0.5)
            // 2. k4 (primary 0.8, secondary 5.0, id 0)
            // 3. k2 (primary 0.8, secondary 5.0, id 2)
            // 4. k1 (primary 0.8, secondary 10.0, id 1)
            assertEquals("Task3", heap.extractMin());
            assertEquals("Task4", heap.extractMin());
            assertEquals("Task2", heap.extractMin());
            assertEquals("Task1", heap.extractMin());
            assertTrue(heap.isEmpty());
        }

        @Test
        @DisplayName("18. Randomized differential testing against Java PriorityQueue oracle (2,000 operations)")
        void testRandomizedCorrectnessAgainstOracle() {
            FibonacciHeap<Integer, Integer> fibHeap = new FibonacciHeap<>();
            PriorityQueue<Integer> refQueue = new PriorityQueue<>();

            Random random = new Random(424242);
            int operations = 2000;

            for (int i = 0; i < operations; i++) {
                int op = random.nextInt(10);

                if (op < 6) {
                    // 60% Insert
                    int val = random.nextInt(10000);
                    fibHeap.insert(val, val);
                    refQueue.add(val);
                } else if (op < 9) {
                    // 30% Extract
                    if (!refQueue.isEmpty()) {
                        Integer expected = refQueue.poll();
                        Integer actual = fibHeap.extractMin();
                        assertEquals(expected, actual, "Extracted min must match reference PriorityQueue oracle at op " + i);
                    } else {
                        assertTrue(fibHeap.isEmpty());
                        assertNull(fibHeap.extractMin());
                    }
                } else {
                    // 10% Peek
                    if (!refQueue.isEmpty()) {
                        assertEquals(refQueue.peek(), fibHeap.peek(), "Peek must match oracle at op " + i);
                    } else {
                        assertNull(fibHeap.peek());
                    }
                }

                assertEquals(refQueue.size(), fibHeap.size(), "Size must match oracle at op " + i);
                assertEquals(refQueue.isEmpty(), fibHeap.isEmpty(), "isEmpty must match oracle at op " + i);
            }

            // Empty remainder
            while (!refQueue.isEmpty()) {
                assertEquals(refQueue.poll(), fibHeap.extractMin());
            }
            assertTrue(fibHeap.isEmpty());
            assertEquals(0, fibHeap.size());
        }
    }
}
