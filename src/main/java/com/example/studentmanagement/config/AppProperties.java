package com.example.studentmanagement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Cors cors = new Cors();
    private BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();

    @Data
    public static class Cors {
        private String allowedOrigin;
    }

    @Data
    public static class BootstrapAdmin {
        private String username = "";
        private String email = "";
        private String password = "";
    }
}
