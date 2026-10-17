package com.proflow.dto.superadmin;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformStatsResponse {
    private long totalCompanies;
    private long totalAdmins;
    private long totalEmployees;
    private long totalProjects;
    private long totalTasks;
}
