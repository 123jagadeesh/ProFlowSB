package com.proflow.dto.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintRiskResponse {
    private Long projectId;
    private String projectName;
    private Long sprintId;
    private String sprintTitle;
    private String sprintStatus;
    private RiskSeverity overallRisk;
    private String summary;
    private SprintRiskMetricsDto metrics;

    @Builder.Default
    private List<RiskFindingDto> findings = new ArrayList<>();

    private boolean aiGenerated;
    private Instant generatedAt;
}
