package com.proflow.dto.sprint;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SprintIssueRequest {

    @NotNull(message = "Sprint ID is required")
    private Long sprintId;

    @NotNull(message = "Task ID is required")
    private Long taskId;
}
