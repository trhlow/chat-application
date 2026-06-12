package com.chatrealtime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.refresh-cookie")
public record AuthCookieProperties(
        String name,
        String path,
        String sameSite,
        boolean secure
) {
    public AuthCookieProperties {
        if (name == null || name.isBlank()) {
            name = "refreshToken";
        }
        if (path == null || path.isBlank()) {
            path = "/api/auth";
        }
        if (sameSite == null || sameSite.isBlank()) {
            sameSite = "Strict";
        }
    }
}
