package scheduler.analysis;

import scheduler.experiment.WorkloadSerializer;
import scheduler.model.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Loads raw simulation outcomes and workload files for independent analysis.
 */
public class ResultLoader {

    /**
     * Loads all raw result records from the summary CSV.
     *
     * @param summaryCsvPath path to raw_experiment_results.csv
     * @return unmodifiable list of RawResultRow
     * @throws IOException if I/O error occurs
     */
    public static List<RawResultRow> loadRawResults(Path summaryCsvPath) throws IOException {
        Objects.requireNonNull(summaryCsvPath, "Summary CSV path cannot be null");
        if (!Files.exists(summaryCsvPath)) {
            throw new IOException("Raw results CSV not found at: " + summaryCsvPath);
        }

        List<RawResultRow> rows = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(summaryCsvPath)) {
            String header = reader.readLine(); // skip header
            if (header == null) return Collections.emptyList();

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split(",");
                if (p.length < 14) {
                    throw new IOException("Malformed raw result line: " + line);
                }

                rows.add(new RawResultRow(
                        p[0].trim(),
                        Integer.parseInt(p[1].trim()),
                        Long.parseLong(p[2].trim()),
                        p[3].trim(),
                        p[4].trim(),
                        Double.parseDouble(p[5].trim()),
                        Double.parseDouble(p[6].trim()),
                        Double.parseDouble(p[7].trim()),
                        Double.parseDouble(p[8].trim()),
                        Integer.parseInt(p[9].trim()),
                        Double.parseDouble(p[10].trim()),
                        Integer.parseInt(p[11].trim()),
                        Integer.parseInt(p[12].trim()),
                        Double.parseDouble(p[13].trim())
                ));
            }
        }
        return Collections.unmodifiableList(rows);
    }

    /**
     * Container for workload-level baseline characteristics.
     */
    public record WorkloadCharacteristics(
            double meanPriority,
            double meanExecutionTime,
            double meanDeadlineSlackAtArrival
    ) {}

    /**
     * Computes intrinsic workload characteristics directly from the workload file.
     *
     * @param workloadFilePath path to workload CSV
     * @return calculated WorkloadCharacteristics
     * @throws IOException if error reading workload
     */
    public static WorkloadCharacteristics computeWorkloadCharacteristics(Path workloadFilePath) throws IOException {
        List<Task> tasks = WorkloadSerializer.readWorkloadFromCsv(workloadFilePath);
        if (tasks.isEmpty()) {
            return new WorkloadCharacteristics(0.0, 0.0, 0.0);
        }

        double totalPriority = 0.0;
        double totalExec = 0.0;
        double totalSlack = 0.0;

        for (Task t : tasks) {
            totalPriority += t.getPriority();
            totalExec += t.getExecutionTime();
            totalSlack += (t.getDeadline() - t.getArrivalTime() - t.getExecutionTime());
        }

        int n = tasks.size();
        return new WorkloadCharacteristics(
                Math.round((totalPriority / n) * 10000.0) / 10000.0,
                Math.round((totalExec / n) * 10000.0) / 10000.0,
                Math.round((totalSlack / n) * 10000.0) / 10000.0
        );
    }
}
