package com.proflow.controller;

import com.proflow.dto.task.*;
import com.proflow.security.UserPrincipal;
import com.proflow.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management, status progression, comments, and file attachments")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Create a new task and notify assignee via email")
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        TaskResponse response = taskService.createTask(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get tasks filtered by project and user role")
    public ResponseEntity<List<TaskResponse>> getTasks(
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "assigneeId", required = false) Long assigneeId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        List<TaskResponse> tasks = taskService.getTasks(projectId, assigneeId, currentUser);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single task by ID with assignees, sprint, and comments")
    public ResponseEntity<TaskResponse> getTaskById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        TaskResponse task = taskService.getTaskById(id, currentUser);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task and send update email to assignee")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        TaskResponse task = taskService.updateTask(id, request, currentUser);
        return ResponseEntity.ok(task);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task and notify assignee via email")
    public ResponseEntity<Map<String, String>> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        taskService.deleteTask(id, currentUser);
        return ResponseEntity.ok(Map.of("message", "Task removed"));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add a comment to a task (assignee, reporter, or admin)")
    public ResponseEntity<Map<String, Object>> addComment(
            @PathVariable Long id,
            @Valid @RequestBody AddCommentRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        TaskCommentDto comment = taskService.addComment(id, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "comment", comment,
                "message", "Comment added successfully"
        ));
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an attachment to a task")
    public ResponseEntity<Map<String, Object>> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        TaskAttachmentDto attachment = taskService.uploadAttachment(id, file, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "attachment", attachment,
                "message", "File uploaded successfully"
        ));
    }
}
