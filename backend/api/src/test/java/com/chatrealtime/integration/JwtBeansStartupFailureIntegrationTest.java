package com.chatrealtime.integration;

import com.chatrealtime.config.JwtBeansConfig;
import com.chatrealtime.security.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.NestedExceptionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures {@link JwtBeansConfig} fails the context before a {@code JwtTokenService} is usable in non-relaxed profiles.
 */
class JwtBeansStartupFailureIntegrationTest {

    @Configuration
    @Import(JwtBeansConfig.class)
    @EnableConfigurationProperties(JwtProperties.class)
    static class JwtBeansWithProps {
    }

    @Test
    void prodProfile_shortSecret_contextFails() {
        new ApplicationContextRunner()
                .withUserConfiguration(JwtBeansWithProps.class)
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "app.jwt.secret=short",
                        "app.jwt.access-expiration-ms=900000",
                        "app.jwt.refresh-expiration-ms=604800000"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable root = NestedExceptionUtils.getMostSpecificCause(context.getStartupFailure());
                    assertThat(root.getMessage()).contains("32");
                });
    }
}
