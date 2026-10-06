package scheduler.cloudsim;

import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import scheduler.experiment.TaskExecutionRecord;
import scheduler.model.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adapter bridging application domain {@link Task} objects and CloudSim Plus 8.0.0 {@link Cloudlet} instances.
 *
 * <p><b>Workload-to-Cloudlet Mapping Conventions:</b>
 * <ul>
 *   <li><b>Computational Workload (MI):</b>
 *       $$\text{Cloudlet Length (MI)} = \text{round}(\text{Task executionTime (seconds)} \times \text{referenceMips})$$
 *       For example, a task with nominal duration 2.50s under reference MIPS of 1000.0 is translated into a
 *       2,500 MI Cloudlet. When dispatched to a 1000 MIPS VM, CloudSim Plus executes it in exactly 2.50s.
 *       When dispatched to a 500 MIPS VM, CloudSim Plus executes it in 5.00s.</li>
 *   <li><b>Arrival Time Mapping:</b>
 *       The domain {@code task.getArrivalTime()} represents the absolute simulation timestamp at which the task
 *       becomes available in the datacenter. In CloudSim Plus, this is mapped directly to the Cloudlet's
 *       {@code submissionDelay}, instructing the {@link org.cloudsimplus.brokers.DatacenterBroker} to defer submission
 *       until the simulation clock reaches that arrival timestamp.
 *       $$\text{submissionDelay} = \text{arrivalTime}$$
 *   </li>
 *   <li><b>PE Requirement:</b> Each task is mapped to a single-core Cloudlet (1 PE).</li>
 * </ul>
 *
 * <p><b>Result Harvesting Conventions:</b>
 * <ul>
 *   <li>{@code cloudlet.getExecStartTime()} $\to$ {@code task.startTime}</li>
 *   <li>{@code cloudlet.getFinishTime()} $\to$ {@code task.completionTime}</li>
 *   <li>{@code cloudlet.getVm().getId()} $\to$ {@code task.allocatedVmId}</li>
 * </ul>
 */
public class TaskCloudletAdapter {

    /**
     * Converts a domain {@link Task} into a CloudSim Plus {@link CloudletSimple}.
     *
     * @param task          the domain task definition
     * @param referenceMips reference MIPS rating used for scaling nominal seconds to Million Instructions (MI)
     * @return configured CloudletSimple ready for CloudSim broker submission
     */
    public static CloudletSimple toCloudlet(Task task, double referenceMips) {
        Objects.requireNonNull(task, "Task cannot be null");
        if (referenceMips <= 0.0) {
            throw new IllegalArgumentException("Reference MIPS must be positive, got: " + referenceMips);
        }

        // Computational length in Million Instructions (MI)
        long lengthMi = Math.max(1L, Math.round(task.getExecutionTime() * referenceMips));

        CloudletSimple cloudlet = new CloudletSimple(task.getTaskId(), lengthMi, 1);
        cloudlet.setUtilizationModelCpu(new UtilizationModelFull())
                .setUtilizationModelRam(new UtilizationModelFull())
                .setUtilizationModelBw(new UtilizationModelFull());

        // Map domain arrival time to CloudSim Plus submission delay
        cloudlet.setSubmissionDelay(Math.max(0.0, task.getArrivalTime()));

        return cloudlet;
    }

    /**
     * Converts a collection of domain tasks into CloudSim Plus cloudlets.
     *
     * @param tasks         list of domain tasks
     * @param referenceMips reference MIPS rating
     * @return list of converted CloudletSimple instances
     */
    public static List<CloudletSimple> toCloudletList(List<Task> tasks, double referenceMips) {
        Objects.requireNonNull(tasks, "Task list cannot be null");
        List<CloudletSimple> cloudlets = new ArrayList<>(tasks.size());
        for (Task task : tasks) {
            cloudlets.add(toCloudlet(task, referenceMips));
        }
        return cloudlets;
    }

    /**
     * Updates domain task lifecycle state from completed CloudSim Plus Cloudlet execution data.
     *
     * @param task     the domain task to update
     * @param cloudlet the executed Cloudlet returned from the CloudSim broker
     */
    public static void updateTaskFromCloudlet(Task task, Cloudlet cloudlet) {
        Objects.requireNonNull(task, "Task cannot be null");
        Objects.requireNonNull(cloudlet, "Cloudlet cannot be null");

        double execStart = cloudlet.getExecStartTime();
        double finishTime = cloudlet.getFinishTime();
        long vmId = cloudlet.getVm() != null ? cloudlet.getVm().getId() : 0L;

        // Guard against minute double floating-point precision differences where start < arrival
        double safeStart = Math.max(execStart, task.getArrivalTime());
        double safeFinish = Math.max(finishTime, safeStart);

        task.resetRuntimeState();
        task.markReady();
        task.markRunning(safeStart, vmId);
        task.markFinished(safeFinish);
    }

    /**
     * Harvests CloudSim Plus execution metrics into a standardized {@link TaskExecutionRecord}.
     *
     * @param task         domain task
     * @param cloudlet     completed CloudSim cloudlet
     * @param dynamicScore recorded priority / heuristic score
     * @return populated TaskExecutionRecord
     */
    public static TaskExecutionRecord toTaskExecutionRecord(Task task, Cloudlet cloudlet, double dynamicScore) {
        Objects.requireNonNull(task, "Task cannot be null");
        Objects.requireNonNull(cloudlet, "Cloudlet cannot be null");

        double arrival = task.getArrivalTime();
        double start = Math.max(cloudlet.getExecStartTime(), arrival);
        double completion = Math.max(cloudlet.getFinishTime(), start);
        long vmId = cloudlet.getVm() != null ? cloudlet.getVm().getId() : 0L;
        double wait = start - arrival;
        double turnaround = completion - arrival;
        double cpuTime = cloudlet.getActualCpuTime() > 0.0 ? cloudlet.getActualCpuTime() : (completion - start);
        boolean deadlineMissed = completion > task.getDeadline();

        return new TaskExecutionRecord(
                task.getTaskId(),
                vmId,
                arrival,
                start,
                completion,
                cpuTime,
                wait,
                turnaround,
                task.getPriority(),
                dynamicScore,
                task.getDeadline(),
                deadlineMissed
        );
    }
}
