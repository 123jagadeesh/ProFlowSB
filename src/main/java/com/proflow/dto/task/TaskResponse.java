package com.proflow.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.proflow.dto.auth.UserSummaryDto;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private Map<String, Object> project;
    private Map<String, Object> parentTask;
    private Map<String, Object> sprint;
    private List<UserSummaryDto> assignee;
    private UserSummaryDto reporter;
    private String priority;
    private String status;
    private Instant plannedDateStart;
    private Instant plannedDateEnd;
    private List<TaskAttachmentDto> attachments;
    private List<TaskCommentDto> comments;
    private Long company;
    private Instant createdAt;
    private Instant updatedAt;

    @JsonProperty("_id")
    public String getUnderscoreId() {
        return id != null ? String.valueOf(id) : null;
    }
}
