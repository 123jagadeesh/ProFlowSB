package com.proflow.service;

import com.proflow.dto.auth.UserSummaryDto;
import com.proflow.dto.superadmin.CompanyDetailDto;
import com.proflow.dto.superadmin.CreateCompanyWithAdminRequest;
import com.proflow.dto.superadmin.PlatformStatsResponse;
import com.proflow.entity.Company;
import com.proflow.entity.User;
import com.proflow.exception.BadRequestException;
import com.proflow.exception.ResourceNotFoundException;
import com.proflow.repository.CompanyRepository;
import com.proflow.repository.ProjectRepository;
import com.proflow.repository.TaskRepository;
import com.proflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<CompanyDetailDto> getAllCompanies() {
        return companyRepository.findAll().stream().map(company -> {
            long employeeCount = userRepository.countByCompanyId(company.getId());
            long projectCount = projectRepository.countByCompanyId(company.getId());

            return CompanyDetailDto.builder()
                    .id(company.getId())
                    .name(company.getName())
                    .location(company.getLocation())
                    .industry(company.getIndustry())
                    .adminName(company.getAdminName())
                    .adminEmail(company.getAdminEmail())
                    .isActive(company.getIsActive())
                    .employeeCount(employeeCount)
                    .projectCount(projectCount)
                    .createdAt(company.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDto> getAllAdmins() {
        return userRepository.findByRoleIgnoreCase("admin").stream()
                .map(user -> UserSummaryDto.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public CompanyDetailDto createCompanyWithAdmin(CreateCompanyWithAdminRequest request) {
        if (companyRepository.existsByNameIgnoreCase(request.getCompanyName())) {
            throw new BadRequestException("Company with this name already exists.");
        }

        if (userRepository.existsByEmailIgnoreCase(request.getAdminEmail())) {
            throw new BadRequestException("Admin email is already registered.");
        }

        Company company = Company.builder()
                .name(request.getCompanyName().trim())
                .location(request.getCompanyLocation().trim())
                .industry(request.getIndustry())
                .adminName(request.getAdminName().trim())
                .adminEmail(request.getAdminEmail().trim().toLowerCase())
                .isActive(true)
                .build();
        company = companyRepository.save(company);

        User admin = User.builder()
                .name(request.getAdminName().trim())
                .email(request.getAdminEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getAdminPassword()))
                .role("admin")
                .company(company)
                .isActive(true)
                .build();
        userRepository.save(admin);

        return CompanyDetailDto.builder()
                .id(company.getId())
                .name(company.getName())
                .location(company.getLocation())
                .industry(company.getIndustry())
                .adminName(company.getAdminName())
                .adminEmail(company.getAdminEmail())
                .isActive(company.getIsActive())
                .employeeCount(1L)
                .projectCount(0L)
                .createdAt(company.getCreatedAt())
                .build();
    }

    @Transactional
    public void toggleCompanyStatus(Long companyId, boolean isActive) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        company.setIsActive(isActive);
        companyRepository.save(company);
        log.info("Company {} active status set to {}", company.getName(), isActive);
    }

    @Transactional
    public void toggleUserStatus(Long userId, boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setIsActive(isActive);
        userRepository.save(user);
        log.info("User {} active status set to {}", user.getEmail(), isActive);
    }

    @Transactional(readOnly = true)
    public PlatformStatsResponse getPlatformStats() {
        return PlatformStatsResponse.builder()
                .totalCompanies(companyRepository.count())
                .totalAdmins(userRepository.countByRoleIgnoreCase("admin"))
                .totalEmployees(userRepository.countByRoleIgnoreCase("employee"))
                .totalProjects(projectRepository.count())
                .totalTasks(taskRepository.count())
                .build();
    }
}
