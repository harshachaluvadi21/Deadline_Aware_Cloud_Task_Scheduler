import os
import csv
import matplotlib.pyplot as plt
import numpy as np

# Set clean publication style
plt.rcParams['font.family'] = 'sans-serif'
plt.rcParams['font.size'] = 10
plt.rcParams['axes.titlesize'] = 11
plt.rcParams['axes.labelsize'] = 10
plt.rcParams['xtick.labelsize'] = 9
plt.rcParams['ytick.labelsize'] = 9
plt.rcParams['legend.fontsize'] = 9
plt.rcParams['figure.titlesize'] = 12

def load_data(csv_path):
    data = []
    with open(csv_path, 'r', newline='') as f:
        reader = csv.DictReader(f)
        for row in reader:
            data.append({
                'scenario': row['scenario'],
                'taskCount': int(row['taskCount']),
                'scheduler': row['scheduler'],
                'makespan': float(row['makespan']),
                'waitingTime': float(row['averageWaitingTime']),
                'turnaroundTime': float(row['averageTurnaroundTime']),
                'throughput': float(row['throughput']),
                'missRate': float(row['deadlineMissRate']) * 100.0, # percentage
                'utilization': float(row['resourceUtilization'])
            })
    return data

def plot_metric_grid(data, metric_key, metric_title, y_label, output_filename, output_dir):
    scenarios = ['NORMAL_LOAD', 'HIGH_LOAD', 'DEADLINE_SENSITIVE', 'MIXED']
    task_counts = [20, 50, 100]
    
    fig, axes = plt.subplots(2, 2, figsize=(11, 8))
    axes = axes.flatten()
    
    colors = {
        'IEEE_BASELINE': '#2b5c8f',           # Muted slate blue
        'PROPOSED_DEADLINE_AWARE': '#c4513d'   # Muted terra cotta / crimson
    }
    labels = {
        'IEEE_BASELINE': 'IEEE Baseline',
        'PROPOSED_DEADLINE_AWARE': 'Proposed Deadline-Aware'
    }
    
    for idx, sc in enumerate(scenarios):
        ax = axes[idx]
        sc_data = [d for d in data if d['scenario'] == sc]
        
        base_vals = [next(d[metric_key] for d in sc_data if d['taskCount'] == tc and d['scheduler'] == 'IEEE_BASELINE') for tc in task_counts]
        prop_vals = [next(d[metric_key] for d in sc_data if d['taskCount'] == tc and d['scheduler'] == 'PROPOSED_DEADLINE_AWARE') for tc in task_counts]
        
        x = np.arange(len(task_counts))
        width = 0.35
        
        rects1 = ax.bar(x - width/2, base_vals, width, label=labels['IEEE_BASELINE'], color=colors['IEEE_BASELINE'], edgecolor='black', linewidth=0.6, alpha=0.9)
        rects2 = ax.bar(x + width/2, prop_vals, width, label=labels['PROPOSED_DEADLINE_AWARE'], color=colors['PROPOSED_DEADLINE_AWARE'], edgecolor='black', linewidth=0.6, alpha=0.9)
        
        ax.set_title(f"Scenario: {sc}", fontweight='bold')
        ax.set_xlabel("Task Count")
        ax.set_ylabel(y_label)
        ax.set_xticks(x)
        ax.set_xticklabels(task_counts)
        ax.grid(axis='y', linestyle='--', alpha=0.5)
        
        # Add values on top of bars
        for rect in rects1:
            height = rect.get_height()
            ax.annotate(f'{height:.1f}' if height >= 10 else f'{height:.2f}',
                        xy=(rect.get_x() + rect.get_width() / 2, height),
                        xytext=(0, 3), textcoords="offset points",
                        ha='center', va='bottom', fontsize=8)
        for rect in rects2:
            height = rect.get_height()
            ax.annotate(f'{height:.1f}' if height >= 10 else f'{height:.2f}',
                        xy=(rect.get_x() + rect.get_width() / 2, height),
                        xytext=(0, 3), textcoords="offset points",
                        ha='center', va='bottom', fontsize=8)
        
        # Determine y limits with padding
        max_val = max(max(base_vals), max(prop_vals))
        ax.set_ylim(0, max_val * 1.18 if max_val > 0 else 10)
        
        if idx == 0:
            ax.legend(loc='upper left', framealpha=0.9)

    plt.suptitle(f"Controlled Comparison: {metric_title}", fontsize=13, fontweight='bold')
    plt.tight_layout(rect=[0, 0.03, 1, 0.96])
    
    os.makedirs(output_dir, exist_ok=True)
    out_path = os.path.join(output_dir, output_filename)
    plt.savefig(out_path, dpi=300)
    plt.close()
    print(f"Generated chart: {out_path}")

def main():
    csv_path = os.path.join('results', 'experiments', 'summary', 'raw_experiment_results.csv')
    charts_dir = os.path.join('results', 'charts')
    
    if not os.path.exists(csv_path):
        print(f"Error: Raw summary CSV not found at {csv_path}")
        return
        
    data = load_data(csv_path)
    
    # 1. Makespan
    plot_metric_grid(data, 'makespan', 'Makespan Across Workload Scenarios', 'Makespan (seconds)', 'makespan_comparison.png', charts_dir)
    # 2. Average Waiting Time
    plot_metric_grid(data, 'waitingTime', 'Average Waiting Time Across Workload Scenarios', 'Average Waiting Time (seconds)', 'waiting_time_comparison.png', charts_dir)
    # 3. Average Turnaround Time
    plot_metric_grid(data, 'turnaroundTime', 'Average Turnaround Time Across Workload Scenarios', 'Average Turnaround Time (seconds)', 'turnaround_time_comparison.png', charts_dir)
    # 4. Throughput
    plot_metric_grid(data, 'throughput', 'Throughput Across Workload Scenarios', 'Throughput (tasks/second)', 'throughput_comparison.png', charts_dir)
    # 5. Deadline Miss Rate
    plot_metric_grid(data, 'missRate', 'Deadline Miss Rate Across Workload Scenarios', 'Deadline Miss Rate (%)', 'deadline_miss_rate_comparison.png', charts_dir)
    # 6. Resource Utilization
    plot_metric_grid(data, 'utilization', 'Time-based VM Resource Utilization Across Workload Scenarios', 'Resource Utilization (%)', 'resource_utilization_comparison.png', charts_dir)

if __name__ == '__main__':
    main()
