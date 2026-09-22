package scheduler.experiment;

import scheduler.model.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Deterministic synthetic workload generator for controlled scheduling experiments.
 *
 * <p><b>Fairness &amp; Reproducibility Guarantees:</b>
 * <ul>
 *   <li>Uses {@link java.util.Random} initialized with a deterministic seed for each scenario.</li>
 *   <li>Generates the identical task stream once, then clones it for both schedulers.</li>
 *   <li>Task deadlines are established strictly using scenario-level slack factors:
 *       \[
 *         \text{deadline} = \text{arrivalTime} + \text{executionTime} + (\text{executionTime} \times \text{slackMultiplier})
 *       \]
 *       This formulation is completely independent of the proposed scheduler's urgency formula
 *       to prevent circular experimental bias.</li>
 * </ul>
 */
public class WorkloadGenerator {

    /**
     * Generates a deterministic workload for the specified scenario, task count, and seed.
     *
     * @param scenario  workload scenario defining parameter distributions
     * @param taskCount number of tasks to generate
     * @param seed      pseudorandom seed
     * @return unmodifiable list of newly created, unmutated tasks
     */
    public static List<Task> generateWorkload(WorkloadScenarioType scenario, int taskCount, long seed) {
        Objects.requireNonNull(scenario, "Scenario cannot be null");
        if (taskCount <= 0) {
            throw new IllegalArgumentException("Task count must be positive: " + taskCount);
        }

        Random rand = new Random(seed);
        List<Task> tasks = new ArrayList<>(taskCount);

        double currentArrival = 0.0;
        double minArr = scenario.getMinArrivalInterval();
        double maxArr = scenario.getMaxArrivalInterval();
        double minExec = scenario.getMinExecutionTime();
        double maxExec = scenario.getMaxExecutionTime();
        double minSlack = scenario.getMinSlackMultiplier();
        double maxSlack = scenario.getMaxSlackMultiplier();

        for (int i = 0; i < taskCount; i++) {
            if (i > 0) {
                double interval = minArr + (maxArr - minArr) * rand.nextDouble();
                currentArrival += round2(interval);
            }
            double arrivalTime = round2(currentArrival);

            double rawExec = minExec + (maxExec - minExec) * rand.nextDouble();
            double executionTime = Math.max(0.1, round2(rawExec));

            int priority = 1 + rand.nextInt(10); // [1, 10]

            double rawSlackMult = minSlack + (maxSlack - minSlack) * rand.nextDouble();
            double slack = Math.max(0.1, round2(executionTime * rawSlackMult));
            double deadline = round2(arrivalTime + executionTime + slack);

            tasks.add(new Task(i, priority, arrivalTime, executionTime, deadline));
        }

        return Collections.unmodifiableList(tasks);
    }

    /**
     * Generates a workload directly from an {@link ExperimentConfig}.
     *
     * @param config the experiment configuration
     * @return unmodifiable list of tasks
     */
    public static List<Task> generateWorkload(ExperimentConfig config) {
        Objects.requireNonNull(config, "Config cannot be null");
        return generateWorkload(config.scenarioType(), config.taskCount(), config.seed());
    }

    /**
     * Creates an independent deep clone of the given task list.
     *
     * <p>All returned tasks are in initial {@link scheduler.model.TaskStatus#SUBMITTED} state
     * with unassigned VMs and zeroed runtime timestamps, preventing mutable state leakage
     * between different scheduler executions.
     *
     * @param tasks original list of tasks
     * @return unmodifiable list of fresh task clones
     */
    public static List<Task> cloneWorkload(List<Task> tasks) {
        if (tasks == null) {
            return Collections.emptyList();
        }
        List<Task> clones = new ArrayList<>(tasks.size());
        for (Task t : tasks) {
            clones.add(new Task(
                    t.getTaskId(),
                    t.getPriority(),
                    t.getArrivalTime(),
                    t.getExecutionTime(),
                    t.getDeadline()
            ));
        }
        return Collections.unmodifiableList(clones);
    }

    private static double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
