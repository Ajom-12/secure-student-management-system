package com.example.studentmanagement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long expiration;
    private RefreshToken refreshToken = new RefreshToken();

    @Data
    public static class RefreshToken {
        private long expiration;
    }
}
