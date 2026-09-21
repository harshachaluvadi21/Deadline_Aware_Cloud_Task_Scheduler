# Deadline-Aware Priority-Based Task Scheduling in Cloud Computing

An academic research mini-project extending the priority-based heuristic task scheduling approach from the IEEE Access 2023 paper (*"Task Scheduling in Cloud Computing: A Priority-Based Heuristic Approach"* by S. Lipsa et al.) through deadline-aware heuristic enhancements using CloudSim Plus.

---

## 1. Project Objectives
- **Baseline System**: Faithfully reproduces the IEEE Access 2023 priority scheduling approach using the Priority Assignment to Tasks (PAT) algorithm, Waiting Time Matrix (WTM), and a standalone Fibonacci Heap.
- **Proposed System**: Enhances the scheduling decision process by introducing deadline awareness considering:
  1. Base Priority
  2. Deadline
  3. Arrival Time
  4. Execution Time
  5. Waiting Time
  6. VM Workload / Resource Availability
- **Research Question**: Can deadline-aware heuristic scheduling reduce deadline misses in cloud environments while preserving balanced resource utilization, waiting time, turnaround time, and throughput?

---

## 2. Technology Stack
- **Language**: Java (Targeting JDK 21 LTS)
- **Build Tool**: Apache Maven
- **Simulation Toolkit**: CloudSim Plus `8.0.0`
- **Data Structure**: Standalone Fibonacci Heap (`FibonacciHeap<T>`)
- **Testing**: JUnit Jupiter 5

---

## 3. Directory Layout
```
├── pom.xml                   # Maven project descriptor
├── README.md                 # Project guide
├── PROJECT_PLAN.md           # Phased execution plan
├── ARCHITECTURE.md           # System architecture & component design
├── DESIGN_DECISIONS.md       # Documented design choices & ambiguities
├── docs/                     # Research notes & documentation
├── data/                     # Controlled experimental benchmark datasets
├── src/
│   ├── main/java/scheduler/  # Core system source packages
│   └── test/java/scheduler/  # Automated unit & integration tests
└── results/                  # Authentic simulation logs and CSV metrics
```

---

## 4. Building the Project
```bash
# Compile code
mvn clean compile

# Run tests
mvn test
```
