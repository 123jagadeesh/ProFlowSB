package com.proflow.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTaskRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Project ID is required")
    private Long project;

    private Long parentTask;

    private List<Long> assignee;

    private String priority; // Low, Medium, High

    private String status; // Todo, In Progress, Done, etc.

    private Instant plannedDateStart;

    private Instant plannedDateEnd;

    private Long sprint;
}
