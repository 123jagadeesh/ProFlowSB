package com.proflow.service;

import com.proflow.dto.auth.UserSummaryDto;
import com.proflow.dto.task.*;
import com.proflow.entity.*;
import com.proflow.exception.BadRequestException;
import com.proflow.exception.ResourceNotFoundException;
import com.proflow.exception.UnauthorizedException;
import com.proflow.repository.*;
import com.proflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final SprintRepository sprintRepository;
    private final TaskCommentRepository commentRepository;
    private final TaskAttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;

    private boolean isAdminOrReporter(UserPrincipal user) {
        String role = user.getRole();
        return "admin".equalsIgnoreCase(role) || "reporter".equalsIgnoreCase(role) || "super_admin".equalsIgnoreCase(role);
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, UserPrincipal currentUser) {
        if (!isAdminOrReporter(currentUser)) {
            throw new UnauthorizedException("Only admin or reporter can create tasks");
        }

        Project project = projectRepository.findById(request.getProject())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.getCompany().getId().equals(currentUser.getCompanyId()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Project not found or unauthorized");
        }

        String taskStatus = request.getStatus() != null && !request.getStatus().isBlank() ? request.getStatus() : "Todo";
        if (project.getStatuses() != null && !project.getStatuses().isEmpty() && !project.getStatuses().contains(taskStatus)) {
            throw new BadRequestException(String.format("Invalid status: %s. Must be one of %s", taskStatus, project.getStatuses()));
        }

        Set<User> assignees = new HashSet<>();
        if (request.getAssignee() != null && !request.getAssignee().isEmpty()) {
            for (Long assigneeId : request.getAssignee()) {
                User assignee = userRepository.findById(assigneeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Assignee with ID " + assigneeId + " not found"));
                if (!assignee.getCompany().getId().equals(currentUser.getCompanyId()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
                    throw new UnauthorizedException("Assignee with ID " + assigneeId + " not authorized");
                }
                assignees.add(assignee);
            }
        }

        Task parentTask = null;
        if (request.getParentTask() != null) {
            parentTask = taskRepository.findById(request.getParentTask())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent task not found"));
        }

        Sprint sprint = null;
        if (request.getSprint() != null) {
            sprint = sprintRepository.findById(request.getSprint())
                    .orElseThrow(() -> new ResourceNotFoundException("Sprint not found"));
        }

        User reporter = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Reporter user not found"));

        Task task = Task.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .project(project)
                .parentTask(parentTask)
                .sprint(sprint)
                .assignee(assignees)
                .reporter(reporter)
                .priority(request.getPriority() != null ? request.getPriority() : "Low")
                .status(taskStatus)
                .plannedDateStart(request.getPlannedDateStart())
                .plannedDateEnd(request.getPlannedDateEnd())
                .company(project.getCompany())
                .build();

        task = taskRepository.save(task);

        // Trigger email notification to assignee(s)
        if (!assignees.isEmpty()) {
            User firstAssignee = assignees.iterator().next();
            String companyName = project.getCompany().getName();
            emailService.sendTaskAssignedEmail(firstAssignee.getEmail(), firstAssignee.getName(), task.getTitle(), companyName);
        }

        return mapToResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(Long projectId, Long assigneeIdFilter, UserPrincipal currentUser) {
        if (currentUser.getCompanyId() == null) {
            return List.of();
        }

        Long filterAssigneeId = null;
        if (!"admin".equalsIgnoreCase(currentUser.getRole()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            // Non-admins only see tasks assigned to them
            filterAssigneeId = currentUser.getId();
        } else if (assigneeIdFilter != null) {
            filterAssigneeId = assigneeIdFilter;
        }

        List<Task> tasks = taskRepository.findTasksWithFilter(currentUser.getCompanyId(), projectId, filterAssigneeId);
        return tasks.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id, UserPrincipal currentUser) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (!task.getCompany().getId().equals(currentUser.getCompanyId()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Task not found or unauthorized");
        }

        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(Long id, UpdateTaskRequest request, UserPrincipal currentUser) {
        if (!isAdminOrReporter(currentUser)) {
            throw new UnauthorizedException("Only admin or reporter can update tasks");
        }

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (!task.getCompany().getId().equals(currentUser.getCompanyId()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Task not found or unauthorized");
        }

        if (request.getStatus() != null) {
            Project project = task.getProject();
            if (project.getStatuses() != null && !project.getStatuses().isEmpty() && !project.getStatuses().contains(request.getStatus())) {
                throw new BadRequestException(String.format("Invalid status: %s. Must be one of %s", request.getStatus(), project.getStatuses()));
            }
            task.setStatus(request.getStatus());
        }

        if (request.getTitle() != null) task.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getPlannedDateStart() != null) task.setPlannedDateStart(request.getPlannedDateStart());
        if (request.getPlannedDateEnd() != null) task.setPlannedDateEnd(request.getPlannedDateEnd());

        if (request.getSprint() != null) {
            Sprint sprint = sprintRepository.findById(request.getSprint())
                    .orElseThrow(() -> new ResourceNotFoundException("Sprint not found"));
            task.setSprint(sprint);
        }

        if (request.getAssignee() != null) {
            Set<User> newAssignees = new HashSet<>();
            for (Long assigneeId : request.getAssignee()) {
                User user = userRepository.findById(assigneeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Assignee with ID " + assigneeId + " not found"));
                newAssignees.add(user);
            }
            task.setAssignee(newAssignees);
        }

        task = taskRepository.save(task);

        // Notify assignee about update
        if (!task.getAssignee().isEmpty()) {
            User firstAssignee = task.getAssignee().iterator().next();
            String companyName = task.getCompany().getName();
            emailService.sendTaskUpdatedEmail(firstAssignee.getEmail(), firstAssignee.getName(), task.getTitle(), companyName);
        }

        return mapToResponse(task);
    }

    @Transactional
    public void deleteTask(Long id, UserPrincipal currentUser) {
        if (!isAdminOrReporter(currentUser)) {
            throw new UnauthorizedException("Only admin or reporter can delete tasks");
        }

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (!task.getCompany().getId().equals(currentUser.getCompanyId()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Unauthorized");
        }

        List<User> assignees = new ArrayList<>(task.getAssignee());
        String taskTitle = task.getTitle();
        String companyName = task.getCompany().getName();

        taskRepository.delete(task);

        // Notify assignees about deletion
        if (!assignees.isEmpty()) {
            User firstAssignee = assignees.get(0);
            emailService.sendTaskDeletedEmail(firstAssignee.getEmail(), firstAssignee.getName(), taskTitle, companyName);
        }
    }

    @Transactional
    public TaskCommentDto addComment(Long taskId, AddCommentRequest request, UserPrincipal currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        boolean isAssignee = task.getAssignee().stream().anyMatch(a -> a.getId().equals(currentUser.getId()));
        boolean isReporter = task.getReporter().getId().equals(currentUser.getId());
        boolean isAdmin = "admin".equalsIgnoreCase(currentUser.getRole()) || "super_admin".equalsIgnoreCase(currentUser.getRole());

        if (!isAssignee && !isReporter && !isAdmin) {
            throw new UnauthorizedException("Not authorized to comment on this task");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TaskComment comment = TaskComment.builder()
                .task(task)
                .user(user)
                .message(request.getMessage().trim())
                .build();

        comment = commentRepository.save(comment);

        return TaskCommentDto.builder()
                .id(comment.getId())
                .user(UserSummaryDto.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .build())
                .message(comment.getMessage())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    @Transactional
    public TaskAttachmentDto uploadAttachment(Long taskId, MultipartFile file, UserPrincipal currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        boolean isAssignee = task.getAssignee().stream().anyMatch(a -> a.getId().equals(currentUser.getId()));
        boolean isReporter = task.getReporter().getId().equals(currentUser.getId());
        boolean isAdmin = "admin".equalsIgnoreCase(currentUser.getRole()) || "super_admin".equalsIgnoreCase(currentUser.getRole());

        if (!isAssignee && !isReporter && !isAdmin) {
            throw new UnauthorizedException("Not authorized to upload attachments to this task");
        }

        User uploader = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        FileStorageService.StoredFile storedFile = fileStorageService.storeFile(file, false);

        String originalFilename = storedFile.originalFilename();
        String fileExtension = "";
        int dotIdx = originalFilename.lastIndexOf('.');
        if (dotIdx > 0) fileExtension = originalFilename.substring(dotIdx + 1).toLowerCase();

        String fileSizeInMb = String.format("%.2f", storedFile.size() / (1024.0 * 1024.0));

        TaskAttachment attachment = TaskAttachment.builder()
                .task(task)
                .filename(originalFilename)
                .path(storedFile.path().replace("\\", "/"))
                .fileType(fileExtension)
                .fileSize(fileSizeInMb)
                .uploadedBy(uploader)
                .build();

        attachment = attachmentRepository.save(attachment);

        return TaskAttachmentDto.builder()
                .id(attachment.getId())
                .filename(attachment.getFilename())
                .path(attachment.getPath())
                .fileType(attachment.getFileType())
                .fileSize(attachment.getFileSize())
                .uploadedBy(UserSummaryDto.builder()
                        .id(uploader.getId())
                        .name(uploader.getName())
                        .email(uploader.getEmail())
                        .build())
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }

    private TaskResponse mapToResponse(Task task) {
        Map<String, Object> projectMap = Map.of(
                "_id", task.getProject().getId(),
                "id", task.getProject().getId(),
                "name", task.getProject().getName()
        );

        Map<String, Object> parentMap = null;
        if (task.getParentTask() != null) {
            parentMap = Map.of(
                    "_id", task.getParentTask().getId(),
                    "id", task.getParentTask().getId(),
                    "title", task.getParentTask().getTitle()
            );
        }

        Map<String, Object> sprintMap = null;
        if (task.getSprint() != null) {
            sprintMap = Map.of(
                    "_id", task.getSprint().getId(),
                    "id", task.getSprint().getId(),
                    "title", task.getSprint().getTitle()
            );
        }

        List<UserSummaryDto> assignees = task.getAssignee().stream()
                .map(a -> UserSummaryDto.builder()
                        .id(a.getId())
                        .name(a.getName())
                        .email(a.getEmail())
                        .role(a.getRole())
                        .build())
                .collect(Collectors.toList());

        UserSummaryDto reporterDto = task.getReporter() != null ? UserSummaryDto.builder()
                .id(task.getReporter().getId())
                .name(task.getReporter().getName())
                .email(task.getReporter().getEmail())
                .role(task.getReporter().getRole())
                .build() : null;

        List<TaskAttachmentDto> attachmentDtos = task.getAttachments() != null
                ? task.getAttachments().stream().map(a -> TaskAttachmentDto.builder()
                .id(a.getId())
                .filename(a.getFilename())
                .path(a.getPath())
                .fileType(a.getFileType())
                .fileSize(a.getFileSize())
                .uploadedBy(a.getUploadedBy() != null ? UserSummaryDto.builder()
                        .id(a.getUploadedBy().getId())
                        .name(a.getUploadedBy().getName())
                        .email(a.getUploadedBy().getEmail())
                        .build() : null)
                .uploadedAt(a.getUploadedAt())
                .build()).collect(Collectors.toList())
                : List.of();

        List<TaskCommentDto> commentDtos = task.getComments() != null
                ? task.getComments().stream().map(c -> TaskCommentDto.builder()
                .id(c.getId())
                .message(c.getMessage())
                .user(c.getUser() != null ? UserSummaryDto.builder()
                        .id(c.getUser().getId())
                        .name(c.getUser().getName())
                        .email(c.getUser().getEmail())
                        .build() : null)
                .createdAt(c.getCreatedAt())
                .build()).collect(Collectors.toList())
                : List.of();

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .project(projectMap)
                .parentTask(parentMap)
                .sprint(sprintMap)
                .assignee(assignees)
                .reporter(reporterDto)
                .priority(task.getPriority())
                .status(task.getStatus())
                .plannedDateStart(task.getPlannedDateStart())
                .plannedDateEnd(task.getPlannedDateEnd())
                .attachments(attachmentDtos)
                .comments(commentDtos)
                .company(task.getCompany().getId())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
