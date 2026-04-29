package com.chatrealtime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * Browser origins for REST CORS and WebSocket handshake (comma-separated exact URLs).
 * Override with env {@code CORS_ALLOWED_ORIGINS} in deployment.
 */
@ConfigurationProperties(prefix = "app.cors")
public class AppCorsProperties {
    /**
     * Comma-separated list, e.g. {@code https://app.example.com,https://www.example.com}.
     */
    private String allowedOrigins = "http://localhost:5173,http://127.0.0.1:5173,http://localhost:3000,http://127.0.0.1:3000";

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> originList() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return List.of();
        }
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public String[] originArray() {
        List<String> list = originList();
        return list.toArray(String[]::new);
    }
}
