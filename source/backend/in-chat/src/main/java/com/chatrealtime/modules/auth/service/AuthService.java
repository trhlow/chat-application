package com.chatrealtime.modules.auth.service;

import com.chatrealtime.modules.auth.dto.LoginRequest;
import com.chatrealtime.modules.auth.dto.LogoutRequest;
import com.chatrealtime.modules.auth.dto.RefreshTokenRequest;
import com.chatrealtime.modules.auth.dto.RegisterRequest;
import com.chatrealtime.modules.auth.dto.response.AuthResponse;
import com.chatrealtime.exception.ExistsEmailException;
import com.chatrealtime.exception.ExistsUsernameException;
import com.chatrealtime.exception.InvalidCredentialsException;
import com.chatrealtime.exception.UserNotFoundException;
import com.chatrealtime.modules.auth.model.RefreshToken;
import com.chatrealtime.modules.presence.service.PresenceService;
import com.chatrealtime.modules.user.mapper.UserMapper;
import com.chatrealtime.modules.user.model.User;
import com.chatrealtime.modules.user.repository.UserRepository;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.security.JwtProperties;
import com.chatrealtime.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;
    private final AuthContextService authContextService;
    private final PresenceService presenceService;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse register(RegisterRequest request) {
        String normalizedUsername = request.getUsername().trim().toLowerCase(Locale.ROOT);
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new ExistsUsernameException("Username already exists");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ExistsEmailException("Email already exists");
        }

        Instant now = Instant.now();
        User newUser = User.builder()
                .username(normalizedUsername)
                .password(passwordEncoder.encode(request.getPassword()))
                .email(normalizedEmail)
                .displayName(normalizedUsername)
                .themePreference("system")
                .avatar(request.getAvatar())
                .isOnline(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        User savedUser = userRepository.save(newUser);
        return buildAuthResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        User user = findByEmailOrUsername(request.getEmail(), request.getUsername());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        user.setUpdatedAt(Instant.now());
        user.setOnline(true);
        User savedUser = userRepository.save(user);
        presenceService.markOnline(savedUser.getId());
        return buildAuthResponse(savedUser);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.requireActiveToken(request.getRefreshToken());
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

    public void logout(LogoutRequest request) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (request != null) {
            refreshTokenService.revokeUserToken(user.getId(), request.getRefreshToken());
        }

        user.setOnline(false);
        userRepository.save(user);
        presenceService.markOffline(user.getId());
    }

    public void logoutAll() {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        refreshTokenService.revokeAllUserTokens(user.getId());
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setOnline(false);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
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


