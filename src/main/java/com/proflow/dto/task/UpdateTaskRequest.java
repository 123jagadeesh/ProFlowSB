package com.proflow.dto.task;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTaskRequest {
    private String title;
    private String description;
    private List<Long> assignee;
    private String priority;
    private String status;
    private Instant plannedDateStart;
    private Instant plannedDateEnd;
    private Long sprint;
}
