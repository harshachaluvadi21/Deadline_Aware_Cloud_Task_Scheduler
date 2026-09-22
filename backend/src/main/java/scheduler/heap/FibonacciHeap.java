package scheduler.heap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Generic, standalone Min-Oriented Fibonacci Heap implementation.
 *
 * <p>Ordering Convention:
 * <b>Min-Heap:</b> The root tracks the node with the minimum key according to natural ordering
 * or a supplied {@link Comparator}. The element with the smallest key is extracted first.
 *
 * <p>Asymptotic Time Complexity:
 * <ul>
 *   <li><b>Insert:</b> $O(1)$ amortized</li>
 *   <li><b>Find Min (Peek):</b> $O(1)$ worst-case</li>
 *   <li><b>Union:</b> $O(1)$ amortized</li>
 *   <li><b>Extract Min:</b> $O(\log n)$ amortized</li>
 *   <li><b>Decrease Key:</b> $O(1)$ amortized</li>
 *   <li><b>Delete:</b> $O(\log n)$ amortized</li>
 * </ul>
 *
 * <p>Completely decoupled from CloudSim Plus, Task models, and scheduling algorithm logic.
 * Both the IEEE Access 2023 baseline scheduler and the proposed deadline-aware scheduler
 * utilize this exact same data structure.
 *
 * @param <K> the key type used for ordering
 * @param <V> the payload value type associated with keys
 */
public class FibonacciHeap<K, V> {

    private final Comparator<? super K> comparator;
    private FibonacciNode<K, V> min;
    private int size;

    /**
     * Constructs an empty Fibonacci Heap using the natural ordering of its keys.
     */
    public FibonacciHeap() {
        this(null);
    }

    /**
     * Constructs an empty Fibonacci Heap using the specified comparator.
     *
     * @param comparator custom comparator, or null for natural ordering
     */
    public FibonacciHeap(Comparator<? super K> comparator) {
        this.comparator = comparator;
        this.min = null;
        this.size = 0;
    }

    /**
     * Compares two keys using the configured comparator or natural Comparable order.
     */
    @SuppressWarnings("unchecked")
    private int compare(K k1, K k2) {
        if (comparator != null) {
            return comparator.compare(k1, k2);
        }
        return ((Comparable<? super K>) k1).compareTo(k2);
    }

    /**
     * Inserts a new (key, value) pair into the heap.
     * Runs in $O(1)$ amortized time.
     *
     * @param key   the priority/ordering key (must not be null)
     * @param value the associated payload value
     * @return the created node, which can be retained for $O(1)$ {@link #decreaseKey} operations
     */
    public FibonacciNode<K, V> insert(K key, V value) {
        FibonacciNode<K, V> node = new FibonacciNode<>(key, value);

        if (min == null) {
            min = node;
        } else {
            // Splice node into the root list next to min
            node.left = min;
            node.right = min.right;
            min.right.left = node;
            min.right = node;

            if (compare(node.key, min.key) < 0) {
                min = node;
            }
        }

        size++;
        return node;
    }

    /**
     * Retrieves, but does not remove, the value associated with the minimum key.
     * Runs in $O(1)$ time.
     *
     * @return the payload value of the minimum node, or null if the heap is empty
     */
    public V peek() {
        return min == null ? null : min.value;
    }

    /**
     * Retrieves, but does not remove, the minimum node in the heap.
     * Runs in $O(1)$ time.
     *
     * @return the minimum node, or null if empty
     */
    public FibonacciNode<K, V> peekNode() {
        return min;
    }

    /**
     * Removes and returns the value associated with the minimum key.
     * Consolidates root trees to maintain Fibonacci tree properties.
     * Runs in $O(\log n)$ amortized time.
     *
     * @return the value of the extracted minimum node, or null if the heap is empty
     */
    public V extractMin() {
        FibonacciNode<K, V> extracted = extractMinNode();
        return extracted == null ? null : extracted.value;
    }

    /**
     * Removes and returns the minimum node from the heap.
     * Runs in $O(\log n)$ amortized time.
     *
     * @return the extracted minimum node, or null if empty
     */
    public FibonacciNode<K, V> extractMinNode() {
        FibonacciNode<K, V> z = min;
        if (z == null) {
            return null;
        }

        // 1. Promote all children of z to the root list
        if (z.child != null) {
            List<FibonacciNode<K, V>> children = new ArrayList<>(z.degree);
            FibonacciNode<K, V> current = z.child;
            do {
                children.add(current);
                current = current.right;
            } while (current != z.child);

            for (FibonacciNode<K, V> child : children) {
                child.parent = null;

                // Splice child into the root list
                child.left = min;
                child.right = min.right;
                min.right.left = child;
                min.right = child;
            }
            z.child = null;
        }

        // 2. Remove z from the root list
        z.left.right = z.right;
        z.right.left = z.left;

        if (z == z.right) {
            min = null;
        } else {
            min = z.right;
            consolidate();
        }

        size--;
        return z;
    }

