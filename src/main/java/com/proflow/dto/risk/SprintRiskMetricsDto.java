package com.proflow.dto.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintRiskMetricsDto {
    private int totalTasks;
    private int completedTasks;
    private int incompleteTasks;
    private int overdueTasks;
    private int highPriorityOverdueTasks;
    private int dueSoonTasks;
    private int unassignedTasks;
    private double completionRate;

    @Builder.Default
    private Map<String, Integer> tasksPerAssignee = new HashMap<>();

    @Builder.Default
    private Map<String, Integer> incompleteTasksPerAssignee = new HashMap<>();
}
