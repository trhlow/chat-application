package com.chatrealtime.modules.auth.service;

import com.chatrealtime.modules.auth.dto.LoginRequest;
import com.chatrealtime.modules.auth.dto.LogoutRequest;
import com.chatrealtime.modules.auth.dto.RefreshTokenRequest;
import com.chatrealtime.modules.auth.dto.RegisterRequest;
import com.chatrealtime.modules.auth.model.RefreshToken;
import com.chatrealtime.modules.user.dto.response.UserProfileResponse;
import com.chatrealtime.exception.InvalidCredentialsException;
import com.chatrealtime.modules.user.mapper.UserMapper;
import com.chatrealtime.modules.user.model.User;
import com.chatrealtime.modules.user.repository.UserRepository;
import com.chatrealtime.modules.presence.service.PresenceService;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.security.JwtProperties;
import com.chatrealtime.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AuthContextService authContextService;
    @Mock
    private PresenceService presenceService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_ShouldHashPasswordAndReturnToken() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("Alice");
        request.setEmail("Alice@example.com");
        request.setPassword("secret123");
        request.setAvatar("https://avatar");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");

        User savedUser = User.builder()
                .id("u1")
                .username("alice")
                .email("alice@example.com")
                .password("hashed")
                .createdAt(Instant.now())
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenService.generateToken(any(AuthUserPrincipal.class))).thenReturn("token");
        when(refreshTokenService.createToken("u1")).thenReturn("refresh-token");
        when(jwtProperties.accessExpirationMs()).thenReturn(1000L);
        when(jwtProperties.refreshExpirationMs()).thenReturn(2000L);
        when(userMapper.toUserProfileResponse(savedUser))
                .thenReturn(new UserProfileResponse("u1", "alice", "alice@example.com", "Alice", null, null, "system", null, null, false, null));

        var response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
        assertThat(captor.getValue().getUsername()).isEqualTo("alice");
        assertThat(response.accessToken()).isEqualTo("token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_ShouldThrowOnInvalidPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("wrong");

        User user = User.builder()
                .id("u1")
                .username("alice")
                .email("alice@example.com")
                .password("hashed")
                .build();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void refresh_ShouldRotateRefreshTokenAndReturnNewTokens() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("old-refresh");

        RefreshToken refreshToken = RefreshToken.builder()
                .userId("u1")
                .tokenHash("old-hash")
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        User user = User.builder()
                .id("u1")
                .username("alice")
                .email("alice@example.com")
                .password("hashed")
                .build();

        when(refreshTokenService.requireActiveToken("old-refresh")).thenReturn(refreshToken);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(refreshTokenService.rotateToken(refreshToken)).thenReturn("new-refresh");
        when(jwtTokenService.generateToken(any(AuthUserPrincipal.class))).thenReturn("new-access");
        when(jwtProperties.accessExpirationMs()).thenReturn(1000L);
        when(jwtProperties.refreshExpirationMs()).thenReturn(2000L);
        when(userMapper.toUserProfileResponse(user))
                .thenReturn(new UserProfileResponse("u1", "alice", "alice@example.com", "Alice", null, null, "system", null, null, false, null));

        var response = authService.refresh(request);

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void logout_ShouldRevokeRefreshTokenAndMarkUserOffline() {
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("refresh-token");

        User user = User.builder()
                .id("u1")
                .username("alice")
                .password("hashed")
                .isOnline(true)
                .build();
        when(authContextService.requireCurrentUser())
                .thenReturn(new AuthUserPrincipal("u1", "alice", "hashed", 0));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        authService.logout(request);

        assertThat(user.isOnline()).isFalse();
        verify(refreshTokenService).revokeUserToken("u1", "refresh-token");
        verify(presenceService).markOffline("u1");
        verify(userRepository).save(user);
    }
}

