package com.chatrealtime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.redis")
public record AppRedisProperties(
        boolean enabled,
        Duration presenceTtl,
        Channels channels,
        Cache cache
) {
    public record Channels(
            String presence,
            String notification
    ) {
    }

    public record Cache(
            Duration userPrincipalByIdTtl,
            Duration userPrincipalByUsernameTtl
    ) {
    }
}
