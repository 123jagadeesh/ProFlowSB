package com.proflow.controller;

import com.proflow.dto.auth.UserSummaryDto;
import com.proflow.dto.employee.CreateEmployeeRequest;
import com.proflow.dto.employee.EmployeeDto;
import com.proflow.security.UserPrincipal;
import com.proflow.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "Employee management and invitees")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create a new employee user in current company")
    public ResponseEntity<Map<String, Object>> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        EmployeeDto employee = employeeService.createEmployee(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Employee created successfully",
                "employee", employee
        ));
    }

    @GetMapping
    @Operation(summary = "Get all employees for the authenticated user's company")
    public ResponseEntity<Map<String, List<EmployeeDto>>> getEmployees(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        List<EmployeeDto> employees = employeeService.getEmployees(currentUser);
        return ResponseEntity.ok(Map.of("employees", employees));
    }

    @GetMapping("/zoom-invitees")
    @Operation(summary = "Get all company users except the current user for Zoom meeting invites")
    public ResponseEntity<List<UserSummaryDto>> getZoomInvitees(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        List<UserSummaryDto> invitees = employeeService.getZoomInvitees(currentUser);
        return ResponseEntity.ok(invitees);
    }
}
