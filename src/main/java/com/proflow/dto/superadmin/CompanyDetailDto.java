package com.proflow.dto.superadmin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyDetailDto {
    private Long id;
    private String name;
    private String location;
    private String industry;
    private String adminName;
    private String adminEmail;
    private Boolean isActive;
    private Long employeeCount;
    private Long projectCount;
    private Instant createdAt;

    @JsonProperty("_id")
    public String getUnderscoreId() {
        return id != null ? String.valueOf(id) : null;
    }
}
