package com.proflow.service;

import com.proflow.dto.risk.RiskFindingDto;
import com.proflow.dto.risk.RiskSeverity;
import com.proflow.dto.risk.SprintRiskMetricsDto;
import com.proflow.dto.risk.SprintRiskResponse;
import com.proflow.entity.Project;
import com.proflow.entity.Sprint;
import com.proflow.entity.Task;
import com.proflow.entity.User;
import com.proflow.exception.ResourceNotFoundException;
import com.proflow.exception.UnauthorizedException;
import com.proflow.repository.ProjectRepository;
import com.proflow.repository.SprintRepository;
import com.proflow.repository.TaskRepository;
import com.proflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SprintRiskAnalysisService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final Clock clock;
    private final AiRiskExplanationService aiRiskExplanationService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneOffset.UTC);

    @Transactional(readOnly = true)
    public SprintRiskResponse analyzeSprintRisks(Long projectId, Long sprintId, UserPrincipal currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        if (!"super_admin".equalsIgnoreCase(currentUser.getRole()) &&
                !project.getCompany().getId().equals(currentUser.getCompanyId())) {
            throw new UnauthorizedException("Unauthorized access to project");
        }

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found with id: " + sprintId));

        if (!sprint.getProject().getId().equals(projectId) ||
                (!"super_admin".equalsIgnoreCase(currentUser.getRole()) &&
                        !sprint.getCompany().getId().equals(currentUser.getCompanyId()))) {
            throw new UnauthorizedException("Sprint does not belong to the requested project or company");
        }

        List<Task> sprintTasks = taskRepository.findBySprintIdAndProjectIdAndCompanyId(
                sprintId,
                projectId,
                project.getCompany().getId()
        );

        SprintRiskResponse deterministicReport = computeRiskReport(project, sprint, sprintTasks);
        if (aiRiskExplanationService != null) {
            return aiRiskExplanationService.enrichWithAiExplanation(deterministicReport);
        }
        return deterministicReport;
    }

    public SprintRiskResponse computeRiskReport(Project project, Sprint sprint, List<Task> tasks) {
        Instant now = Instant.now(clock);
        String sprintStatus = sprint.getStatus() != null ? sprint.getStatus() : "Created";

        int totalTasks = tasks.size();
        int completedTasks = 0;
        int incompleteTasks = 0;
        int overdueTasks = 0;
        int highPriorityOverdueTasks = 0;
        int dueSoonTasks = 0;
        int unassignedTasks = 0;

        Map<String, Integer> tasksPerAssignee = new HashMap<>();
        Map<String, Integer> incompletePerAssignee = new HashMap<>();

        List<RiskFindingDto> findings = new ArrayList<>();

        // If sprint is Completed, return retrospective status without flagging past deadlines
        if ("Completed".equalsIgnoreCase(sprintStatus)) {
            for (Task task : tasks) {
                if (isTaskCompleted(task)) {
                    completedTasks++;
                } else {
                    incompleteTasks++;
                }
            }
            double completionRate = totalTasks > 0 ? (completedTasks * 100.0) / totalTasks : 0.0;
            SprintRiskMetricsDto metrics = SprintRiskMetricsDto.builder()
                    .totalTasks(totalTasks)
                    .completedTasks(completedTasks)
                    .incompleteTasks(incompleteTasks)
                    .completionRate(Math.round(completionRate * 100.0) / 100.0)
                    .build();

            return SprintRiskResponse.builder()
                    .projectId(project.getId())
                    .projectName(project.getName())
                    .sprintId(sprint.getId())
                    .sprintTitle(sprint.getTitle())
                    .sprintStatus(sprintStatus)
                    .overallRisk(incompleteTasks > 0 ? RiskSeverity.MEDIUM : RiskSeverity.LOW)
                    .summary(String.format("Sprint is completed. %d of %d tasks were completed (%.1f%%).",
                            completedTasks, totalTasks, completionRate))
                    .metrics(metrics)
                    .findings(findings)
                    .aiGenerated(false)
                    .generatedAt(now)
                    .build();
        }

        // If sprint is Created (not started yet), run pre-sprint planning checks
        if ("Created".equalsIgnoreCase(sprintStatus)) {
            for (Task task : tasks) {
                if (task.getAssignee() == null || task.getAssignee().isEmpty()) {
                    unassignedTasks++;
                    findings.add(RiskFindingDto.builder()
                            .taskId(task.getId())
                            .taskTitle(task.getTitle())
                            .severity(RiskSeverity.INFO)
                            .riskType("UNASSIGNED_IN_PLANNING")
                            .reason("Task has no assigned team member before sprint kickoff.")
                            .evidence(List.of("Status: " + task.getStatus(), "Priority: " + task.getPriority()))
                            .recommendedActions(List.of("Assign an owner during sprint planning."))
                            .build());
                }
            }

            SprintRiskMetricsDto metrics = SprintRiskMetricsDto.builder()
                    .totalTasks(totalTasks)
                    .unassignedTasks(unassignedTasks)
                    .build();

            return SprintRiskResponse.builder()
                    .projectId(project.getId())
                    .projectName(project.getName())
                    .sprintId(sprint.getId())
                    .sprintTitle(sprint.getTitle())
                    .sprintStatus(sprintStatus)
                    .overallRisk(unassignedTasks > 0 ? RiskSeverity.INFO : RiskSeverity.LOW)
                    .summary("Sprint is in planning phase (Created). No active execution risks detected.")
                    .metrics(metrics)
                    .findings(findings)
                    .aiGenerated(false)
                    .generatedAt(now)
                    .build();
        }

        // Active sprint: Calculate full deterministic risk indicators
        Instant dueSoonWindow = now.plus(Duration.ofHours(48));

        for (Task task : tasks) {
            boolean isCompleted = isTaskCompleted(task);
            if (isCompleted) {
                completedTasks++;
            } else {
                incompleteTasks++;

                // Track assignee distribution
                if (task.getAssignee() == null || task.getAssignee().isEmpty()) {
                    unassignedTasks++;
                    findings.add(RiskFindingDto.builder()
                            .taskId(task.getId())
                            .taskTitle(task.getTitle())
                            .severity(RiskSeverity.MEDIUM)
                            .riskType("UNASSIGNED_ACTIVE_TASK")
                            .reason("Task is in an active sprint with no assigned owner.")
                            .evidence(List.of(
                                    "Status: " + task.getStatus(),
                                    "Priority: " + task.getPriority(),
                                    task.getPlannedDateEnd() != null ? "Planned End: " + DATE_FORMATTER.format(task.getPlannedDateEnd()) : "No planned end date"
                            ))
                            .recommendedActions(List.of(
                                    "Assign to an available team member immediately",
                                    "Clarify ownership during the daily standup"
                            ))
                            .build());
                } else {
                    for (User assignee : task.getAssignee()) {
                        String name = assignee.getName();
                        tasksPerAssignee.put(name, tasksPerAssignee.getOrDefault(name, 0) + 1);
                        incompletePerAssignee.put(name, incompletePerAssignee.getOrDefault(name, 0) + 1);
                    }
                }

                // Check planned end date
                if (task.getPlannedDateEnd() != null) {
                    if (task.getPlannedDateEnd().isBefore(now)) {
                        // Overdue
                        overdueTasks++;
                        boolean isHighPriority = "High".equalsIgnoreCase(task.getPriority());
                        if (isHighPriority) {
                            highPriorityOverdueTasks++;
                        }

                        findings.add(RiskFindingDto.builder()
                                .taskId(task.getId())
                                .taskTitle(task.getTitle())
                                .severity(isHighPriority ? RiskSeverity.CRITICAL : RiskSeverity.HIGH)
                                .riskType(isHighPriority ? "OVERDUE_HIGH_PRIORITY" : "OVERDUE")
                                .reason(String.format("Task is %s and overdue since %s.",
                                        task.getStatus(), DATE_FORMATTER.format(task.getPlannedDateEnd())))
                                .evidence(List.of(
                                        "Status: " + task.getStatus(),
                                        "Priority: " + task.getPriority(),
                                        "Planned End: " + DATE_FORMATTER.format(task.getPlannedDateEnd()),
                                        "Assignees: " + formatAssignees(task)
                                ))
                                .recommendedActions(List.of(
                                        "Investigate immediate blockers with assignees",
                                        "Re-estimate timeline or split task into smaller units"
                                ))
                                .build());
                    } else if (!task.getPlannedDateEnd().isAfter(dueSoonWindow)) {
                        // Due within 48 hours
                        dueSoonTasks++;
                        findings.add(RiskFindingDto.builder()
                                .taskId(task.getId())
                                .taskTitle(task.getTitle())
                                .severity(RiskSeverity.MEDIUM)
                                .riskType("DUE_SOON")
                                .reason("Task deadline is within 48 hours.")
                                .evidence(List.of(
                                        "Status: " + task.getStatus(),
                                        "Planned End: " + DATE_FORMATTER.format(task.getPlannedDateEnd()),
                                        "Assignees: " + formatAssignees(task)
                                ))
                                .recommendedActions(List.of(
                                        "Verify task progress in current standup",
                                        "Ensure testing/review resources are ready"
                                ))
                                .build());
                    }
                }
            }
        }

        // Sprint deadline check
        if (sprint.getEndDate() != null && incompleteTasks > 0) {
            if (sprint.getEndDate().isBefore(now)) {
                findings.add(RiskFindingDto.builder()
                        .severity(RiskSeverity.CRITICAL)
                        .riskType("SPRINT_OVERDUE")
                        .reason(String.format("Sprint ended on %s with %d incomplete tasks.",
                                DATE_FORMATTER.format(sprint.getEndDate()), incompleteTasks))
                        .evidence(List.of(
                                "Sprint End: " + DATE_FORMATTER.format(sprint.getEndDate()),
                                "Incomplete Tasks: " + incompleteTasks
                        ))
                        .recommendedActions(List.of(
                                "Perform sprint wrap-up and descope unfinished items",
                                "Plan remaining items into the next sprint"
                        ))
                        .build());
            } else if (!sprint.getEndDate().isAfter(dueSoonWindow)) {
                findings.add(RiskFindingDto.builder()
                        .severity(RiskSeverity.HIGH)
                        .riskType("SPRINT_ENDING_SOON")
                        .reason(String.format("Sprint ends within 48 hours (%s) with %d incomplete tasks.",
                                DATE_FORMATTER.format(sprint.getEndDate()), incompleteTasks))
                        .evidence(List.of(
                                "Sprint End: " + DATE_FORMATTER.format(sprint.getEndDate()),
                                "Incomplete Tasks: " + incompleteTasks
                        ))
                        .recommendedActions(List.of(
                                "Focus team efforts exclusively on closing in-flight tasks",
                                "De-prioritize non-essential items"
                        ))
                        .build());
            }
        }

        // Assignee overload check (> 4 incomplete tasks for one person in sprint)
        for (Map.Entry<String, Integer> entry : incompletePerAssignee.entrySet()) {
            if (entry.getValue() > 4) {
                findings.add(RiskFindingDto.builder()
                        .severity(RiskSeverity.MEDIUM)
                        .riskType("MEMBER_OVERLOAD")
                        .reason(String.format("Team member %s is assigned %d active tasks in this sprint.",
                                entry.getKey(), entry.getValue()))
                        .evidence(List.of(
                                "Assignee: " + entry.getKey(),
                                "Active Incomplete Tasks: " + entry.getValue()
                        ))
                        .recommendedActions(List.of(
                                "Reassign lower-priority tasks to balance team workload",
                                "Verify capacity during daily standup"
                        ))
                        .build());
            }
        }

        // Determine overall risk
        RiskSeverity overallRisk = calculateOverallRisk(findings);

        double completionRate = totalTasks > 0 ? (completedTasks * 100.0) / totalTasks : 0.0;
        SprintRiskMetricsDto metrics = SprintRiskMetricsDto.builder()
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .incompleteTasks(incompleteTasks)
                .overdueTasks(overdueTasks)
                .highPriorityOverdueTasks(highPriorityOverdueTasks)
                .dueSoonTasks(dueSoonTasks)
                .unassignedTasks(unassignedTasks)
                .completionRate(Math.round(completionRate * 100.0) / 100.0)
                .tasksPerAssignee(tasksPerAssignee)
                .incompleteTasksPerAssignee(incompletePerAssignee)
                .build();

        String summary = generateDeterministicSummary(sprint.getTitle(), metrics, overallRisk, findings);

        return SprintRiskResponse.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .sprintId(sprint.getId())
                .sprintTitle(sprint.getTitle())
                .sprintStatus(sprintStatus)
                .overallRisk(overallRisk)
                .summary(summary)
                .metrics(metrics)
                .findings(findings)
                .aiGenerated(false)
                .generatedAt(now)
                .build();
    }

    public boolean isTaskCompleted(Task task) {
        if (task == null || task.getStatus() == null) {
            return false;
        }
        String status = task.getStatus().trim();
        Project project = task.getProject();
        if (project != null && project.getStatuses() != null && !project.getStatuses().isEmpty()) {
            List<String> statuses = project.getStatuses();
            String terminalStatus = statuses.get(statuses.size() - 1);
            if (terminalStatus.equalsIgnoreCase(status)) {
                return true;
            }
        }
        return "Done".equalsIgnoreCase(status)
                || "Completed".equalsIgnoreCase(status)
                || "Closed".equalsIgnoreCase(status);
    }

    private RiskSeverity calculateOverallRisk(List<RiskFindingDto> findings) {
        long criticalCount = findings.stream().filter(f -> f.getSeverity() == RiskSeverity.CRITICAL).count();
        long highCount = findings.stream().filter(f -> f.getSeverity() == RiskSeverity.HIGH).count();
        long mediumCount = findings.stream().filter(f -> f.getSeverity() == RiskSeverity.MEDIUM).count();

        if (criticalCount > 0 || highCount >= 3) {
            return RiskSeverity.CRITICAL;
        }
        if (highCount > 0 || mediumCount >= 3) {
            return RiskSeverity.HIGH;
        }
        if (mediumCount > 0) {
            return RiskSeverity.MEDIUM;
        }
        return RiskSeverity.LOW;
    }

    private String generateDeterministicSummary(String sprintTitle, SprintRiskMetricsDto metrics, RiskSeverity overallRisk, List<RiskFindingDto> findings) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Sprint '%s' has an overall risk level of %s. ", sprintTitle, overallRisk));

        if (metrics.getOverdueTasks() > 0) {
            sb.append(String.format("%d task(s) are overdue", metrics.getOverdueTasks()));
            if (metrics.getHighPriorityOverdueTasks() > 0) {
                sb.append(String.format(" (%d high-priority)", metrics.getHighPriorityOverdueTasks()));
            }
            sb.append(". ");
        }

        if (metrics.getDueSoonTasks() > 0) {
            sb.append(String.format("%d task(s) are due within 48 hours. ", metrics.getDueSoonTasks()));
        }

        if (metrics.getUnassignedTasks() > 0) {
            sb.append(String.format("%d incomplete task(s) have no assignee. ", metrics.getUnassignedTasks()));
        }

        if (findings.isEmpty()) {
            sb.append(String.format("Sprint progress is healthy with %d of %d tasks completed (%.1f%%).",
                    metrics.getCompletedTasks(), metrics.getTotalTasks(), metrics.getCompletionRate()));
        }

        return sb.toString().trim();
    }

    private String formatAssignees(Task task) {
        if (task.getAssignee() == null || task.getAssignee().isEmpty()) {
            return "Unassigned";
        }
        return task.getAssignee().stream()
                .map(User::getName)
                .collect(Collectors.joining(", "));
    }
}
