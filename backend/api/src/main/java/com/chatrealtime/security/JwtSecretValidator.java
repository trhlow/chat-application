package com.chatrealtime.security;

import org.springframework.core.env.Environment;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Validates JWT secret before {@link JwtTokenService} is constructed so startup fails with a clear message.
 */
public final class JwtSecretValidator {

    private JwtSecretValidator() {
    }

    public static boolean isRelaxedProfile(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equals("local") || profile.equals("test"));
    }

    /**
     * @throws IllegalStateException when secret is unsafe for non-relaxed profiles
     */
    public static void validate(Environment environment, JwtProperties jwtProperties) {
        String secret = jwtProperties.secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("APP_JWT_SECRET must be set (app.jwt.secret)");
        }
        if (isRelaxedProfile(environment)) {
            return;
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("APP_JWT_SECRET must encode to at least 32 bytes (256 bits) for HMAC-SHA256 for this Spring profile");
        }
        if (secret.toLowerCase().contains("change-this")) {
            throw new IllegalStateException("APP_JWT_SECRET must not use a known weak placeholder");
        }
    }
}
