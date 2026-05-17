package com.marketplace.config;

import com.marketplace.entity.User;
import com.marketplace.enums.Role;
import com.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AdminInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public org.springframework.boot.CommandLineRunner createAdmin() {
        return args -> {

            String email = "admin@marchi.tn";

            if (!userRepository.existsByEmail(email)) {
                User admin = new User();
                admin.setNom("Super Admin");
                admin.setEmail(email);
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(Role.ADMIN);
                admin.setEnabled(Boolean.TRUE);

                userRepository.save(admin);

                System.out.println("✅ Admin account created: " + email);
            } else {
                System.out.println("ℹ️ Admin already exists");
            }
        };
    }
}
