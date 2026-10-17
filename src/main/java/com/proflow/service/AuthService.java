package com.proflow.service;

import com.proflow.config.ApplicationProperties;
import com.proflow.dto.auth.*;
import com.proflow.entity.Company;
import com.proflow.entity.User;
import com.proflow.exception.BadRequestException;
import com.proflow.exception.ResourceNotFoundException;
import com.proflow.repository.CompanyRepository;
import com.proflow.repository.UserRepository;
import com.proflow.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final ApplicationProperties applicationProperties;

    @Transactional
    public AuthResponse adminSignUp(AdminSignUpRequest request) {
        if (companyRepository.existsByNameIgnoreCase(request.getCompanyName())) {
            throw new BadRequestException("Company with this name already exists.");
        }

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException("Email is already registered.");
        }

        Company company = Company.builder()
                .name(request.getCompanyName().trim())
                .location(request.getCompanyLocation().trim())
                .adminName(request.getAdminName().trim())
                .adminEmail(request.getEmail().trim().toLowerCase())
                .isActive(true)
                .build();
        company = companyRepository.save(company);

        User admin = User.builder()
                .name(request.getAdminName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("admin")
                .company(company)
                .isActive(true)
                .build();
        admin = userRepository.save(admin);

        String token = jwtTokenProvider.generateToken(admin);

        UserSummaryDto userSummary = UserSummaryDto.builder()
                .id(admin.getId())
                .name(admin.getName())
                .email(admin.getEmail())
                .role(admin.getRole())
                .company(CompanySummaryDto.builder()
                        .id(company.getId())
                        .name(company.getName())
                        .build())
                .build();

        return AuthResponse.builder()
                .message("Company and admin registered successfully")
                .token(token)
                .user(userSummary)
                .build();
    }

    @Transactional
    public AuthResponse signIn(SignInRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new BadRequestException("Account is deactivated. Please contact support.");
        }

        // If user belongs to a company, ensure the company is active too
        if (user.getCompany() != null && Boolean.FALSE.equals(user.getCompany().getIsActive())) {
            throw new BadRequestException("Company account is suspended. Please contact administrator.");
        }

        user.setLastLogin(Instant.now());
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user);

        CompanySummaryDto companyDto = null;
        if (user.getCompany() != null) {
            companyDto = CompanySummaryDto.builder()
                    .id(user.getCompany().getId())
                    .name(user.getCompany().getName())
                    .build();
        }

        UserSummaryDto userSummary = UserSummaryDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .company(companyDto)
                .build();

        return AuthResponse.builder()
                .token(token)
                .user(userSummary)
                .build();
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String resetToken = jwtTokenProvider.generateResetPasswordToken(user);
        String resetUrl = applicationProperties.getClient().getUrl() + "/reset-password?token=" + resetToken;

        emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!jwtTokenProvider.validateToken(request.getToken())) {
            throw new BadRequestException("Invalid or expired token.");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(request.getToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Invalid or expired token."));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
