package com.proflow.controller;

import com.proflow.dto.project.CreateProjectRequest;
import com.proflow.dto.project.ProjectAttachmentDto;
import com.proflow.dto.project.ProjectResponse;
import com.proflow.dto.project.UpdateProjectStatusesRequest;
import com.proflow.security.UserPrincipal;
import com.proflow.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project management and project attachments")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create a new project")
    public ResponseEntity<Map<String, Object>> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        ProjectResponse project = projectService.createProject(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "data", project,
                "message", "Project created successfully"
        ));
    }

    @GetMapping
    @Operation(summary = "Get all projects (admin gets all, employees get only their assigned projects)")
    public ResponseEntity<List<ProjectResponse>> getProjects(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        List<ProjectResponse> projects = projectService.getProjects(currentUser);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single project by ID")
    public ResponseEntity<ProjectResponse> getProjectById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        ProjectResponse project = projectService.getProjectById(id, currentUser);
        return ResponseEntity.ok(project);
    }

    @PutMapping("/{id}/statuses")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update project status workflow list")
    public ResponseEntity<Map<String, Object>> updateProjectStatuses(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectStatusesRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        ProjectResponse project = projectService.updateProjectStatuses(id, request, currentUser);
        return ResponseEntity.ok(Map.of(
                "message", "Project statuses updated successfully",
                "project", project
        ));
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Upload an attachment to a project")
    public ResponseEntity<Map<String, Object>> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("attachment") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        ProjectAttachmentDto attachment = projectService.uploadAttachment(id, file, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "File uploaded successfully",
                "attachment", attachment
        ));
    }

    @GetMapping("/{id}/attachments")
    @Operation(summary = "Get all attachments for a project")
    public ResponseEntity<List<ProjectAttachmentDto>> getProjectAttachments(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        List<ProjectAttachmentDto> attachments = projectService.getProjectAttachments(id, currentUser);
        return ResponseEntity.ok(attachments);
    }

    @GetMapping("/{projectId}/attachments/{filename}")
    @Operation(summary = "Download a project attachment")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long projectId,
            @PathVariable String filename,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Resource file = projectService.downloadAttachment(projectId, filename, currentUser);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    @DeleteMapping("/{projectId}/attachments/{filename}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete a project attachment")
    public ResponseEntity<Map<String, String>> deleteAttachment(
            @PathVariable Long projectId,
            @PathVariable String filename,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        projectService.deleteAttachment(projectId, filename, currentUser);
        return ResponseEntity.ok(Map.of("message", "Attachment deleted successfully"));
    }
}
