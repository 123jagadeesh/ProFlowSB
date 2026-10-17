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
public class TaskAttachmentDto {
    private Long id;
    private String filename;
    private String path;
    private String fileType;
    private String fileSize;
    private UserSummaryDto uploadedBy;
    private Instant uploadedAt;

    @JsonProperty("_id")
    public String getUnderscoreId() {
        return id != null ? String.valueOf(id) : null;
    }
}
