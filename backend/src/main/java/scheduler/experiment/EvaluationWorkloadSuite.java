package scheduler.experiment;

import scheduler.model.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Deterministic Workload Evaluation Suite providing reproducible workloads
 * across varied operational regimes:
 *
 * 1. Benchmark (10 tasks) - Standard balanced reference
 * 2. Representative Cloud (20 tasks) - Realistic multi-type cloud workload
 * 3. Representative Cloud (50 tasks) - Medium-scale cluster workload
 * 4. Representative Cloud (100 tasks) - Large-scale cluster workload
 * 5. Deadline-Intensive (30 tasks) - Competing tight deadlines & urgency inversions
 * 6. Preemption Stress Test (25 tasks) - Burst-inversion patterns with long tasks and arriving short urgent jobs
 * 7. Mixed Cloud (40 tasks) - Balanced mixture of short, medium, and heavy cloud jobs
 * 8. Relaxed-Deadline Control (30 tasks) - Control workload with generous deadlines
 */
public class EvaluationWorkloadSuite {

    public static final String[] CLOUD_TASK_TYPES = {
        "Image Processing",
        "Video Transcoding",
        "Database Query",
        "ML Model Inference",
        "Log Analysis",
        "File Compression",
        "Data Analytics",
        "Backup Processing",
        "Web Request Processing",
        "ETL/Data Pipeline",
        "Report Generation",
        "Document Processing"
    };

    public record WorkloadDefinition(
        String id,
        String name,
        String category,
        String description,
        List<Task> tasks,
        List<String> taskTypes
    ) {}

    // =========================================================================
    // 1. Existing 10-Task Representative Benchmark
    // =========================================================================
    public static WorkloadDefinition getWorkload10Benchmark() {
        List<Task> tasks = List.of(
            new Task(0, 5, 0.0, 15.65, 47.31),  // T0: Image Processing
            new Task(1, 8, 1.0, 8.0, 12.0),     // T1: Video Transcoding
            new Task(2, 3, 2.5, 14.0, 32.0),    // T2: Database Query
            new Task(3, 9, 3.0, 6.5, 11.0),     // T3: ML Model Inference
            new Task(4, 4, 4.5, 18.0, 55.0),    // T4: Log Analysis
            new Task(5, 7, 6.0, 10.0, 20.0),    // T5: File Compression
            new Task(6, 2, 7.5, 12.0, 45.0),    // T6: Data Analytics
            new Task(7, 6, 8.0, 9.5, 22.0),     // T7: Backup Processing
            new Task(8, 10, 10.0, 5.0, 16.0),   // T8: Web Request Processing
            new Task(9, 4, 12.0, 16.0, 35.0)    // T9: ETL/Data Pipeline
        );
        List<String> types = List.of(
            "Image Processing", "Video Transcoding", "Database Query", "ML Model Inference",
            "Log Analysis", "File Compression", "Data Analytics", "Backup Processing",
            "Web Request Processing", "ETL/Data Pipeline"
        );
        return new WorkloadDefinition(
            "benchmark-10",
            "Existing 10-Task Representative Workload",
            "Benchmark",
            "Balanced distribution of priority, arrival times, and execution requirements for baseline comparison.",
            tasks, types
        );
    }

    // =========================================================================
    // 2. Representative Cloud Workload — 20 Tasks
    // =========================================================================
    public static WorkloadDefinition getWorkload20Representative() {
        List<Task> tasks = new ArrayList<>(20);
        List<String> types = new ArrayList<>(20);

        long[] ids = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};
        int[] priorities = {4, 7, 3, 9, 5, 2, 8, 6, 10, 3, 5, 8, 4, 6, 2, 9, 7, 3, 5, 1};
        double[] arrivals = {0.0, 1.2, 2.5, 3.8, 5.0, 6.4, 7.8, 9.0, 10.5, 12.0, 13.5, 15.0, 16.8, 18.2, 20.0, 21.5, 23.0, 25.0, 27.0, 29.5};
        double[] bursts = {10.0, 6.5, 14.0, 4.0, 12.0, 8.0, 5.5, 16.0, 3.0, 18.0, 7.5, 9.0, 15.0, 6.0, 22.0, 4.5, 11.0, 13.0, 8.5, 20.0};
        double[] deadlines = {28.0, 14.0, 38.0, 9.5, 32.0, 26.0, 15.0, 42.0, 15.0, 48.0, 26.0, 26.0, 45.0, 27.0, 65.0, 32.0, 36.0, 48.0, 39.0, 70.0};
        String[] taskTypeMapping = {
            "Image Processing", "ML Model Inference", "Video Transcoding", "Web Request Processing",
            "Data Analytics", "Document Processing", "Database Query", "ETL/Data Pipeline",
            "Web Request Processing", "Backup Processing", "File Compression", "ML Model Inference",
            "Video Transcoding", "Log Analysis", "Backup Processing", "Database Query",
            "Report Generation", "Data Analytics", "Image Processing", "Backup Processing"
        };

