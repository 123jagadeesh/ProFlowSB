package com.proflow.dto.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskFindingDto {
    private Long taskId;
    private String taskTitle;
    private RiskSeverity severity;
    private String riskType;
    private String reason;

    @Builder.Default
    private List<String> evidence = new ArrayList<>();

    @Builder.Default
    private List<String> recommendedActions = new ArrayList<>();
}
