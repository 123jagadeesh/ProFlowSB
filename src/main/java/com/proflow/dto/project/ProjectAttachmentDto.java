package com.proflow.dto.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.proflow.dto.auth.UserSummaryDto;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectAttachmentDto {
    private Long id;
    private String filename;
    private String storedFilename;
    private String url;
    private String mimeType;
    private Long size;
    private String formattedSize;
    private String icon;
    private UserSummaryDto uploadedBy;
    private String description;
    private Instant uploadedAt;

    @JsonProperty("_id")
    public String getUnderscoreId() {
        return id != null ? String.valueOf(id) : null;
    }
}
