import { MetricsDto, TaskResultDto } from '../types/simulation';

export interface GeminiAnalysisResponse {
  summary: string;
  bottlenecks: string;
  tradeoffs: string;
  recommendations: string[];
}

export const getGeminiApiKey = (): string => {
  const envKey = import.meta.env.VITE_GEMINI_API_KEY;
  if (envKey && envKey !== 'YOUR_GEMINI_API_KEY' && envKey.trim().length > 0) {
    return envKey.trim();
  }
  return localStorage.getItem('gemini_api_key')?.trim() || '';
};

export const setGeminiApiKey = (key: string): void => {
  localStorage.setItem('gemini_api_key', key.trim());
};

export const hasGeminiApiKey = (): boolean => {
  return getGeminiApiKey().length > 0;
};

export const callGemini = async (
  prompt: string,
  systemInstruction?: string,
  options?: { jsonMode?: boolean }
): Promise<string> => {
  const apiKey = getGeminiApiKey();
  if (!apiKey) {
    throw new Error('Gemini API Key is not configured. Please add it to .env.local or enter it in the AI Settings.');
  }

  // Model cascade: prioritize gemini-3.8-flash as required by API
  const models = ['gemini-3.8-flash', 'gemini-2.5-flash', 'gemini-1.5-flash'];
  let lastError: Error | null = null;

  for (const model of models) {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;

    const generationConfig: Record<string, unknown> = {
      temperature: 0.2,
      topK: 40,
      topP: 0.95,
      maxOutputTokens: 2048,
    };

    if (options?.jsonMode) {
      generationConfig.responseMimeType = 'application/json';
    }

    const bodyPayload: Record<string, unknown> = {
      contents: [{ role: 'user', parts: [{ text: prompt }] }],
      generationConfig,
    };

    if (systemInstruction) {
      bodyPayload.systemInstruction = {
        parts: [{ text: systemInstruction }],
      };
    }

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(bodyPayload),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        const errMsg = errorData.error?.message || `HTTP ${response.status} ${response.statusText}`;
        throw new Error(errMsg);
      }

      const data = await response.json();
      const text = data.candidates?.[0]?.content?.parts?.[0]?.text;
      if (!text) {
        throw new Error('Empty response received from Gemini.');
      }
      return text;
    } catch (err: unknown) {
      lastError = err instanceof Error ? err : new Error(String(err));
      // If error is about quota or key invalid, don't cascade endlessly
      if (lastError.message.includes('API_KEY_INVALID') || lastError.message.includes('quota')) {
        throw lastError;
      }
    }
  }

  throw lastError || new Error('Failed to communicate with Gemini API.');
};

export const analyzeSimulation = async (params: {
  isCompare: boolean;
  baselineMetrics?: MetricsDto;
  proposedMetrics?: MetricsDto;
  singleMetrics?: MetricsDto;
  singleAlgorithm?: string;
  tasksCount: number;
  baselineTasks?: TaskResultDto[];
  proposedTasks?: TaskResultDto[];
}): Promise<string> => {
  const systemInstruction = `You are a Principal Cloud Computing Architect and Academic Reviewer specializing in Distributed Systems, Quality of Service (QoS), and Cloud Task Scheduling on platforms like CloudSim Plus.
Your job is to analyze the simulation metrics from a cloud scheduler comparison between a baseline Priority Scheduler (Lipsa et al., IEEE Access 2023) and a Proposed Deadline-Aware Priority Scheduler.

Provide an insightful, technically rigorous yet crystal-clear evaluation formatted in clean Markdown with:
1. ### 📌 Executive Summary
2. ### ⏱️ Scheduling & Queue Dynamics
3. ### 🔍 Bottleneck & VM Allocation Analysis
4. ### ⚖️ Scientific Trade-offs & Micro-Workload Context (Address sample size, non-preemptive constraints, and why waiting time vs makespan/miss rate behaves the way it does)
5. ### 💡 Production Recommendations (e.g. VM provisioning, SLA optimization, dynamic tuning)

Keep your tone professional, academic, and encouraging for a college final-year engineering defense.`;

  let prompt = '';
  if (params.isCompare && params.baselineMetrics && params.proposedMetrics) {
    const b = params.baselineMetrics;
    const p = params.proposedMetrics;
    prompt = `Please analyze this comparative cloud simulation run with ${params.tasksCount} tasks on a 4-VM cluster (1000 MIPS each):

### Standard Priority Baseline:
- Makespan: ${b.makespan.toFixed(2)} s
- Average Waiting Time: ${b.averageWaitingTime.toFixed(2)} s
- Average Turnaround Time: ${b.averageTurnaroundTime.toFixed(2)} s
- Throughput: ${b.throughput.toFixed(4)} tasks/s
- Deadline Miss Rate: ${b.deadlineMissRate.toFixed(1)}%
- VM Resource Utilization: ${b.resourceUtilization.toFixed(1)}%

### Proposed Deadline-Aware Scheduler:
- Makespan: ${p.makespan.toFixed(2)} s (Diff: ${(p.makespan - b.makespan >= 0 ? '+' : '')}${(p.makespan - b.makespan).toFixed(2)} s)
- Average Waiting Time: ${p.averageWaitingTime.toFixed(2)} s (Diff: ${(p.averageWaitingTime - b.averageWaitingTime >= 0 ? '+' : '')}${(p.averageWaitingTime - b.averageWaitingTime).toFixed(2)} s)
- Average Turnaround Time: ${p.averageTurnaroundTime.toFixed(2)} s (Diff: ${(p.averageTurnaroundTime - b.averageTurnaroundTime >= 0 ? '+' : '')}${(p.averageTurnaroundTime - b.averageTurnaroundTime).toFixed(2)} s)
- Throughput: ${p.throughput.toFixed(4)} (Diff: ${(p.throughput - b.throughput >= 0 ? '+' : '')}${(p.throughput - b.throughput).toFixed(4)})
- Deadline Miss Rate: ${p.deadlineMissRate.toFixed(1)}% (Diff: ${(p.deadlineMissRate - b.deadlineMissRate >= 0 ? '+' : '')}${(p.deadlineMissRate - b.deadlineMissRate).toFixed(1)}%)
- VM Resource Utilization: ${p.resourceUtilization.toFixed(1)}% (Diff: ${(p.resourceUtilization - b.resourceUtilization >= 0 ? '+' : '')}${(p.resourceUtilization - b.resourceUtilization).toFixed(1)}%)

Please provide a deep-dive analysis explaining the trade-offs, why the average waiting time improved, and how examiners should interpret these findings.`;
  } else if (params.singleMetrics) {
    const m = params.singleMetrics;
    prompt = `Please analyze this single-scheduler simulation run using the ${params.singleAlgorithm || 'Scheduler'} on ${params.tasksCount} tasks (4-VM cluster, 1000 MIPS each):
- Makespan: ${m.makespan.toFixed(2)} s
- Average Waiting Time: ${m.averageWaitingTime.toFixed(2)} s
- Average Turnaround Time: ${m.averageTurnaroundTime.toFixed(2)} s
- Throughput: ${m.throughput.toFixed(4)} tasks/s
- Deadline Miss Rate: ${m.deadlineMissRate.toFixed(1)}%
- VM Resource Utilization: ${m.resourceUtilization.toFixed(1)}%

Provide an executive assessment of its efficiency and suggestions for improvement.`;
  }

  return callGemini(prompt, systemInstruction);
};
