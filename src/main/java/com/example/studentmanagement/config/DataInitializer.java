package com.example.studentmanagement.config;

import com.example.studentmanagement.user.Role;
import com.example.studentmanagement.user.User;
import com.example.studentmanagement.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final AppProperties appProperties;

    @Bean
    CommandLineRunner bootstrapAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String username = appProperties.getBootstrapAdmin().getUsername();
            String email = appProperties.getBootstrapAdmin().getEmail();
            String password = appProperties.getBootstrapAdmin().getPassword();
            if (username.isBlank() && email.isBlank() && password.isBlank()) {
                return;
            }
            if (username.isBlank() || email.isBlank() || password.isBlank()) {
                throw new IllegalStateException("All app.bootstrap-admin properties must be configured together");
            }
            if (!userRepository.existsByUsername(username) && !userRepository.existsByEmail(email)) {
                userRepository.save(User.builder()
                        .username(username)
                        .email(email.toLowerCase())
                        .password(passwordEncoder.encode(password))
                        .role(Role.ADMIN)
                        .build());
            }
        };
    }
}
