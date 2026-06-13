package com.chatrealtime.unit;

import com.chatrealtime.dto.response.AuthResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthResponseSerializationTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void authResponseJson_ShouldNotExposeRefreshToken() throws Exception {
        AuthResponse response = new AuthResponse(
                "access-token",
                "refresh-token",
                "Bearer",
                1000L,
                2000L,
                null
        );

        String json = objectMapper.writeValueAsString(response);

        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(json).contains("access-token");
        assertThat(json).doesNotContain("refresh-token");
        assertThat(json).doesNotContain("refreshToken");
    }
}
