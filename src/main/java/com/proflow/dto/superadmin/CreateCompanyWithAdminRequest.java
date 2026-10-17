package com.proflow.dto.superadmin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCompanyWithAdminRequest {

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Company location is required")
    private String companyLocation;

    private String industry;

    @NotBlank(message = "Admin name is required")
    private String adminName;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Please enter a valid email address")
    private String adminEmail;

    @NotBlank(message = "Admin password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String adminPassword;
}
