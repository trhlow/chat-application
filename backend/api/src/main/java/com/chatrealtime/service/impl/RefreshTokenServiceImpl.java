package com.chatrealtime.service.impl;

import com.chatrealtime.service.RefreshTokenService;

import com.chatrealtime.exception.InvalidCredentialsException;
import com.chatrealtime.domain.RefreshToken;
import com.chatrealtime.repository.RefreshTokenRepository;
import com.chatrealtime.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@Transactional
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private static final int TOKEN_BYTE_LENGTH = 64;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String createToken(String userId) {
        String rawToken = generateSecureToken();
        String tokenHash = hash(rawToken);
        Instant now = Instant.now();

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .createdAt(now)
                .expiresAt(now.plusMillis(jwtProperties.refreshExpirationMs()))
                .build();
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Override
    public RefreshToken requireActiveToken(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        if (refreshToken.getRevokedAt() != null || refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }

        return refreshToken;
    }

    @Override
    public String rotateToken(RefreshToken refreshToken) {
        String newRawToken = generateSecureToken();
        String newTokenHash = hash(newRawToken);
        Instant now = Instant.now();

        refreshToken.setRevokedAt(now);
        refreshToken.setReplacedByTokenHash(newTokenHash);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .userId(refreshToken.getUserId())
                .tokenHash(newTokenHash)
                .createdAt(now)
                .expiresAt(now.plusMillis(jwtProperties.refreshExpirationMs()))
                .build();

        refreshTokenRepository.save(refreshToken);
        refreshTokenRepository.save(newRefreshToken);
        return newRawToken;
    }

    @Override
    public void revokeUserToken(String userId, String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .filter(refreshToken -> refreshToken.getUserId().equals(userId))
                .filter(refreshToken -> refreshToken.getRevokedAt() == null)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(refreshToken);
                });
    }

    @Override
    public void revokeAllUserTokens(String userId) {
        Instant now = Instant.now();
        refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId).forEach(refreshToken -> {
            refreshToken.setRevokedAt(now);
            refreshTokenRepository.save(refreshToken);
        });
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
