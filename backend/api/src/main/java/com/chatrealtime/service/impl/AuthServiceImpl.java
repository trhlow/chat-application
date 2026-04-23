package com.chatrealtime.service.impl;

import com.chatrealtime.service.AuthService;

import com.chatrealtime.dto.request.LoginRequest;
import com.chatrealtime.dto.request.LogoutRequest;
import com.chatrealtime.dto.request.RefreshTokenRequest;
import com.chatrealtime.dto.request.RegisterRequest;
import com.chatrealtime.dto.response.AuthResponse;
import com.chatrealtime.event.UserCreatedEvent;
import com.chatrealtime.exception.ExistsEmailException;
import com.chatrealtime.exception.ExistsUsernameException;
import com.chatrealtime.exception.InvalidCredentialsException;
import com.chatrealtime.exception.UserNotFoundException;
import com.chatrealtime.domain.RefreshToken;
import com.chatrealtime.service.PresenceService;
import com.chatrealtime.service.RefreshTokenService;
import com.chatrealtime.mapper.UserMapper;
import com.chatrealtime.domain.User;
import com.chatrealtime.repository.UserRepository;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.security.JwtProperties;
import com.chatrealtime.security.JwtTokenService;
import com.chatrealtime.security.UserPrincipalService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;
    private final AuthContextService authContextService;
    private final PresenceService presenceService;
    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserPrincipalService userPrincipalService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        String normalizedUsername = request.username().trim().toLowerCase(Locale.ROOT);
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        String displayName = request.displayName() != null && !request.displayName().isBlank()
                ? request.displayName().trim()
                : normalizedUsername;
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new ExistsUsernameException("Username already exists");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ExistsEmailException("Email already exists");
        }

        Instant now = Instant.now();
        User newUser = User.builder()
                .username(normalizedUsername)
                .password(passwordEncoder.encode(request.password()))
                .email(normalizedEmail)
                .displayName(displayName)
                .themePreference("system")
                .avatar(request.avatar())
                .isOnline(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        User savedUser = userRepository.save(newUser);
        eventPublisher.publishEvent(new UserCreatedEvent(savedUser.getId(), savedUser.getCreatedAt()));
        return buildAuthResponse(savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = findByEmailOrUsername(request.email(), request.username());

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        user.setUpdatedAt(Instant.now());
        user.setOnline(true);
        User savedUser = userRepository.save(user);
        userPrincipalService.evictUserCaches(savedUser.getId(), savedUser.getUsername());
        presenceService.markOnline(savedUser.getId());
        return buildAuthResponse(savedUser);
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.requireActiveToken(request.refreshToken());
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String rotatedRefreshToken = refreshTokenService.rotateToken(refreshToken);
        String accessToken = jwtTokenService.generateToken(AuthUserPrincipal.from(user));
        return new AuthResponse(
                accessToken,
                rotatedRefreshToken,
                "Bearer",
                jwtProperties.accessExpirationMs(),
                jwtProperties.refreshExpirationMs(),
                userMapper.toUserProfileResponse(user)
        );
    }

    @Override
    public void logout(LogoutRequest request) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (request != null) {
            refreshTokenService.revokeUserToken(user.getId(), request.refreshToken());
        }

        user.setOnline(false);
        User savedUser = userRepository.save(user);
        userPrincipalService.evictUserCaches(savedUser.getId(), savedUser.getUsername());
        presenceService.markOffline(user.getId());
    }

    @Override
    public void logoutAll() {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        refreshTokenService.revokeAllUserTokens(user.getId());
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setOnline(false);
        user.setUpdatedAt(Instant.now());
        User savedUser = userRepository.save(user);
        userPrincipalService.evictUserCaches(savedUser.getId(), savedUser.getUsername());
        presenceService.markOffline(user.getId());
    }

    private AuthResponse buildAuthResponse(User user) {
        AuthUserPrincipal principal = AuthUserPrincipal.from(user);
        String accessToken = jwtTokenService.generateToken(principal);
        String refreshToken = refreshTokenService.createToken(user.getId());
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtProperties.accessExpirationMs(),
                jwtProperties.refreshExpirationMs(),
                userMapper.toUserProfileResponse(user)
        );
    }

    private User findByEmailOrUsername(String email, String username) {
        if (email != null && !email.isBlank()) {
            return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));
        }

        if (username != null && !username.isBlank()) {
            return userRepository.findByUsername(username.trim().toLowerCase(Locale.ROOT))
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));
        }

        throw new InvalidCredentialsException("Email or username is required");
    }
}


