package com.proflow.controller;

import com.proflow.dto.risk.SprintRiskResponse;
import com.proflow.security.UserPrincipal;
import com.proflow.service.SprintRiskAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/sprints/{sprintId}/ai/risk-analysis")
@RequiredArgsConstructor
@Tag(name = "AI Risk Assistant", description = "AI project and sprint risk analysis, bottleneck detection, and mitigation recommendations")
public class SprintRiskController {

    private final SprintRiskAnalysisService riskAnalysisService;

    @PostMapping
    @Operation(summary = "Analyze sprint risks and bottleneck indicators (POST trigger)")
    public ResponseEntity<SprintRiskResponse> triggerSprintRiskAnalysis(
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        SprintRiskResponse response = riskAnalysisService.analyzeSprintRisks(projectId, sprintId, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get sprint risk analysis report (GET)")
    public ResponseEntity<SprintRiskResponse> getSprintRiskAnalysis(
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        SprintRiskResponse response = riskAnalysisService.analyzeSprintRisks(projectId, sprintId, currentUser);
        return ResponseEntity.ok(response);
    }
}
