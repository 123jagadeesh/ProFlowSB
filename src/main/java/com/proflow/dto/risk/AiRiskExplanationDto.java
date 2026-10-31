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
public class AiRiskExplanationDto {
    private String summary;
    private String overallRisk;

    @Builder.Default
    private List<String> strategicRecommendations = new ArrayList<>();

    @Builder.Default
    private List<TaskExplanation> taskExplanations = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskExplanation {
        private Long taskId;
        private String explanation;
        private String recommendedAction;
    }
}