        for (int i = 0; i < 20; i++) {
            tasks.add(new Task(ids[i], priorities[i], arrivals[i], bursts[i], deadlines[i]));
            types.add(taskTypeMapping[i]);
        }

        return new WorkloadDefinition(
            "representative-20",
            "Representative Cloud Workload — 20 Tasks",
            "Scale-Up",
            "Deterministic 20-task workload featuring diverse cloud types (Web requests, ML inference, Database queries, Video transcoding) with varied arrival and burst profiles.",
            Collections.unmodifiableList(tasks), Collections.unmodifiableList(types)
        );
    }

    // =========================================================================
    // 3. Representative Cloud Workload — 50 Tasks
    // =========================================================================
    public static WorkloadDefinition getWorkload50Representative() {
        return generateDeterministicSyntheticWorkload(
            "representative-50",
            "Representative Cloud Workload — 50 Tasks",
            "Scale-Up",
            "Continuous multi-job stream across 50 tasks with realistic burst durations, arrival pacing, and deadline margins.",
            50, 5001L, 0.8, 2.2, 3.0, 22.0, 1.2, 2.6
        );
    }

    // =========================================================================
    // 4. Representative Cloud Workload — 100 Tasks
    // =========================================================================
    public static WorkloadDefinition getWorkload100Representative() {
        return generateDeterministicSyntheticWorkload(
            "representative-100",
            "Representative Cloud Workload — 100 Tasks",
            "Scale-Up",
            "High-throughput enterprise cloud trace simulation containing 100 heterogeneous jobs across 12 realistic workload categories.",
            100, 10001L, 0.5, 1.8, 2.5, 25.0, 1.0, 2.8
        );
    }

    // =========================================================================
    // 5. Deadline-Intensive Workload (30 Tasks)
    // =========================================================================
    public static WorkloadDefinition getWorkloadDeadlineIntensive() {
        List<Task> tasks = new ArrayList<>(30);
        List<String> types = new ArrayList<>(30);

        // Explicitly incorporates tight deadlines and priority inversions (low base priority with critical urgency,
        // vs high base priority with relaxed margins) to evaluate deadline-aware dynamic prioritization.
        Random rng = new Random(3030L);
        double clock = 0.0;

        for (int i = 0; i < 30; i++) {
            if (i > 0) {
                clock += round2(0.5 + rng.nextDouble() * 1.5);
            }
            double arrival = round2(clock);
            int typeIdx = i % CLOUD_TASK_TYPES.length;
            String typeName = CLOUD_TASK_TYPES[typeIdx];

            double burst;
            double deadline;
            int priority;

            if (i % 3 == 0) {
                // Critical deadline with low base priority
                burst = round2(4.0 + rng.nextDouble() * 6.0);
                priority = 2 + rng.nextInt(3); // [2, 4]
                double slack = round2(1.0 + rng.nextDouble() * 3.0); // very tight slack!
                deadline = round2(arrival + burst + slack);
            } else if (i % 3 == 1) {
                // High base priority but relaxed deadline
                burst = round2(8.0 + rng.nextDouble() * 12.0);
                priority = 8 + rng.nextInt(3); // [8, 10]
                double slack = round2(25.0 + rng.nextDouble() * 35.0); // comfortable slack!
                deadline = round2(arrival + burst + slack);
            } else {
                // Moderate task
                burst = round2(5.0 + rng.nextDouble() * 10.0);
                priority = 4 + rng.nextInt(4); // [4, 7]
                double slack = round2(4.0 + rng.nextDouble() * 8.0);
                deadline = round2(arrival + burst + slack);
            }

            tasks.add(new Task(i, priority, arrival, burst, deadline));
            types.add(typeName);
        }

        return new WorkloadDefinition(
            "deadline-intensive",
            "Deadline-Intensive Workload",
            "Urgency Stress",
            "A synthetic workload containing competing tight deadlines designed to evaluate the effectiveness of dynamic deadline urgency.",
            Collections.unmodifiableList(tasks), Collections.unmodifiableList(types)
        );
    }

    // =========================================================================
    // 6. Preemption Stress Test / Burst-Inversion Workload (25 Tasks)
    // =========================================================================
    public static WorkloadDefinition getWorkloadPreemptionStress() {
        List<Task> tasks = new ArrayList<>(25);
        List<String> types = new ArrayList<>(25);

        // Pairs of long background tasks followed closely by short urgent tasks
        // Designed to test whether preemptive scheduling interrupts long tasks to save urgent deadlines.
        long id = 0;
        double baseTime = 0.0;

        for (int pair = 0; pair < 8; pair++) {
            // Long Task (starts earlier, heavy burst, relaxed deadline, moderate priority)
            double longArr = round2(baseTime);
            double longBurst = 16.0 + (pair % 3) * 4.0; // 16, 20, 24
            double longDead = round2(longArr + longBurst + 35.0);
            int longPrio = 3 + (pair % 2); // 3 or 4
            tasks.add(new Task(id, longPrio, longArr, longBurst, longDead));
            types.add(pair % 2 == 0 ? "Video Transcoding" : "Data Analytics");
            id++;

            // Urgent Short Task (arrives shortly after long task starts, short burst, very tight deadline, higher priority)
            double shortArr = round2(longArr + 2.5);
            double shortBurst = 3.5 + (pair % 2) * 1.5; // 3.5 or 5.0
            double shortDead = round2(shortArr + shortBurst + 3.0); // only 3s slack!
            int shortPrio = 7 + (pair % 3); // 7, 8, 9
            tasks.add(new Task(id, shortPrio, shortArr, shortBurst, shortDead));
            types.add(pair % 2 == 0 ? "Web Request Processing" : "Database Query");
            id++;

            baseTime += 7.0; // overlap with preceding long task
        }

        // Add 9 remaining mixed jobs to complete 25 tasks
        Random rng = new Random(9090L);
        for (int k = 0; k < 9; k++) {
            double arr = round2(baseTime + k * 2.0);
            double burst = round2(4.0 + rng.nextDouble() * 8.0);
            double dead = round2(arr + burst + 8.0 + rng.nextDouble() * 12.0);
            int prio = 2 + rng.nextInt(7);
            tasks.add(new Task(id, prio, arr, burst, dead));
            types.add(CLOUD_TASK_TYPES[k % CLOUD_TASK_TYPES.length]);
            id++;
        }

        return new WorkloadDefinition(
            "preemption-stress",
            "Preemption Stress Test",
            "Preemption",
            "A workload containing long-running tasks followed by short urgent tasks, designed to evaluate whether preemptive deadline-aware scheduling can reduce deadline misses.",
            Collections.unmodifiableList(tasks), Collections.unmodifiableList(types)
        );
    }

    // =========================================================================
    // 7. Mixed Cloud Workload (40 Tasks)
    // =========================================================================
    public static WorkloadDefinition getWorkloadMixedCloud() {
        List<Task> tasks = new ArrayList<>(40);
        List<String> types = new ArrayList<>(40);

        Random rng = new Random(4040L);
        double clock = 0.0;

        for (int i = 0; i < 40; i++) {
            if (i > 0) {
                clock += round2(0.6 + rng.nextDouble() * 1.8);
            }
            double arrival = round2(clock);
            int category = i % 3; // 0 = short query/web, 1 = medium ML/image, 2 = heavy batch/analytics

            double burst;
            double deadline;
            int priority;
            String typeName;

            if (category == 0) {
                // Short interactive job
                burst = round2(2.0 + rng.nextDouble() * 3.5);
                priority = 6 + rng.nextInt(5); // [6, 10]
                double slack = round2(3.0 + rng.nextDouble() * 6.0);
                deadline = round2(arrival + burst + slack);
                typeName = (i % 2 == 0) ? "Database Query" : "Web Request Processing";
            } else if (category == 1) {
                // Medium computing job
                burst = round2(7.0 + rng.nextDouble() * 6.0);
                priority = 3 + rng.nextInt(6); // [3, 8]
                double slack = round2(8.0 + rng.nextDouble() * 14.0);
                deadline = round2(arrival + burst + slack);
                typeName = (i % 2 == 0) ? "ML Model Inference" : "Image Processing";
            } else {
                // Heavy batch job
                burst = round2(15.0 + rng.nextDouble() * 12.0);
                priority = 1 + rng.nextInt(5); // [1, 5]
                double slack = round2(20.0 + rng.nextDouble() * 30.0);
                deadline = round2(arrival + burst + slack);
                typeName = (i % 2 == 0) ? "Video Transcoding" : "Data Analytics";
            }

            tasks.add(new Task(i, priority, arrival, burst, deadline));
            types.add(typeName);
        }

        return new WorkloadDefinition(
            "mixed-cloud",
            "Mixed Cloud Workload",
            "General",
            "Balanced enterprise mixture of short interactive queries (2-5s), medium compute tasks (7-13s), and long batch jobs (15-27s) with realistic arrival patterns.",
            Collections.unmodifiableList(tasks), Collections.unmodifiableList(types)
        );
    }

    // =========================================================================
    // 8. Relaxed-Deadline Control Workload (30 Tasks)
    // =========================================================================
    public static WorkloadDefinition getWorkloadRelaxedControl() {
        List<Task> tasks = new ArrayList<>(30);
        List<String> types = new ArrayList<>(30);

        Random rng = new Random(8080L);
        double clock = 0.0;

        for (int i = 0; i < 30; i++) {
            if (i > 0) {
                clock += round2(0.8 + rng.nextDouble() * 2.0);
            }
            double arrival = round2(clock);
            double burst = round2(5.0 + rng.nextDouble() * 15.0);
            int priority = 1 + rng.nextInt(10);
            // Generous deadline slack multiplier: 3.5 to 6.0x burst duration
            double slack = round2(burst * (3.5 + rng.nextDouble() * 2.5));
            double deadline = round2(arrival + burst + slack);

            tasks.add(new Task(i, priority, arrival, burst, deadline));
            types.add(CLOUD_TASK_TYPES[i % CLOUD_TASK_TYPES.length]);
        }

        return new WorkloadDefinition(
            "relaxed-control",
            "Relaxed Deadline Control Workload",
            "Control",
            "Control workload where all deadlines are sufficiently loose. Evaluates baseline vs proposed scheduler overhead when deadline urgency is not a constraining factor.",
            Collections.unmodifiableList(tasks), Collections.unmodifiableList(types)
        );
    }

    public static List<WorkloadDefinition> getAllWorkloads() {
        return List.of(
            getWorkload10Benchmark(),
            getWorkload20Representative(),
            getWorkload50Representative(),
            getWorkload100Representative(),
            getWorkloadDeadlineIntensive(),
            getWorkloadPreemptionStress(),
            getWorkloadMixedCloud(),
            getWorkloadRelaxedControl()
        );
    }

    private static WorkloadDefinition generateDeterministicSyntheticWorkload(
            String id, String name, String category, String description,
            int taskCount, long seed,
            double minArr, double maxArr,
            double minExec, double maxExec,
            double minSlackMult, double maxSlackMult) {

        Random rng = new Random(seed);
        List<Task> tasks = new ArrayList<>(taskCount);
        List<String> types = new ArrayList<>(taskCount);

        double clock = 0.0;
        for (int i = 0; i < taskCount; i++) {
            if (i > 0) {
                clock += round2(minArr + rng.nextDouble() * (maxArr - minArr));
            }
            double arrival = round2(clock);
            double burst = round2(minExec + rng.nextDouble() * (maxExec - minExec));
            int priority = 1 + rng.nextInt(10);
            double slackMult = minSlackMult + rng.nextDouble() * (maxSlackMult - minSlackMult);
            double slack = Math.max(1.0, round2(burst * slackMult));
            double deadline = round2(arrival + burst + slack);

            tasks.add(new Task(i, priority, arrival, burst, deadline));
            types.add(CLOUD_TASK_TYPES[i % CLOUD_TASK_TYPES.length]);
        }

        return new WorkloadDefinition(id, name, category, description,
                Collections.unmodifiableList(tasks), Collections.unmodifiableList(types));
    }

    private static double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
