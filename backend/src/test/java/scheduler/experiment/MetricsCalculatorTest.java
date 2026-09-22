package scheduler.experiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MetricsCalculator Tests")
class MetricsCalculatorTest {

    @Test
    @DisplayName("Calculates makespan, waiting time, turnaround, throughput, and miss rate accurately")
    void testStandardMetricsCalculation() {
        // Task 0: arr=0.0, start=0.0, comp=10.0, wait=0.0, turn=10.0, deadline=15.0 (met)
        // Task 1: arr=2.0, start=10.0, comp=20.0, wait=8.0, turn=18.0, deadline=18.0 (missed: 20 > 18)
        // Task 2: arr=4.0, start=5.0, comp=15.0, wait=1.0, turn=11.0, deadline=25.0 (met)
        List<TaskExecutionRecord> records = List.of(
                new TaskExecutionRecord(0, 0, 0.0, 0.0, 10.0, 10.0, 0.0, 10.0, 5, 0.5, 15.0, false),
                new TaskExecutionRecord(1, 0, 2.0, 10.0, 20.0, 10.0, 8.0, 18.0, 3, 0.3, 18.0, true),
                new TaskExecutionRecord(2, 1, 4.0, 5.0, 15.0, 10.0, 1.0, 11.0, 8, 0.8, 25.0, false)
        );

        // Makespan = max(comp) - min(arr) = 20.0 - 0.0 = 20.0
        double makespan = MetricsCalculator.calculateMakespan(records);
        assertEquals(20.0, makespan, 1e-4);

        // AvgWait = (0 + 8 + 1) / 3 = 9 / 3 = 3.0
        double avgWait = MetricsCalculator.calculateAverageWaitingTime(records);
        assertEquals(3.0, avgWait, 1e-4);

        // AvgTurnaround = (10 + 18 + 11) / 3 = 39 / 3 = 13.0
        double avgTurnaround = MetricsCalculator.calculateAverageTurnaroundTime(records);
        assertEquals(13.0, avgTurnaround, 1e-4);

        // Throughput = 3 / 20.0 = 0.15 tasks/sec
        double throughput = MetricsCalculator.calculateThroughput(records, makespan);
        assertEquals(0.15, throughput, 1e-4);

        // Miss count = 1, Miss rate = 1 / 3 = 0.3333
        int missCount = MetricsCalculator.calculateDeadlineMissCount(records);
        assertEquals(1, missCount);

        MetricsCalculator.MetricsSummary summary = MetricsCalculator.calculate(records, 2);
        assertEquals(20.0, summary.makespan(), 1e-4);
        assertEquals(3.0, summary.averageWaitingTime(), 1e-4);
        assertEquals(13.0, summary.averageTurnaroundTime(), 1e-4);
        assertEquals(0.15, summary.throughput(), 1e-4);
        assertEquals(1.0 / 3.0, summary.deadlineMissRate(), 1e-4);
        assertEquals(1, summary.deadlineMissedCount());
        assertEquals(3, summary.completedTaskCount());
    }

    @Test
    @DisplayName("Time-based VM Resource Utilization adheres to sum(busy)/(VMs * makespan) * 100")
    void testTimeBasedVmResourceUtilization() {
        // 2 VMs:
        // VM 0: runs task 0 (busy 10.0) and task 1 (busy 10.0) -> busy = 20.0
        // VM 1: runs task 2 (busy 10.0) -> busy = 10.0
        // Total busy time = 30.0
        // Makespan = 20.0
        // VM capacity = 2 * 20.0 = 40.0
        // Utilization = (30.0 / 40.0) * 100 = 75.0%
        List<TaskExecutionRecord> records = List.of(
                new TaskExecutionRecord(0, 0, 0.0, 0.0, 10.0, 10.0, 0.0, 10.0, 5, 0.5, 15.0, false),
                new TaskExecutionRecord(1, 0, 2.0, 10.0, 20.0, 10.0, 8.0, 18.0, 3, 0.3, 18.0, true),
                new TaskExecutionRecord(2, 1, 4.0, 5.0, 15.0, 10.0, 1.0, 11.0, 8, 0.8, 25.0, false)
        );

        double util = MetricsCalculator.calculateTimeBasedVmResourceUtilization(records, 2, 20.0);
        assertEquals(75.0, util, 1e-4);
    }

    @Test
    @DisplayName("Time-based VM Resource Utilization is strictly bounded in [0, 100]")
    void testUtilizationBounds() {
        // Fully saturated 1 VM run: busy = 10.0, makespan = 10.0 -> 100%
        List<TaskExecutionRecord> fullRecords = List.of(
                new TaskExecutionRecord(0, 0, 0.0, 0.0, 10.0, 10.0, 0.0, 10.0, 5, 0.5, 15.0, false)
        );
        double fullUtil = MetricsCalculator.calculateTimeBasedVmResourceUtilization(fullRecords, 1, 10.0);
        assertEquals(100.0, fullUtil, 1e-4);

        // Edge case: if mathematical anomalies exceed makespan, clamp to 100.0
        double clamped = MetricsCalculator.calculateTimeBasedVmResourceUtilization(fullRecords, 1, 5.0);
        assertEquals(100.0, clamped, 1e-4);
    }

    @Test
    @DisplayName("Handles zero makespan, zero VMs, and empty task list without division-by-zero")
    void testEdgeCases() {
        MetricsCalculator.MetricsSummary emptySummary = MetricsCalculator.calculate(Collections.emptyList(), 4);
        assertEquals(0.0, emptySummary.makespan());
        assertEquals(0.0, emptySummary.averageWaitingTime());
        assertEquals(0.0, emptySummary.averageTurnaroundTime());
        assertEquals(0.0, emptySummary.throughput());
        assertEquals(0.0, emptySummary.deadlineMissRate());
        assertEquals(0, emptySummary.deadlineMissedCount());
        assertEquals(0, emptySummary.completedTaskCount());
        assertEquals(0.0, emptySummary.resourceUtilization());

        double utilZeroMakespan = MetricsCalculator.calculateTimeBasedVmResourceUtilization(
                List.of(new TaskExecutionRecord(0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1, 0.1, 1.0, false)),
                4,
                0.0
        );
        assertEquals(0.0, utilZeroMakespan);

        double utilZeroVms = MetricsCalculator.calculateTimeBasedVmResourceUtilization(
                List.of(new TaskExecutionRecord(0, 0, 0.0, 0.0, 5.0, 5.0, 0.0, 5.0, 1, 0.1, 1.0, false)),
                0,
                5.0
        );
        assertEquals(0.0, utilZeroVms);
    }
}
