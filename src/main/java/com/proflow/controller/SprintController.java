package com.proflow.controller;

import com.proflow.dto.sprint.CreateSprintRequest;
import com.proflow.dto.sprint.SprintIssueRequest;
import com.proflow.dto.sprint.SprintResponse;
import com.proflow.dto.sprint.UpdateSprintRequest;
import com.proflow.security.UserPrincipal;
import com.proflow.service.SprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sprints")
@RequiredArgsConstructor
@Tag(name = "Sprints", description = "Sprint planning, state transitions, and task backlog management")
public class SprintController {

    private final SprintService sprintService;

    @PostMapping
    @Operation(summary = "Create a new sprint")
    public ResponseEntity<SprintResponse> createSprint(
            @Valid @RequestBody CreateSprintRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        SprintResponse response = sprintService.createSprint(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all sprints for a project")
    public ResponseEntity<List<SprintResponse>> getSprints(
            @RequestParam("projectId") Long projectId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        List<SprintResponse> sprints = sprintService.getSprints(projectId, currentUser);
        return ResponseEntity.ok(sprints);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single sprint by ID")
    public ResponseEntity<SprintResponse> getSprintById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        SprintResponse sprint = sprintService.getSprintById(id, currentUser);
        return ResponseEntity.ok(sprint);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a sprint (goal, duration, dates, or status transitions: Created -> Started -> Completed)")
    public ResponseEntity<SprintResponse> updateSprint(
            @PathVariable Long id,
            @RequestBody UpdateSprintRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        SprintResponse response = sprintService.updateSprint(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a sprint and clear sprint references on tasks")
    public ResponseEntity<Map<String, String>> deleteSprint(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        sprintService.deleteSprint(id, currentUser);
        return ResponseEntity.ok(Map.of("message", "Sprint deleted"));
    }

    @PostMapping("/add-issue")
    @Operation(summary = "Add an issue/task to a sprint")
    public ResponseEntity<SprintResponse> addIssueToSprint(
            @Valid @RequestBody SprintIssueRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        SprintResponse response = sprintService.addIssueToSprint(request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/remove-issue")
    @Operation(summary = "Remove an issue/task from a sprint")
    public ResponseEntity<SprintResponse> removeIssueFromSprint(
            @Valid @RequestBody SprintIssueRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        SprintResponse response = sprintService.removeIssueFromSprint(request, currentUser);
        return ResponseEntity.ok(response);
    }
}
