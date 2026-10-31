package com.proflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proflow.dto.risk.AiRiskExplanationDto;
import com.proflow.dto.risk.RiskFindingDto;
import com.proflow.dto.risk.RiskSeverity;
import com.proflow.dto.risk.SprintRiskResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiRiskExplanationService {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    private static final String SYSTEM_PROMPT = """
            You are ProFlow's Project Risk Assistant.
            Your purpose is to explain factual sprint risks calculated by the system and suggest practical, actionable steps for the project manager.

            CRITICAL RULES:
            1. PostgreSQL is the source of truth. Use ONLY the supplied project, sprint, metrics, and findings evidence.
            2. NEVER invent task statuses, progress percentages, assignees, deadlines, or historical velocity.
            3. Do NOT make up blockers or reasons not found in the evidence.
            4. Every task mentioned in taskExplanations MUST have a valid taskId from the supplied findings.
            5. Return ONLY a valid JSON object matching this schema (do NOT wrap in markdown ```json or prose):
            {
              "summary": "Crisp 2-3 sentence overview of the sprint's risk profile",
              "overallRisk": "CRITICAL|HIGH|MEDIUM|LOW",
              "strategicRecommendations": ["High level action item 1", "Action item 2"],
              "taskExplanations": [
                {
                  "taskId": 123,
                  "explanation": "Why this specific task poses a risk",
                  "recommendedAction": "Actionable step to unblock or mitigate"
                }
              ]
            }
            """;

    @Autowired
    public AiRiskExplanationService(
            @Autowired(required = false) ChatModel chatModel,
            ObjectMapper objectMapper
    ) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    public SprintRiskResponse enrichWithAiExplanation(SprintRiskResponse deterministicReport) {
        // Fallback early if AI is not configured or disabled
        if (chatModel == null || apiKey == null || apiKey.isBlank() || "demo-key".equalsIgnoreCase(apiKey)) {
            log.info("Spring AI is running in deterministic mode (No valid GEMINI_API_KEY configured).");
            return deterministicReport;
        }

        try {
            // Build minimal, anonymized evidence payload
            Map<String, Object> evidencePayload = new LinkedHashMap<>();
            evidencePayload.put("projectId", deterministicReport.getProjectId());
            evidencePayload.put("projectName", deterministicReport.getProjectName());
            evidencePayload.put("sprintId", deterministicReport.getSprintId());
            evidencePayload.put("sprintTitle", deterministicReport.getSprintTitle());
            evidencePayload.put("sprintStatus", deterministicReport.getSprintStatus());
            evidencePayload.put("metrics", deterministicReport.getMetrics());

            List<Map<String, Object>> findingsSummary = deterministicReport.getFindings().stream()
                    .map(f -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("taskId", f.getTaskId());
                        map.put("taskTitle", f.getTaskTitle());
                        map.put("severity", f.getSeverity());
                        map.put("riskType", f.getRiskType());
                        map.put("reason", f.getReason());
                        map.put("evidence", f.getEvidence());
                        return map;
                    })
                    .collect(Collectors.toList());

            evidencePayload.put("findings", findingsSummary);

            String evidenceJson = objectMapper.writeValueAsString(evidencePayload);

            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(SYSTEM_PROMPT),
                    new UserMessage("Evidence Payload:\n" + evidenceJson)
            ));

            ChatResponse response = chatModel.call(prompt);
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                log.warn("Empty response received from LLM. Falling back to deterministic analysis.");
                return deterministicReport;
            }

            String content = response.getResult().getOutput().getText();
            if (content == null || content.isBlank()) {
                return deterministicReport;
            }

            // Clean any potential markdown code block wrappers
            content = cleanJsonMarkdown(content);

            AiRiskExplanationDto aiDto = objectMapper.readValue(content, AiRiskExplanationDto.class);

            // Validate and merge AI insights
            return mergeAiExplanation(deterministicReport, aiDto);

        } catch (Exception e) {
            log.warn("Failed to generate AI explanation from model: {}. Safely falling back to deterministic risk report.", e.getMessage());
            return deterministicReport;
        }
    }

    private SprintRiskResponse mergeAiExplanation(SprintRiskResponse deterministicReport, AiRiskExplanationDto aiDto) {
        // Collect valid task IDs from deterministic findings
        Set<Long> validTaskIds = deterministicReport.getFindings().stream()
                .map(RiskFindingDto::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Update overall summary if valid
        if (aiDto.getSummary() != null && !aiDto.getSummary().isBlank()) {
            deterministicReport.setSummary(aiDto.getSummary());
        }

        // Align AI task explanations with existing verified findings
        if (aiDto.getTaskExplanations() != null) {
            Map<Long, AiRiskExplanationDto.TaskExplanation> explanationsByTaskId = aiDto.getTaskExplanations().stream()
                    .filter(te -> te.getTaskId() != null && validTaskIds.contains(te.getTaskId()))
                    .collect(Collectors.toMap(
                            AiRiskExplanationDto.TaskExplanation::getTaskId,
                            te -> te,
                            (existing, replacement) -> existing
                    ));

            for (RiskFindingDto finding : deterministicReport.getFindings()) {
                if (finding.getTaskId() != null && explanationsByTaskId.containsKey(finding.getTaskId())) {
                    AiRiskExplanationDto.TaskExplanation te = explanationsByTaskId.get(finding.getTaskId());
                    if (te.getRecommendedAction() != null && !te.getRecommendedAction().isBlank()) {
                        List<String> actions = new ArrayList<>(finding.getRecommendedActions());
                        if (!actions.contains(te.getRecommendedAction())) {
                            actions.add(0, te.getRecommendedAction()); // Add AI recommendation at top
                        }
                        finding.setRecommendedActions(actions);
                    }
                }
            }
        }

        deterministicReport.setAiGenerated(true);
        return deterministicReport;
    }

    private String cleanJsonMarkdown(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
