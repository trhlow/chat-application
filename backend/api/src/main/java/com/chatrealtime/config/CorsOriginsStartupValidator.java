package com.chatrealtime.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Fail fast when production has no CORS/WebSocket origins configured.
 */
@Component
@Profile("prod")
@Order(0)
@RequiredArgsConstructor
public class CorsOriginsStartupValidator implements ApplicationRunner {
    private final AppCorsProperties appCorsProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (appCorsProperties.originList().isEmpty()) {
            throw new IllegalStateException(
                    "app.cors.allowed-origins must not be empty in prod (set CORS_ALLOWED_ORIGINS)"
            );
        }
    }
}
