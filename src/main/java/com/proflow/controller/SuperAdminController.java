package com.proflow.controller;

import com.proflow.dto.auth.UserSummaryDto;
import com.proflow.dto.superadmin.CompanyDetailDto;
import com.proflow.dto.superadmin.CreateCompanyWithAdminRequest;
import com.proflow.dto.superadmin.PlatformStatsResponse;
import com.proflow.dto.superadmin.UpdateStatusRequest;
import com.proflow.service.SuperAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/super-admin")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ROOT')")
@RequiredArgsConstructor
@Tag(name = "Super Admin (Root)", description = "Platform-level operations for the Root User to oversee all companies and admins")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    @GetMapping("/companies")
    @Operation(summary = "Get all registered companies across the platform")
    public ResponseEntity<List<CompanyDetailDto>> getAllCompanies() {
        List<CompanyDetailDto> companies = superAdminService.getAllCompanies();
        return ResponseEntity.ok(companies);
    }

    @GetMapping("/admins")
    @Operation(summary = "Get all company administrators across the platform")
    public ResponseEntity<List<UserSummaryDto>> getAllAdmins() {
        List<UserSummaryDto> admins = superAdminService.getAllAdmins();
        return ResponseEntity.ok(admins);
    }

    @PostMapping("/companies")
    @Operation(summary = "Provision a new company and administrator account")
    public ResponseEntity<CompanyDetailDto> createCompanyWithAdmin(
            @Valid @RequestBody CreateCompanyWithAdminRequest request
    ) {
        CompanyDetailDto company = superAdminService.createCompanyWithAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(company);
    }

    @PatchMapping("/companies/{id}/status")
    @Operation(summary = "Activate or suspend a company")
    public ResponseEntity<Map<String, Object>> toggleCompanyStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        superAdminService.toggleCompanyStatus(id, request.getIsActive());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Company status updated successfully",
                "isActive", request.getIsActive()
        ));
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Activate or deactivate any user account (admin or employee)")
    public ResponseEntity<Map<String, Object>> toggleUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        superAdminService.toggleUserStatus(id, request.getIsActive());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User status updated successfully",
                "isActive", request.getIsActive()
        ));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get platform-wide statistics (companies, admins, employees, projects, tasks)")
    public ResponseEntity<PlatformStatsResponse> getPlatformStats() {
        PlatformStatsResponse stats = superAdminService.getPlatformStats();
        return ResponseEntity.ok(stats);
    }
}
