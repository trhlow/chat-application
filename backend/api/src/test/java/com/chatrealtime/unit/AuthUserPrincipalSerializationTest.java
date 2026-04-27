package com.chatrealtime.unit;

import com.chatrealtime.security.AuthUserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures cached principals do not leak password hashes into JSON (e.g. Redis).
 */
class AuthUserPrincipalSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void jsonSerialize_shouldOmitPassword() throws Exception {
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "alice", "$2a$10$hashhashhashhashhashhashhash", 3);

        String json = objectMapper.writeValueAsString(principal);

        assertThat(json).doesNotContain("hashhash");
        assertThat(json).doesNotContain("$2a$");
        assertThat(json).contains("u1");
        assertThat(json).contains("alice");
        assertThat(json).contains("\"tokenVersion\":3");
    }

    @Test
    void jsonDeserialize_withoutPassword_shouldRestorePrincipal() throws Exception {
        String json = """
                {"id":"u1","username":"bob","tokenVersion":1}
                """;

        AuthUserPrincipal principal = objectMapper.readValue(json, AuthUserPrincipal.class);

        assertThat(principal.getId()).isEqualTo("u1");
        assertThat(principal.getUsername()).isEqualTo("bob");
        assertThat(principal.getTokenVersion()).isEqualTo(1);
        assertThat(principal.getPassword()).isNull();
    }
}
