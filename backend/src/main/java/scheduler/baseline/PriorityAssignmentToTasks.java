package scheduler.baseline;

import scheduler.model.Task;

import java.util.List;

/**
 * Architectural alias for {@link PriorityAssignment}, mapping directly to the class name
 * defined in the project architecture and development plan.
 *
 * <p>Traceability: [Clearly implied by paper / Architectural naming]
 * Extends {@link PriorityAssignment} to ensure 100% backward and architectural compatibility.
 */
public class PriorityAssignmentToTasks extends PriorityAssignment {

    /**
     * Constructs PriorityAssignmentToTasks and executes the IEEE Access 2023 PAT algorithm.
     *
     * @param tasks list of tasks to prioritize
     */
    public PriorityAssignmentToTasks(List<Task> tasks) {
        super(tasks);
    }
}
