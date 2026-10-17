package com.proflow.service;

import com.proflow.dto.sprint.CreateSprintRequest;
import com.proflow.dto.sprint.SprintIssueRequest;
import com.proflow.dto.sprint.SprintResponse;
import com.proflow.dto.sprint.UpdateSprintRequest;
import com.proflow.entity.Company;
import com.proflow.entity.Project;
import com.proflow.entity.Sprint;
import com.proflow.entity.Task;
import com.proflow.exception.BadRequestException;
import com.proflow.exception.ResourceNotFoundException;
import com.proflow.exception.UnauthorizedException;
import com.proflow.repository.CompanyRepository;
import com.proflow.repository.ProjectRepository;
import com.proflow.repository.SprintRepository;
import com.proflow.repository.TaskRepository;
import com.proflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final CompanyRepository companyRepository;
    private final TaskRepository taskRepository;

    private boolean isAdminOrReporter(UserPrincipal user) {
        String role = user.getRole();
        return "admin".equalsIgnoreCase(role) || "reporter".equalsIgnoreCase(role) || "super_admin".equalsIgnoreCase(role);
    }

    @Transactional
    public SprintResponse createSprint(CreateSprintRequest request, UserPrincipal currentUser) {
        if (!isAdminOrReporter(currentUser)) {
            throw new UnauthorizedException("Only admin or reporter can create sprints");
        }

        Project project = projectRepository.findById(request.getProject())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.getCompany().getId().equals(currentUser.getCompanyId()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Project not found or unauthorized");
        }

        Company company = companyRepository.findById(currentUser.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        Sprint sprint = Sprint.builder()
                .title(request.getTitle().trim())
                .goal(request.getGoal())
                .duration(request.getDuration())
                .project(project)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .company(company)
                .status("Created")
                .build();

        sprint = sprintRepository.save(sprint);
        return mapToResponse(sprint);
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> getSprints(Long projectId, UserPrincipal currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.getCompany().getId().equals(currentUser.getCompanyId()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Project not found or unauthorized");
        }

        return sprintRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SprintResponse getSprintById(Long id, UserPrincipal currentUser) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found"));

        if (!sprint.getCompany().getId().equals(currentUser.getCompanyId()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Sprint not found or unauthorized");
        }

        return mapToResponse(sprint);
    }

    @Transactional
    public SprintResponse updateSprint(Long id, UpdateSprintRequest request, UserPrincipal currentUser) {
        if (!isAdminOrReporter(currentUser)) {
            throw new UnauthorizedException("Only admin or reporter can update sprints");
        }

        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found"));

        if (!sprint.getCompany().getId().equals(currentUser.getCompanyId()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Sprint not found or unauthorized");
        }

        // Validate allowed status transitions: Created -> Started -> Completed
        if (request.getStatus() != null && !request.getStatus().equalsIgnoreCase(sprint.getStatus())) {
            boolean allowed = ("Created".equalsIgnoreCase(sprint.getStatus()) && "Started".equalsIgnoreCase(request.getStatus()))
                    || ("Started".equalsIgnoreCase(sprint.getStatus()) && "Completed".equalsIgnoreCase(request.getStatus()));

            if (!allowed) {
                throw new BadRequestException("Invalid status transition");
            }
            sprint.setStatus(request.getStatus());
        }

        if (request.getTitle() != null) sprint.setTitle(request.getTitle().trim());
        if (request.getGoal() != null) sprint.setGoal(request.getGoal());
        if (request.getDuration() != null) sprint.setDuration(request.getDuration());
        if (request.getStartDate() != null) sprint.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) sprint.setEndDate(request.getEndDate());

        sprint = sprintRepository.save(sprint);
        return mapToResponse(sprint);
    }

    @Transactional
    public void deleteSprint(Long id, UserPrincipal currentUser) {
        if (!isAdminOrReporter(currentUser)) {
            throw new UnauthorizedException("Only admin or reporter can delete sprints");
        }

        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found"));

        if (!sprint.getCompany().getId().equals(currentUser.getCompanyId()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Unauthorized");
        }

        taskRepository.clearSprintReference(id);
        sprintRepository.delete(sprint);
    }

    @Transactional
    public SprintResponse addIssueToSprint(SprintIssueRequest request, UserPrincipal currentUser) {
        Sprint sprint = sprintRepository.findById(request.getSprintId())
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found"));

        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        task.setSprint(sprint);
        taskRepository.save(task);

        if (!sprint.getIssues().contains(task)) {
            sprint.getIssues().add(task);
        }

        return mapToResponse(sprint);
    }

    @Transactional
    public SprintResponse removeIssueFromSprint(SprintIssueRequest request, UserPrincipal currentUser) {
        Sprint sprint = sprintRepository.findById(request.getSprintId())
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found"));

        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        task.setSprint(null);
        taskRepository.save(task);

        sprint.getIssues().removeIf(t -> t.getId().equals(request.getTaskId()));

        return mapToResponse(sprint);
    }

    private SprintResponse mapToResponse(Sprint sprint) {
        List<Object> issueIds = sprint.getIssues() != null
                ? sprint.getIssues().stream().map(Task::getId).collect(Collectors.toList())
                : new ArrayList<>();

        return SprintResponse.builder()
                .id(sprint.getId())
                .title(sprint.getTitle())
                .name(sprint.getTitle())
                .goal(sprint.getGoal())
                .duration(sprint.getDuration())
                .project(sprint.getProject().getId())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .status(sprint.getStatus())
                .company(sprint.getCompany().getId())
                .issues(issueIds)
                .createdAt(sprint.getCreatedAt())
                .build();
    }
}
