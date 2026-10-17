package com.proflow.service;

import com.proflow.dto.auth.CompanySummaryDto;
import com.proflow.dto.auth.UserSummaryDto;
import com.proflow.dto.project.CreateProjectRequest;
import com.proflow.dto.project.ProjectAttachmentDto;
import com.proflow.dto.project.ProjectResponse;
import com.proflow.dto.project.UpdateProjectStatusesRequest;
import com.proflow.entity.Company;
import com.proflow.entity.Project;
import com.proflow.entity.ProjectAttachment;
import com.proflow.entity.Task;
import com.proflow.entity.User;
import com.proflow.exception.BadRequestException;
import com.proflow.exception.ResourceNotFoundException;
import com.proflow.exception.UnauthorizedException;
import com.proflow.repository.CompanyRepository;
import com.proflow.repository.ProjectAttachmentRepository;
import com.proflow.repository.ProjectRepository;
import com.proflow.repository.TaskRepository;
import com.proflow.repository.UserRepository;
import com.proflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectAttachmentRepository attachmentRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, UserPrincipal currentUser) {
        if (!"admin".equalsIgnoreCase(currentUser.getRole()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Access denied. Only administrators can create projects.");
        }

        if (request.getStartDate() != null && request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }

        Company company = companyRepository.findById(currentUser.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<String> statuses = (request.getStatuses() != null && !request.getStatuses().isEmpty())
                ? request.getStatuses()
                : List.of("Todo", "In Progress", "Done");

        Project project = Project.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .customer(request.getCustomer())
                .company(company)
                .statuses(new ArrayList<>(statuses))
                .startDate(request.getStartDate() != null ? request.getStartDate() : Instant.now())
                .endDate(request.getEndDate())
                .createdBy(user)
                .isActive(true)
                .build();

        project = projectRepository.save(project);
        return mapToResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjects(UserPrincipal currentUser) {
        if (currentUser.getCompanyId() == null) {
            return List.of();
        }

        List<Project> projects;
        if ("admin".equalsIgnoreCase(currentUser.getRole()) || "super_admin".equalsIgnoreCase(currentUser.getRole())) {
            projects = projectRepository.findByCompanyIdOrderByCreatedAtDesc(currentUser.getCompanyId());
        } else {
            projects = projectRepository.findProjectsForEmployee(currentUser.getId(), currentUser.getCompanyId());
        }

        return projects.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id, UserPrincipal currentUser) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.getCompany().getId().equals(currentUser.getCompanyId()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Access denied. This project does not belong to your company.");
        }

        if (!"admin".equalsIgnoreCase(currentUser.getRole()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            boolean hasTasks = taskRepository.existsByProjectIdAndAssigneeIdAndCompanyId(id, currentUser.getId(), currentUser.getCompanyId());
            if (!hasTasks) {
                throw new UnauthorizedException("Access denied. You do not have any tasks in this project.");
            }
        }

        return mapToResponse(project);
    }

    @Transactional
    public ProjectResponse updateProjectStatuses(Long id, UpdateProjectStatusesRequest request, UserPrincipal currentUser) {
        if (!"admin".equalsIgnoreCase(currentUser.getRole()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Only administrators can update project statuses.");
        }

        Project project = projectRepository.findByIdAndCompanyId(id, currentUser.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found or unauthorized"));

        List<String> newStatuses = request.getStatuses();
        project.setStatuses(new ArrayList<>(newStatuses));
        project = projectRepository.save(project);

        // Cascade update task status if current task status is no longer valid
        String defaultStatus = newStatuses.get(0);
        List<Task> tasks = taskRepository.findByProjectId(id);
        for (Task task : tasks) {
            if (!newStatuses.contains(task.getStatus())) {
                task.setStatus(defaultStatus);
                taskRepository.save(task);
            }
        }

        return mapToResponse(project);
    }

    @Transactional
    public ProjectAttachmentDto uploadAttachment(Long projectId, MultipartFile file, UserPrincipal currentUser) {
        if (!"admin".equalsIgnoreCase(currentUser.getRole()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Not authorized to upload attachment");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.getCompany().getId().equals(currentUser.getCompanyId()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Access denied to this project");
        }

        User uploader = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        FileStorageService.StoredFile storedFile = fileStorageService.storeFile(file, true);

        String downloadUrl = "/api/projects/" + project.getId() + "/attachments/" + storedFile.storedFilename();

        ProjectAttachment attachment = ProjectAttachment.builder()
                .project(project)
                .filename(storedFile.originalFilename())
                .storedFilename(storedFile.storedFilename())
                .url(downloadUrl)
                .mimeType(storedFile.mimeType())
                .size(storedFile.size())
                .uploadedBy(uploader)
                .uploadedAt(Instant.now())
                .build();

        attachment = attachmentRepository.save(attachment);
        project.getAttachments().add(attachment);

        return mapToAttachmentDto(attachment);
    }

    @Transactional(readOnly = true)
    public List<ProjectAttachmentDto> getProjectAttachments(Long projectId, UserPrincipal currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.getCompany().getId().equals(currentUser.getCompanyId()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Not authorized to view these attachments");
        }

        return attachmentRepository.findByProjectIdOrderByUploadedAtDesc(projectId)
                .stream()
                .map(this::mapToAttachmentDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Resource downloadAttachment(Long projectId, String filename, UserPrincipal currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.getCompany().getId().equals(currentUser.getCompanyId()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Not authorized to access this file");
        }

        ProjectAttachment attachment = attachmentRepository.findByProjectIdAndStoredFilename(projectId, filename)
                .orElseThrow(() -> new ResourceNotFoundException("File not found in project attachments"));

        return fileStorageService.loadFileAsResource(attachment.getStoredFilename());
    }

    @Transactional
    public void deleteAttachment(Long projectId, String filename, UserPrincipal currentUser) {
        if (!"admin".equalsIgnoreCase(currentUser.getRole()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Not authorized to delete attachments");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.getCompany().getId().equals(currentUser.getCompanyId()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Access denied");
        }

        ProjectAttachment attachment = attachmentRepository.findByProjectIdAndStoredFilename(projectId, filename)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        attachmentRepository.delete(attachment);
        fileStorageService.deleteFile(filename);
    }

    private ProjectResponse mapToResponse(Project project) {
        CompanySummaryDto companyDto = CompanySummaryDto.builder()
                .id(project.getCompany().getId())
                .name(project.getCompany().getName())
                .build();

        UserSummaryDto createdByDto = project.getCreatedBy() != null ? UserSummaryDto.builder()
                .id(project.getCreatedBy().getId())
                .name(project.getCreatedBy().getName())
                .email(project.getCreatedBy().getEmail())
                .build() : null;

        UserSummaryDto updatedByDto = project.getUpdatedBy() != null ? UserSummaryDto.builder()
                .id(project.getUpdatedBy().getId())
                .name(project.getUpdatedBy().getName())
                .email(project.getUpdatedBy().getEmail())
                .build() : null;

        List<ProjectAttachmentDto> attachmentDtos = project.getAttachments() != null
                ? project.getAttachments().stream().map(this::mapToAttachmentDto).collect(Collectors.toList())
                : List.of();

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .customer(project.getCustomer())
                .company(companyDto)
                .statuses(project.getStatuses())
                .attachments(attachmentDtos)
                .isActive(project.getIsActive())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .createdBy(createdByDto)
                .updatedBy(updatedByDto)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private ProjectAttachmentDto mapToAttachmentDto(ProjectAttachment attachment) {
        UserSummaryDto uploadedByDto = attachment.getUploadedBy() != null ? UserSummaryDto.builder()
                .id(attachment.getUploadedBy().getId())
                .name(attachment.getUploadedBy().getName())
                .email(attachment.getUploadedBy().getEmail())
                .build() : null;

        return ProjectAttachmentDto.builder()
                .id(attachment.getId())
                .filename(attachment.getFilename())
                .storedFilename(attachment.getStoredFilename())
                .url(attachment.getUrl())
                .mimeType(attachment.getMimeType())
                .size(attachment.getSize())
                .formattedSize(attachment.getFormattedSize())
                .uploadedBy(uploadedByDto)
                .description(attachment.getDescription())
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }
}