    /**
     * Consolidates the root list by linking trees of equal degree until at most
     * one tree of each degree remains.
     */
    @SuppressWarnings("unchecked")
    private void consolidate() {
        // Upper bound on max degree: D(n) <= floor(log_phi(n)) + 2.
        // For n up to 2*10^9, degree is <= 45. 64 is safe.
        int maxDegree = 64;
        if (size > 1) {
            int calculated = (int) Math.floor(Math.log(size) / Math.log((1.0 + Math.sqrt(5.0)) / 2.0)) + 2;
            maxDegree = Math.max(maxDegree, calculated + 5);
        }

        FibonacciNode<K, V>[] array = new FibonacciNode[maxDegree];

        // Collect all existing roots
        List<FibonacciNode<K, V>> rootList = new ArrayList<>();
        FibonacciNode<K, V> current = min;
        do {
            rootList.add(current);
            current = current.right;
        } while (current != min);

        // Combine trees of equal degree
        for (FibonacciNode<K, V> w : rootList) {
            FibonacciNode<K, V> x = w;
            int d = x.degree;

            while (array[d] != null) {
                FibonacciNode<K, V> y = array[d];
                if (compare(y.key, x.key) < 0) {
                    FibonacciNode<K, V> temp = x;
                    x = y;
                    y = temp;
                }
                link(y, x);
                array[d] = null;
                d++;
            }
            array[d] = x;
        }

        // Reconstruct the root list and locate new min
        min = null;
        for (FibonacciNode<K, V> y : array) {
            if (y != null) {
                if (min == null) {
                    min = y;
                    y.left = y;
                    y.right = y;
                } else {
                    y.left = min;
                    y.right = min.right;
                    min.right.left = y;
                    min.right = y;
                    if (compare(y.key, min.key) < 0) {
                        min = y;
                    }
                }
            }
        }
    }

    /**
     * Makes node y a child of node x.
     */
    private void link(FibonacciNode<K, V> y, FibonacciNode<K, V> x) {
        // Remove y from the root list
        y.left.right = y.right;
        y.right.left = y.left;

        // Make y a child of x
        y.parent = x;
        if (x.child == null) {
            x.child = y;
            y.left = y;
            y.right = y;
        } else {
            y.left = x.child;
            y.right = x.child.right;
            x.child.right.left = y;
            x.child.right = y;
        }

        x.degree++;
        y.mark = false;
    }

    /**
     * Decreases the key of a given node to a smaller key.
     * Runs in $O(1)$ amortized time.
     *
     * @param node   the target node
     * @param newKey the new key value (must compare &lt;= current key)
     * @throws NullPointerException     if node or newKey is null
     * @throws IllegalArgumentException if newKey &gt; current key
     */
    public void decreaseKey(FibonacciNode<K, V> node, K newKey) {
        Objects.requireNonNull(node, "Node cannot be null");
        Objects.requireNonNull(newKey, "New key cannot be null");

        if (compare(newKey, node.key) > 0) {
            throw new IllegalArgumentException(String.format(
                "New key (%s) cannot be greater than current key (%s) in min-heap", newKey, node.key
            ));
        }

        node.key = newKey;
        FibonacciNode<K, V> parent = node.parent;

        if (parent != null && compare(node.key, parent.key) < 0) {
            cut(node, parent);
            cascadingCut(parent);
        }

        if (compare(node.key, min.key) < 0) {
            min = node;
        }
    }

    /**
     * Cuts the link between node x and its parent y, moving x to the root list.
     */
    private void cut(FibonacciNode<K, V> x, FibonacciNode<K, V> y) {
        // Remove x from child list of y
        x.left.right = x.right;
        x.right.left = x.left;

        if (y.child == x) {
            y.child = (x.right == x) ? null : x.right;
        }
        y.degree--;

        // Splice x into the root list
        x.left = min;
        x.right = min.right;
        min.right.left = x;
        min.right = x;

        x.parent = null;
        x.mark = false;
    }

    /**
     * Cascading cut: recursively cuts marked parents to the root list.
     */
    private void cascadingCut(FibonacciNode<K, V> y) {
        FibonacciNode<K, V> z = y.parent;
        if (z != null) {
            if (!y.mark) {
                y.mark = true;
            } else {
                cut(y, z);
                cascadingCut(z);
            }
        }
    }

    /**
     * Unites this heap with another heap in $O(1)$ amortized time.
     * The other heap is emptied following the union.
     *
     * @param other the other Fibonacci Heap to merge into this one
     */
    public void union(FibonacciHeap<K, V> other) {
        if (other == null || other.min == null) {
            return;
        }

        if (this.min == null) {
            this.min = other.min;
            this.size = other.size;
        } else {
            // Splice the two root lists together
            FibonacciNode<K, V> thisRight = this.min.right;
            FibonacciNode<K, V> otherLeft = other.min.left;

            this.min.right = other.min;
            other.min.left = this.min;

            thisRight.left = otherLeft;
            otherLeft.right = thisRight;

            if (compare(other.min.key, this.min.key) < 0) {
                this.min = other.min;
            }
            this.size += other.size;
        }

        other.min = null;
        other.size = 0;
    }

    /**
     * Removes all elements from the heap.
     */
    public void clear() {
        this.min = null;
        this.size = 0;
    }

    /**
     * Returns the total number of elements in the heap.
     *
     * @return current heap size
     */
    public int size() {
        return size;
    }

    /**
     * Checks if the heap contains no elements.
     *
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        return size == 0;
    }
}
