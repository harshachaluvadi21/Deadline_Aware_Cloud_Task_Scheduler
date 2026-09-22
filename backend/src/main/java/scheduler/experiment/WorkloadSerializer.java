package scheduler.experiment;

import scheduler.model.Task;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Handles persistent serialization, deserialization, and manifest tracking for experimental workloads.
 *
 * <p>Enables full auditability and reproducibility by recording every generated workload
 * and its lineage in a central workload manifest.
 */
public class WorkloadSerializer {

    public static final String WORKLOAD_HEADER = "taskId,priority,arrivalTime,executionTime,deadline";
    public static final String MANIFEST_HEADER = "scenario,taskCount,seed,workloadFile";
    public static final String DEFAULT_MANIFEST_NAME = "workload_manifest.csv";

    /**
     * Entry recorded in the reproducible workload manifest.
     */
    public record WorkloadManifestEntry(
            String scenario,
            int taskCount,
            long seed,
            String workloadFile
    ) {
        public String toCsvRow() {
            return String.format(Locale.US, "%s,%d,%d,%s", scenario, taskCount, seed, workloadFile);
        }
    }

    /**
     * Generates standard filename for a workload CSV, e.g. "normal_load_20_seed1001.csv".
     *
     * @param scenario  the scenario type
     * @param taskCount task count
     * @param seed      pseudorandom seed
     * @return standardized filename
     */
    public static String getWorkloadFileName(WorkloadScenarioType scenario, int taskCount, long seed) {
        return String.format(Locale.US, "%s_%d_seed%d.csv", scenario.name().toLowerCase(Locale.US), taskCount, seed);
    }

    /**
     * Writes a list of tasks to a CSV file.
     *
     * @param tasks    list of tasks
     * @param filePath target CSV path
     * @throws IOException if I/O error occurs
     */
    public static void writeWorkloadToCsv(List<Task> tasks, Path filePath) throws IOException {
        Objects.requireNonNull(tasks, "Tasks cannot be null");
        Objects.requireNonNull(filePath, "File path cannot be null");

        if (filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(WORKLOAD_HEADER);
            writer.newLine();
            for (Task t : tasks) {
                writer.write(String.format(Locale.US, "%d,%d,%.2f,%.2f,%.2f",
                        t.getTaskId(),
                        t.getPriority(),
                        t.getArrivalTime(),
                        t.getExecutionTime(),
                        t.getDeadline()));
                writer.newLine();
            }
        }
    }

    /**
     * Reads tasks from a workload CSV file.
     *
     * @param filePath path to workload CSV
     * @return unmodifiable list of newly parsed Task objects
     * @throws IOException if I/O error or parsing error occurs
     */
    public static List<Task> readWorkloadFromCsv(Path filePath) throws IOException {
        Objects.requireNonNull(filePath, "File path cannot be null");
        if (!Files.exists(filePath)) {
            throw new IOException("Workload file does not exist: " + filePath);
        }

        List<Task> tasks = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String header = reader.readLine();
            if (header == null || !header.trim().equalsIgnoreCase(WORKLOAD_HEADER)) {
                throw new IOException("Invalid workload CSV header. Expected: " + WORKLOAD_HEADER + ", got: " + header);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length < 5) {
                    throw new IOException("Malformed CSV row: " + line);
                }
                long taskId = Long.parseLong(parts[0].trim());
                int priority = Integer.parseInt(parts[1].trim());
                double arrivalTime = Double.parseDouble(parts[2].trim());
                double executionTime = Double.parseDouble(parts[3].trim());
                double deadline = Double.parseDouble(parts[4].trim());

                tasks.add(new Task(taskId, priority, arrivalTime, executionTime, deadline));
            }
        }
        return Collections.unmodifiableList(tasks);
    }

    /**
     * Records an entry into the workload manifest CSV, creating the file with a header if it does not exist.
     *
     * @param manifestPath path to manifest CSV
     * @param entry        the manifest entry to append
     * @throws IOException if I/O error occurs
     */
    public static synchronized void recordManifest(Path manifestPath, WorkloadManifestEntry entry) throws IOException {
        Objects.requireNonNull(manifestPath, "Manifest path cannot be null");
        Objects.requireNonNull(entry, "Manifest entry cannot be null");

        if (manifestPath.getParent() != null) {
            Files.createDirectories(manifestPath.getParent());
        }

        boolean fileExists = Files.exists(manifestPath);
        if (fileExists) {
            // Check if already present to avoid duplicate entries
            List<String> lines = Files.readAllLines(manifestPath);
            String targetRow = entry.toCsvRow();
            for (String l : lines) {
                if (l.trim().equalsIgnoreCase(targetRow.trim())) {
                    return; // already recorded
                }
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                manifestPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            if (!fileExists || Files.size(manifestPath) == 0) {
                writer.write(MANIFEST_HEADER);
                writer.newLine();
            }
            writer.write(entry.toCsvRow());
            writer.newLine();
        }
    }

    /**
     * Reads all entries from a workload manifest CSV.
     *
     * @param manifestPath path to manifest CSV
     * @return unmodifiable list of manifest entries
     * @throws IOException if I/O error occurs
     */
    public static List<WorkloadManifestEntry> readManifest(Path manifestPath) throws IOException {
        Objects.requireNonNull(manifestPath, "Manifest path cannot be null");
        if (!Files.exists(manifestPath)) {
            return Collections.emptyList();
        }

        List<WorkloadManifestEntry> entries = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(manifestPath)) {
            String header = reader.readLine();
            if (header == null) return Collections.emptyList();

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    entries.add(new WorkloadManifestEntry(
                            parts[0].trim(),
                            Integer.parseInt(parts[1].trim()),
                            Long.parseLong(parts[2].trim()),
                            parts[3].trim()
                    ));
                }
            }
        }
        return Collections.unmodifiableList(entries);
    }
}
