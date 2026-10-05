import { TaskDto } from '../types/simulation';

export interface PresetScenario {
  id: string;
  name: string;
  badge: string;
  description: string;
  tasks: TaskDto[];
}

export const PRESET_SCENARIOS: PresetScenario[] = [
  {
    id: 'tight-deadlines',
    name: 'Tight Deadlines (Stress Test)',
    badge: 'High Urgency',
    description: 'Deadlines are set very close to execution times. Standard priority suffers heavy deadline misses, while Deadline-Aware maintains compliance.',
    tasks: [
      { taskId: 0, priority: 8, arrivalTime: 0.0, executionTime: 12.0, deadline: 16.0 },
      { taskId: 1, priority: 6, arrivalTime: 1.0, executionTime: 8.0, deadline: 11.0 },
      { taskId: 2, priority: 9, arrivalTime: 2.0, executionTime: 14.0, deadline: 20.0 },
      { taskId: 3, priority: 4, arrivalTime: 3.0, executionTime: 6.0, deadline: 11.5 },
      { taskId: 4, priority: 7, arrivalTime: 4.0, executionTime: 10.0, deadline: 16.0 },
      { taskId: 5, priority: 5, arrivalTime: 5.0, executionTime: 9.0, deadline: 16.5 },
      { taskId: 6, priority: 10, arrivalTime: 6.0, executionTime: 5.0, deadline: 13.0 },
      { taskId: 7, priority: 3, arrivalTime: 7.0, executionTime: 11.0, deadline: 20.0 },
    ],
  },
  {
    id: 'priority-inversion',
    name: 'Urgent Low-Priority (Inversion Test)',
    badge: 'Scientific Edge Case',
    description: 'Demonstrates priority inversion: High-priority tasks have distant deadlines, while low-priority tasks have imminent deadlines.',
    tasks: [
      { taskId: 0, priority: 10, arrivalTime: 0.0, executionTime: 15.0, deadline: 60.0 },
      { taskId: 1, priority: 2, arrivalTime: 0.5, executionTime: 6.0, deadline: 10.0 },
      { taskId: 2, priority: 9, arrivalTime: 1.0, executionTime: 14.0, deadline: 55.0 },
      { taskId: 3, priority: 3, arrivalTime: 1.5, executionTime: 5.0, deadline: 9.5 },
      { taskId: 4, priority: 8, arrivalTime: 2.0, executionTime: 12.0, deadline: 50.0 },
      { taskId: 5, priority: 1, arrivalTime: 2.5, executionTime: 4.0, deadline: 8.5 },
      { taskId: 6, priority: 7, arrivalTime: 3.0, executionTime: 10.0, deadline: 45.0 },
      { taskId: 7, priority: 2, arrivalTime: 3.5, executionTime: 6.0, deadline: 12.0 },
    ],
  },
  {
    id: 'heavy-load',
    name: 'High Concurrency (14 Tasks)',
    badge: 'Heavy Load',
    description: 'Mass arrival of tasks across the 4 VMs. Stresses VM load balancing and queues.',
    tasks: [
      { taskId: 0, priority: 8, arrivalTime: 0.0, executionTime: 12.5, deadline: 30.0 },
      { taskId: 1, priority: 6, arrivalTime: 0.5, executionTime: 9.0, deadline: 25.0 },
      { taskId: 2, priority: 9, arrivalTime: 1.0, executionTime: 15.0, deadline: 35.0 },
      { taskId: 3, priority: 4, arrivalTime: 1.2, executionTime: 7.5, deadline: 22.0 },
      { taskId: 4, priority: 7, arrivalTime: 1.8, executionTime: 11.0, deadline: 28.0 },
      { taskId: 5, priority: 5, arrivalTime: 2.2, executionTime: 8.0, deadline: 24.0 },
      { taskId: 6, priority: 10, arrivalTime: 2.5, executionTime: 6.0, deadline: 18.0 },
      { taskId: 7, priority: 3, arrivalTime: 3.0, executionTime: 14.0, deadline: 38.0 },
      { taskId: 8, priority: 8, arrivalTime: 3.5, executionTime: 10.0, deadline: 29.0 },
      { taskId: 9, priority: 6, arrivalTime: 4.0, executionTime: 8.5, deadline: 27.0 },
      { taskId: 10, priority: 5, arrivalTime: 4.5, executionTime: 12.0, deadline: 36.0 },
      { taskId: 11, priority: 9, arrivalTime: 5.0, executionTime: 7.0, deadline: 21.0 },
      { taskId: 12, priority: 4, arrivalTime: 5.5, executionTime: 13.0, deadline: 40.0 },
      { taskId: 13, priority: 7, arrivalTime: 6.0, executionTime: 9.5, deadline: 30.0 },
    ],
  },
  {
    id: 'balanced-benchmark',
    name: 'Standard Benchmark (10 Tasks)',
    badge: 'Baseline',
    description: 'Balanced distribution of priority, arrival times, and execution requirements.',
    tasks: [
      { taskId: 0, priority: 5, arrivalTime: 0.0, executionTime: 15.65, deadline: 47.31 },
      { taskId: 1, priority: 8, arrivalTime: 1.0, executionTime: 8.0, deadline: 12.0 },
      { taskId: 2, priority: 3, arrivalTime: 2.5, executionTime: 14.0, deadline: 32.0 },
      { taskId: 3, priority: 9, arrivalTime: 3.0, executionTime: 6.5, deadline: 11.0 },
      { taskId: 4, priority: 4, arrivalTime: 4.5, executionTime: 18.0, deadline: 55.0 },
      { taskId: 5, priority: 7, arrivalTime: 6.0, executionTime: 10.0, deadline: 20.0 },
      { taskId: 6, priority: 2, arrivalTime: 7.5, executionTime: 12.0, deadline: 45.0 },
      { taskId: 7, priority: 6, arrivalTime: 8.0, executionTime: 9.5, deadline: 22.0 },
      { taskId: 8, priority: 10, arrivalTime: 10.0, executionTime: 5.0, deadline: 16.0 },
      { taskId: 9, priority: 4, arrivalTime: 12.0, executionTime: 16.0, deadline: 35.0 },
    ],
  },
];
