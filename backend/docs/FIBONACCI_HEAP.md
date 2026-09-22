# Standalone Fibonacci Heap Specification & Design

## 1. Architectural Role & Motivation

In priority-based task scheduling for cloud computing, task dispatching requires frequent priority extraction, dynamic insertion, and priority adjustments.

### Why a Fibonacci Heap is Required:
1. **Faithful Reproduction of the IEEE Access 2023 Baseline:** The reference research paper (*Lipsa et al., IEEE Access 2023*) explicitly specifies that queued tasks ordered by the Priority Assignment to Tasks (PAT) algorithm are stored in and managed by a **Fibonacci Heap**.
2. **Theoretical Optimality:** Compared to a standard binary heap or pairing heap, a Fibonacci Heap offers superior amortized time bounds:
   - **Insertion:** $\mathcal{O}(1)$ amortized
   - **Decrease-Key:** $\mathcal{O}(1)$ amortized (vs. $\mathcal{O}(\log n)$ in standard binary heaps)
   - **Extract-Min:** $\mathcal{O}(\log n)$ amortized
   - **Union / Merge:** $\mathcal{O}(1)$ amortized
3. **Common Shared Data Structure:** Both the baseline PAT scheduler and the proposed deadline-aware scheduler utilize this exact same heap implementation, ensuring experimental fairness and decoupling the priority queue implementation from scheduling domain logic.

---

## 2. Ordering Convention & Design

### 2.1 Min-Oriented Heap
The heap is implemented as a **Min-Oriented Fibonacci Heap** (`FibonacciHeap<K, V>`).
- The root pointer (`min`) maintains the node with the minimum key according to natural ordering (`Comparable<K>`) or a supplied `Comparator<? super K>`.
- The item with the smallest key is extracted first via `extractMin()`.

### 2.2 Compatibility with Max-Priority Scheduling
When scheduling requires highest priority scores to be dispatched first:
1. **Custom Reverse Comparator:** Instantiating `new FibonacciHeap<>(Comparator.reverseOrder())` or `(k1, k2) -> Double.compare(k2, k1)` extracts the highest numeric score first.
2. **Deterministic Composite Ordering via `HeapKey`:**
   - Encapsulates `primaryScore` (e.g. priority or deadline), `secondaryScore` (e.g. waiting time), and `sequenceId` (e.g. task ID).
   - Guarantees strict, deterministic total ordering without arbitrary tie-breaking.

---

## 3. Supported Operations & Asymptotic Time Complexity

| Operation | Method Signature | Time Complexity (Amortized) | Description |
|---|---|---|---|
| **Insert** | `insert(K key, V value)` | $\mathcal{O}(1)$ | Adds a new node to the root list; updates `min` if smaller. |
| **Find Min (Peek)** | `peek()` / `peekNode()` | $\mathcal{O}(1)$ worst-case | Returns minimum payload value / node without removal. |
| **Extract Min** | `extractMin()` / `extractMinNode()` | $\mathcal{O}(\log n)$ | Removes minimum node, promotes children to root list, and consolidates equal-degree trees. |
| **Decrease Key** | `decreaseKey(node, newKey)` | $\mathcal{O}(1)$ | Reduces node key; cuts from parent and triggers cascading cuts if heap property violated. |
| **Union** | `union(otherHeap)` | $\mathcal{O}(1)$ | Splices two circular doubly-linked root lists in constant time. |
| **Size / IsEmpty** | `size()`, `isEmpty()` | $\mathcal{O}(1)$ worst-case | Tracks element count accurately across operations. |
| **Clear** | `clear()` | $\mathcal{O}(1)$ | Resets root list and size. |

---

## 4. Internal Structural Mechanics

1. **Circular Doubly-Linked Lists:**
   - Both the root list and child lists are circular doubly-linked lists with `left` and `right` pointers.
   - Splicing a list of children into the root list occurs in $\mathcal{O}(1)$ pointer operations.
2. **Consolidation:**
   - Triggered during `extractMin()`.
   - Links trees of identical degree ($d$) such that at most one tree of each degree exists in the root list.
   - Bounded by maximum degree $D(n) \le \lfloor \log_\phi(n) \rfloor + 2 \le 45$ for $n \le 2 \times 10^9$.
3. **Cascading Cuts:**
   - When `decreaseKey` violates the heap property with a parent node, the modified node is cut to the root list.
   - If the parent was already marked (`mark == true`, meaning it previously lost another child), a cascading cut recurses upward to preserve the logarithmic tree depth guarantee.

---

## 5. Complete Decoupling & Independence

- **Zero CloudSim Dependencies:** No imports or references to `org.cloudsimplus.*`.
- **Zero Model / Domain Dependencies:** Completely generic (`<K, V>`). Contains no imports or references to `Task`, `TaskStatus`, or scheduling algorithm classes.
- **Dependency Hierarchy:**
  $$\text{Heap \& Model Layer} \longrightarrow \text{Schedulers (Baseline / Deadline)} \longrightarrow \text{CloudSim Adapter}$$

---

## 6. Testing & Oracle Validation Strategy

The test suite in `src/test/java/scheduler/heap/FibonacciHeapTest.java` verifies:
1. **Basic Heap Contracts:** Empty heap, single insertion, multiple insertions, non-destructive peek, correct ascending extraction.
2. **Boundary & Edge Conditions:** Duplicate keys, negative/zero/positive floating-point keys, size tracking, empty extraction handling.
3. **Structural Correctness:** Decrease-key min updating, cascading cut execution on multi-level trees, and union merging.
4. **Scale & Performance:** 5,000-element sorted extraction.
5. **Deterministic Ordering:** Composite multi-factor tie-breaking with `HeapKey`.
6. **Differential Testing Against Reference Oracle:**
   - 2,000 pseudo-random intermixed operations (`insert`, `extractMin`, `peek`, `size`) executed in parallel against standard `java.util.PriorityQueue`.
   - Verified that every output from `FibonacciHeap` strictly matches the reference oracle.
