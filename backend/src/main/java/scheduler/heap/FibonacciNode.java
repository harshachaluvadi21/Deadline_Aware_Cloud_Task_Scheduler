package scheduler.heap;

import java.util.Objects;

/**
 * Node structure for the generic {@link FibonacciHeap}.
 *
 * Maintains pointers in a circular, doubly-linked list for siblings,
 * parent reference, child list reference, node degree (number of children),
 * and the mark bit utilized for cascading cuts during decrease-key operations.
 *
 * @param <K> the type of keys maintained by this node
 * @param <V> the type of mapped values
 */
public class FibonacciNode<K, V> {

    K key;
    V value;
    int degree;
    boolean mark;

    FibonacciNode<K, V> parent;
    FibonacciNode<K, V> child;
    FibonacciNode<K, V> left;
    FibonacciNode<K, V> right;

    /**
     * Constructs a new standalone Fibonacci heap node with circular self-pointers.
     *
     * @param key   the search/priority key (must not be null)
     * @param value the associated payload value
     */
    public FibonacciNode(K key, V value) {
        this.key = Objects.requireNonNull(key, "Key must not be null");
        this.value = value;
        this.degree = 0;
        this.mark = false;
        this.parent = null;
        this.child = null;
        this.left = this;
        this.right = this;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public int getDegree() {
        return degree;
    }

    public boolean isMarked() {
        return mark;
    }

    public FibonacciNode<K, V> getParent() {
        return parent;
    }

    public FibonacciNode<K, V> getChild() {
        return child;
    }

    public FibonacciNode<K, V> getLeft() {
        return left;
    }

    public FibonacciNode<K, V> getRight() {
        return right;
    }

    @Override
    public String toString() {
        return "FibonacciNode{key=" + key + ", value=" + value + ", degree=" + degree + ", mark=" + mark + "}";
    }
}
