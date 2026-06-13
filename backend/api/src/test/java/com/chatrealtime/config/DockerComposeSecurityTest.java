package com.chatrealtime.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DockerComposeSecurityTest {

    @Test
    void localCompose_shouldNotHardcodeJwtSecret() throws IOException {
        String compose = Files.readString(Path.of("docker", "docker-compose.yml"));

        assertThat(compose).doesNotContain("APP_JWT_SECRET: change-this");
        assertThat(compose).contains("APP_JWT_SECRET: ${APP_JWT_SECRET:?");
    }

    @Test
    void productionCompose_shouldRequireProdProfileAndExternalSecrets() throws IOException {
        String compose = Files.readString(Path.of("docker", "docker-compose.prod.yml"));

        assertThat(compose)
                .contains("SPRING_PROFILES_ACTIVE: prod")
                .contains("APP_JWT_SECRET: ${APP_JWT_SECRET:?")
                .contains("CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS:?")
                .contains("SPRING_DATA_MONGODB_URI: ${SPRING_DATA_MONGODB_URI:?");
        assertThat(compose).doesNotContain("change-this");
    }
}
