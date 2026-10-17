package com.proflow.service;

import com.proflow.dto.auth.UserSummaryDto;
import com.proflow.dto.employee.CreateEmployeeRequest;
import com.proflow.dto.employee.EmployeeDto;
import com.proflow.entity.Company;
import com.proflow.entity.User;
import com.proflow.exception.BadRequestException;
import com.proflow.exception.ResourceNotFoundException;
import com.proflow.exception.UnauthorizedException;
import com.proflow.repository.CompanyRepository;
import com.proflow.repository.UserRepository;
import com.proflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public EmployeeDto createEmployee(CreateEmployeeRequest request, UserPrincipal currentUser) {
        if (!"admin".equalsIgnoreCase(currentUser.getRole()) && !"super_admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Only administrators can create employees.");
        }

        if (currentUser.getCompanyId() == null) {
            throw new BadRequestException("Company ID is missing");
        }

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        Company company = companyRepository.findById(currentUser.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        User employee = User.builder()
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("employee")
                .company(company)
                .isActive(true)
                .build();
        employee = userRepository.save(employee);

        return EmployeeDto.builder()
                .id(employee.getId())
                .name(employee.getName())
                .email(employee.getEmail())
                .role(employee.getRole())
                .company(company.getId())
                .build();
    }

    @Transactional(readOnly = true)
    public List<EmployeeDto> getEmployees(UserPrincipal currentUser) {
        if (currentUser.getCompanyId() == null) {
            return List.of();
        }

        return userRepository.findByCompanyIdAndRoleIgnoreCase(currentUser.getCompanyId(), "employee")
                .stream()
                .map(user -> EmployeeDto.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .company(currentUser.getCompanyId())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDto> getZoomInvitees(UserPrincipal currentUser) {
        if (currentUser.getCompanyId() == null) {
            return List.of();
        }

        return userRepository.findZoomInvitees(
                currentUser.getCompanyId(),
                currentUser.getId(),
                Arrays.asList("employee", "admin")
        ).stream().map(user -> UserSummaryDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build()
        ).collect(Collectors.toList());
    }
}
