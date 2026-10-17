package com.proflow.dto.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.proflow.dto.auth.CompanySummaryDto;
import com.proflow.dto.auth.UserSummaryDto;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private String customer;
    private CompanySummaryDto company;
    private List<String> statuses;
    private List<ProjectAttachmentDto> attachments;
    private Boolean isActive;
    private Instant startDate;
    private Instant endDate;
    private UserSummaryDto createdBy;
    private UserSummaryDto updatedBy;
    private Instant createdAt;
    private Instant updatedAt;

    @JsonProperty("_id")
    public String getUnderscoreId() {
        return id != null ? String.valueOf(id) : null;
    }
}
