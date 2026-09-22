package scheduler.baseline;

import scheduler.model.Task;

import java.util.List;

/**
 * Architectural alias for {@link WaitingTimeMatrix}, mapping to the name specified in the project plan.
 *
 * <p>Traceability: [Clearly implied by paper / Architectural naming]
 * Extends {@link WaitingTimeMatrix} to ensure 100% backward and architectural compatibility.
 */
public class WaitingTimeControlMatrix extends WaitingTimeMatrix {

    /**
     * Constructs a WaitingTimeControlMatrix for the given tasks.
     *
     * @param taskList the list of tasks (must not be null or empty)
     */
    public WaitingTimeControlMatrix(List<Task> taskList) {
        super(taskList);
    }
}
