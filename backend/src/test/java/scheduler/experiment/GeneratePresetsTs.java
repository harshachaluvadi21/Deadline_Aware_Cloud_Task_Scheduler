package scheduler.experiment;

import org.junit.jupiter.api.Test;
import scheduler.model.Task;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class GeneratePresetsTs {

    @Test
    void exportPresetsToFrontend() throws IOException {
        List<EvaluationWorkloadSuite.WorkloadDefinition> workloads = EvaluationWorkloadSuite.getAllWorkloads();

        StringBuilder sb = new StringBuilder();
        sb.append("import { TaskDto } from '../types/simulation';\n\n");
        sb.append("export interface PresetScenario {\n");
        sb.append("  id: string;\n");
        sb.append("  name: string;\n");
        sb.append("  badge: string;\n");
        sb.append("  description: string;\n");
        sb.append("  tasks: TaskDto[];\n");
        sb.append("}\n\n");
        sb.append("export const PRESET_SCENARIOS: PresetScenario[] = [\n");

        for (EvaluationWorkloadSuite.WorkloadDefinition def : workloads) {
            sb.append("  {\n");
            sb.append("    id: '").append(def.id()).append("',\n");
            sb.append("    name: '").append(def.name()).append("',\n");
            sb.append("    badge: '").append(def.category()).append("',\n");
            sb.append("    description: '").append(def.description().replace("'", "\\'")).append("',\n");
            sb.append("    tasks: [\n");

            for (int i = 0; i < def.tasks().size(); i++) {
                Task t = def.tasks().get(i);
                String tType = (def.taskTypes() != null && i < def.taskTypes().size())
                    ? def.taskTypes().get(i)
                    : null;
                sb.append("      { taskId: ").append(t.getTaskId())
                  .append(", priority: ").append(t.getPriority())
                  .append(", arrivalTime: ").append(t.getArrivalTime())
                  .append(", executionTime: ").append(t.getExecutionTime())
                  .append(", deadline: ").append(t.getDeadline());
                if (tType != null && !tType.isEmpty()) {
                    sb.append(", taskType: '").append(tType.replace("'", "\\'")).append("'");
                }
                sb.append(" },\n");
            }
            sb.append("    ],\n");
            sb.append("  },\n");
        }

        sb.append("];\n");

        String outputPath = Paths.get("..", "frontend", "src", "data", "presetWorkloads.ts").toAbsolutePath().normalize().toString();
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(sb.toString());
        }
        System.out.println("Successfully generated presets TS file at: " + outputPath);
    }
}
