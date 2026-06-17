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
import com.chatrealtime.service.PresenceService;
import com.chatrealtime.service.RefreshRotationResult;
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

import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final String REGISTER_CONFLICT_MESSAGE = "Tài khoản với thông tin này đã tồn tại";
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
            throw new ExistsUsernameException(REGISTER_CONFLICT_MESSAGE);
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ExistsEmailException(REGISTER_CONFLICT_MESSAGE);
        }

        Instant now = Instant.now();
        User newUser = User.builder()
                .username(normalizedUsername)
                .password(passwordEncoder.encode(request.password()))
                .email(normalizedEmail)
                .displayName(displayName)
                .themePreference("system")
                .avatar(null)
                .isOnline(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            User savedUser = userRepository.save(newUser);
            eventPublisher.publishEvent(new UserCreatedEvent(savedUser.getId(), savedUser.getCreatedAt()));
            return buildAuthResponse(savedUser);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new com.chatrealtime.exception.ConflictException(REGISTER_CONFLICT_MESSAGE);
        }
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = findByEmailOrUsername(request.email(), request.username());

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Thông tin đăng nhập không hợp lệ");
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
        RefreshRotationResult rotation = refreshTokenService.rotateRefreshToken(request.refreshToken());
        User user = userRepository.findById(rotation.userId())
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng"));

        String accessToken = jwtTokenService.generateToken(AuthUserPrincipal.from(user));
        return new AuthResponse(
                accessToken,
                rotation.newRefreshToken(),
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
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng"));

        if (request != null) {
            refreshTokenService.revokeUserToken(user.getId(), request.refreshToken());
        }

        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setOnline(false);
        user.setUpdatedAt(Instant.now());
        User savedUser = userRepository.save(user);
        userPrincipalService.evictUserCaches(savedUser.getId(), savedUser.getUsername());
        presenceService.markOffline(user.getId());
    }

    @Override
    public void logoutAll() {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng"));

        refreshTokenService.revokeAllUserTokens(user.getId());
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setOnline(false);
        user.setUpdatedAt(Instant.now());
        User savedUser = userRepository.save(user);
        userPrincipalService.evictUserCaches(savedUser.getId(), savedUser.getUsername());
        presenceService.markOffline(user.getId());
    }

    @Override
    public long refreshTokenTtlMs() {
        return jwtProperties.refreshExpirationMs();
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
                    .orElseThrow(() -> new InvalidCredentialsException("Thông tin đăng nhập không hợp lệ"));
        }

        if (username != null && !username.isBlank()) {
            return userRepository.findByUsername(username.trim().toLowerCase(Locale.ROOT))
                    .orElseThrow(() -> new InvalidCredentialsException("Thông tin đăng nhập không hợp lệ"));
        }

        throw new InvalidCredentialsException("Yêu cầu cung cấp email hoặc tên đăng nhập");
    }
}


