package com.chatrealtime.security;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        @Positive long accessExpirationMs,
        @Positive long refreshExpirationMs
) {
}

