package com.chatrealtime.service.impl;

import com.chatrealtime.service.RefreshRotationResult;
import com.chatrealtime.service.RefreshTokenService;

import com.chatrealtime.exception.InvalidCredentialsException;
import com.chatrealtime.domain.RefreshToken;
import com.chatrealtime.repository.RefreshTokenRepository;
import com.chatrealtime.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private static final int TOKEN_BYTE_LENGTH = 64;
    private static final int NEW_TOKEN_SAVE_MAX_ATTEMPTS = 3;
    private static final long NEW_TOKEN_SAVE_INITIAL_BACKOFF_MS = 25L;

    private final RefreshTokenRepository refreshTokenRepository;
    private final MongoTemplate mongoTemplate;
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
    public RefreshRotationResult rotateRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }

        String tokenHash = hash(rawRefreshToken);
        Instant now = Instant.now();

        String newRawToken = generateSecureToken();
        String newTokenHash = hash(newRawToken);

        Query query = Query.query(
                Criteria.where("tokenHash").is(tokenHash)
                        .and("revokedAt").is(null)
                        .and("expiresAt").gt(now)
        );
        Update update = new Update()
                .set("revokedAt", now)
                .set("replacedByTokenHash", newTokenHash);

        RefreshToken revoked = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(false),
                RefreshToken.class
        );

        if (revoked == null) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }

        RefreshToken newRefreshToken = RefreshToken.builder()
                .userId(revoked.getUserId())
                .tokenHash(newTokenHash)
                .createdAt(now)
                .expiresAt(now.plusMillis(jwtProperties.refreshExpirationMs()))
                .build();
        persistNewRefreshTokenWithRetries(newRefreshToken);

        return new RefreshRotationResult(newRawToken, revoked.getUserId());
    }

    private void persistNewRefreshTokenWithRetries(RefreshToken newRefreshToken) {
        long backoffMs = NEW_TOKEN_SAVE_INITIAL_BACKOFF_MS;
        TransientDataAccessException lastTransient = null;
        for (int attempt = 1; attempt <= NEW_TOKEN_SAVE_MAX_ATTEMPTS; attempt++) {
            try {
                refreshTokenRepository.save(newRefreshToken);
                return;
            } catch (TransientDataAccessException exception) {
                lastTransient = exception;
                log.warn(
                        "Transient failure persisting rotated refresh token for user {} (attempt {}/{}): {}",
                        newRefreshToken.getUserId(),
                        attempt,
                        NEW_TOKEN_SAVE_MAX_ATTEMPTS,
                        exception.getMessage()
                );
                if (attempt < NEW_TOKEN_SAVE_MAX_ATTEMPTS) {
                    sleepQuietly(backoffMs);
                    backoffMs *= 2;
                }
            }
        }
        if (lastTransient != null) {
            log.error(
                    "Could not persist new refresh token after revoking previous token for user {}; client must sign in again",
                    newRefreshToken.getUserId(),
                    lastTransient
            );
            throw lastTransient;
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while persisting refresh token", exception);
        }
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
