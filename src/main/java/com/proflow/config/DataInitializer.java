package com.proflow.config;

import com.proflow.entity.User;
import com.proflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Seed default Root / Super Admin user if no super_admin exists
        String rootEmail = "root@proflows.in";
        if (userRepository.findByEmailIgnoreCase(rootEmail).isEmpty() && userRepository.findByRoleIgnoreCase("super_admin").isEmpty()) {
            User rootUser = User.builder()
                    .name("Root Administrator")
                    .email(rootEmail)
                    .password(passwordEncoder.encode("RootPassword@123"))
                    .role("super_admin")
                    .company(null) // Root user is platform-level, not tied to any company
                    .isActive(true)
                    .build();

            userRepository.save(rootUser);
            log.info("🌟 Initialized default Root Administrator: {} (Password: RootPassword@123)", rootEmail);
        }
    }
}
