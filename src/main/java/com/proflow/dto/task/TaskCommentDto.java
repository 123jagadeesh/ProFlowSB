package com.proflow.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.proflow.dto.auth.UserSummaryDto;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskCommentDto {
    private Long id;
    private UserSummaryDto user;
    private String message;
    private Instant createdAt;

    @JsonProperty("_id")
    public String getUnderscoreId() {
        return id != null ? String.valueOf(id) : null;
    }
}
