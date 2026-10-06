package scheduler.cloudsim;

import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.vms.Vm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import scheduler.baseline.BaselinePriorityScheduler;
import scheduler.baseline.VmAssignment;
import scheduler.model.CloudVmSpec;
import scheduler.model.Task;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cloudlet VM Binding Tests")
public class CloudletBindingTest {

    private static final double REF_MIPS = 1000.0;

    @Test
    @DisplayName("Verify Cloudlet binding adheres strictly to scheduler VM assignments")
    void testCloudletBindingToScheduledVms() {
        List<CloudVmSpec> vms = List.of(
                CloudVmSpec.of(0, 1000.0, 2048, 1000, 10000),
                CloudVmSpec.of(1, 2000.0, 4096, 1000, 10000)
        );

        List<Task> tasks = List.of(
                new Task(1L, 5, 0.0, 2.0, 10.0),
                new Task(2L, 8, 0.0, 3.0, 10.0)
        );

        // Generate baseline schedule
        BaselinePriorityScheduler scheduler = new BaselinePriorityScheduler(vms, REF_MIPS);
        scheduler.schedule(tasks);
        List<VmAssignment> assignments = scheduler.getAssignments();
        assertEquals(2, assignments.size());

        CloudSimEnvironment env = new CloudSimEnvironment(vms);
        CloudSimEnvironment.CloudSimExecutionResult result = env.execute(tasks, assignments, REF_MIPS);

        assertNotNull(result);
        assertEquals(2, result.records().size());

        for (VmAssignment assignment : assignments) {
            var matchingRecord = result.records().stream()
                    .filter(r -> r.taskId() == assignment.taskId())
                    .findFirst()
                    .orElseThrow();

            assertEquals(assignment.vmId(), matchingRecord.assignedVmId(),
                    "Executed VM ID must strictly match scheduler-assigned VM ID");
        }
    }
}
