package com.chatrealtime.unit;

import com.chatrealtime.dto.request.LoginRequest;
import com.chatrealtime.dto.request.LogoutRequest;
import com.chatrealtime.dto.request.RefreshTokenRequest;
import com.chatrealtime.dto.request.RegisterRequest;
import com.chatrealtime.dto.response.UserProfileResponse;
import com.chatrealtime.exception.ExistsEmailException;
import com.chatrealtime.exception.ExistsUsernameException;
import com.chatrealtime.exception.InvalidCredentialsException;
import com.chatrealtime.mapper.UserMapper;
import com.chatrealtime.domain.User;
import com.chatrealtime.repository.UserRepository;
import com.chatrealtime.service.PresenceService;
import com.chatrealtime.service.RefreshRotationResult;
import com.chatrealtime.service.RefreshTokenService;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.security.JwtProperties;
import com.chatrealtime.security.JwtTokenService;
import com.chatrealtime.security.UserPrincipalService;
import com.chatrealtime.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
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
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private UserPrincipalService userPrincipalService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_ShouldUseGenericConflictMessageWhenUsernameTaken() {
        RegisterRequest request = new RegisterRequest(
                "Alice",
                "secret123",
                "new@example.com",
                null,
                null
        );
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ExistsUsernameException.class)
                .hasMessage("Tài khoản với thông tin này đã tồn tại");
    }

    @Test
    void register_ShouldUseGenericConflictMessageWhenEmailTaken() {
        RegisterRequest request = new RegisterRequest(
                "Alice",
                "secret123",
                "taken@example.com",
                null,
                null
        );
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ExistsEmailException.class)
                .hasMessage("Tài khoản với thông tin này đã tồn tại");
    }

    @Test
    void register_ShouldHashPasswordAndReturnToken() {
        RegisterRequest request = new RegisterRequest(
                "Alice",
                "secret123",
                "Alice@example.com",
                "Alice Nguyen",
                "https://avatar"
        );

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
        assertThat(captor.getValue().getDisplayName()).isEqualTo("Alice Nguyen");
        assertThat(captor.getValue().getAvatar()).isNull();
        assertThat(response.accessToken()).isEqualTo("token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_ShouldThrowOnInvalidPassword() {
        LoginRequest request = new LoginRequest(null, "alice@example.com", "wrong");

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
    void login_ShouldThrowOnEmailNotFound() {
        LoginRequest request = new LoginRequest(null, "notfound@example.com", "password");
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refresh_ShouldRotateRefreshTokenAndReturnNewTokens() {
        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh");

        User user = User.builder()
                .id("u1")
                .username("alice")
                .email("alice@example.com")
                .password("hashed")
                .build();

        when(refreshTokenService.rotateRefreshToken("old-refresh"))
                .thenReturn(new RefreshRotationResult("new-refresh", "u1"));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
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
    void refresh_ShouldThrowOnRevokedToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("revoked-token");
        when(refreshTokenService.rotateRefreshToken("revoked-token"))
                .thenThrow(new InvalidCredentialsException("Invalid refresh token"));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void logout_ShouldRevokeRefreshTokenAndMarkUserOffline() {
        LogoutRequest request = new LogoutRequest("refresh-token");

        User user = User.builder()
                .id("u1")
                .username("alice")
                .password("hashed")
                .isOnline(true)
                .build();
        when(authContextService.requireCurrentUser())
                .thenReturn(new AuthUserPrincipal("u1", "alice", "hashed", 0));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        authService.logout(request);

        assertThat(user.isOnline()).isFalse();
        verify(refreshTokenService).revokeUserToken("u1", "refresh-token");
        verify(userPrincipalService).evictUserCaches("u1", "alice");
        verify(presenceService).markOffline("u1");
        verify(userRepository).save(user);
    }

    @Test
    void logoutAll_ShouldRevokeAllUserTokens() {
        User user = User.builder()
                .id("u1")
                .username("alice")
                .isOnline(true)
                .build();
        when(authContextService.requireCurrentUser())
                .thenReturn(new AuthUserPrincipal("u1", "alice", "hashed", 0));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        authService.logoutAll();

        assertThat(user.isOnline()).isFalse();
        verify(refreshTokenService).revokeAllUserTokens("u1");
        verify(userPrincipalService).evictUserCaches("u1", "alice");
        verify(presenceService).markOffline("u1");
        verify(userRepository).save(user);
    }
}

