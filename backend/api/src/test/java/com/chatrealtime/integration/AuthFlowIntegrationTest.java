package com.chatrealtime.integration;

import com.chatrealtime.domain.User;
import com.chatrealtime.dto.request.LoginRequest;
import com.chatrealtime.dto.request.LogoutRequest;
import com.chatrealtime.dto.request.RefreshTokenRequest;
import com.chatrealtime.repository.UserRepository;
import com.chatrealtime.service.RefreshTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenService refreshTokenService;

    private String testUserEmail;
    private String testPassword;

    @BeforeEach
    void setUp() {
        testUserEmail = "test_" + UUID.randomUUID() + "@example.com";
        testPassword = "Password123!";

        userRepository.save(User.builder()
                .id(UUID.randomUUID().toString())
                .username("testuser_" + UUID.randomUUID().toString().substring(0, 8))
                .email(testUserEmail)
                .password(passwordEncoder.encode(testPassword))
                .displayName("Test User")
                .tokenVersion(0)
                .isOnline(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
    }

    @Test
    void authFlow_login_refresh_logout() throws Exception {
        // 1. Login
        LoginRequest loginRequest = new LoginRequest(null, testUserEmail, testPassword);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponseStr = loginResult.getResponse().getContentAsString();
        JsonNode loginResponse = objectMapper.readTree(loginResponseStr);
        String accessToken1 = loginResponse.get("accessToken").asText();
        String cookieHeader1 = loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String refreshToken1 = extractCookieValue(cookieHeader1, "refreshToken");

        assertThat(accessToken1).isNotBlank();
        assertThat(refreshToken1).isNotBlank();

        // 2. Refresh Token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken1);
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String refreshResponseStr = refreshResult.getResponse().getContentAsString();
        JsonNode refreshResponse = objectMapper.readTree(refreshResponseStr);
        String accessToken2 = refreshResponse.get("accessToken").asText();
        String cookieHeader2 = refreshResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String refreshToken2 = extractCookieValue(cookieHeader2, "refreshToken");

        assertThat(accessToken2).isNotBlank();
        assertThat(refreshToken2).isNotBlank();
        // Note: accessToken2 might be equal to accessToken1 if generated in the same second.
        // We only assert that refreshToken has been rotated.
        assertThat(refreshToken2).isNotEqualTo(refreshToken1);

        // Verify old refresh token is revoked
        RefreshTokenRequest oldRefreshRequest = new RefreshTokenRequest(refreshToken1);
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldRefreshRequest)))
                .andExpect(status().isUnauthorized()); // Or 400 depending on your exact error handling, but should fail

        // 3. Logout
        LogoutRequest logoutRequest = new LogoutRequest(refreshToken2);
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken2)
                        .header(HttpHeaders.COOKIE, "refreshToken=" + refreshToken2)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isNoContent());

        // Verify the second refresh token is now revoked
        RefreshTokenRequest newRefreshRequest = new RefreshTokenRequest(refreshToken2);
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRefreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    private String extractCookieValue(String cookieHeader, String cookieName) {
        if (cookieHeader == null) return null;
        for (String part : cookieHeader.split(";")) {
            part = part.trim();
            if (part.startsWith(cookieName + "=")) {
                return part.substring(cookieName.length() + 1);
            }
        }
        return null;
    }
}
