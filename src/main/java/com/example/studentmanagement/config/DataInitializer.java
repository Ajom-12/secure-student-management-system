package com.example.studentmanagement.config;

import com.example.studentmanagement.user.Role;
import com.example.studentmanagement.user.User;
import com.example.studentmanagement.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Value("${app.bootstrap-admin.username:}")
    private String username;

    @Value("${app.bootstrap-admin.email:}")
    private String email;

    @Value("${app.bootstrap-admin.password:}")
    private String password;

    @Bean
    CommandLineRunner bootstrapAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
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
