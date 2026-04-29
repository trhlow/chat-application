package com.chatrealtime.security;

import org.springframework.core.env.Environment;

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
        if (secret.length() < 32) {
            throw new IllegalStateException("APP_JWT_SECRET must be at least 32 characters for this Spring profile");
        }
        if (secret.toLowerCase().contains("change-this")) {
            throw new IllegalStateException("APP_JWT_SECRET must not use a known weak placeholder");
        }
    }
}
