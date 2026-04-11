package com.chatrealtime.service;

import com.chatrealtime.dto.auth.LoginRequest;
import com.chatrealtime.dto.auth.RegisterRequest;
import com.chatrealtime.dto.user.response.UserProfileResponse;
import com.chatrealtime.exception.InvalidCredentialsException;
import com.chatrealtime.mapper.UserMapper;
import com.chatrealtime.model.User;
import com.chatrealtime.repository.UserRepository;
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
        when(jwtProperties.expirationMs()).thenReturn(1000L);
        when(userMapper.toUserProfileResponse(savedUser))
                .thenReturn(new UserProfileResponse("u1", "alice", "alice@example.com", null, false, null));

        var response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
        assertThat(captor.getValue().getUsername()).isEqualTo("alice");
        assertThat(response.accessToken()).isEqualTo("token");
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
}
