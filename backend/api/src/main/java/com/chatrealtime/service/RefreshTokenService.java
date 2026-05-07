package com.chatrealtime.service;

import com.chatrealtime.domain.RefreshToken;

public interface RefreshTokenService {
    String createToken(String userId);

    RefreshToken requireActiveToken(String rawToken);

    /**
     * Revokes the presented token and persists a new refresh token in one logical flow.
     * Uses an atomic update so concurrent refresh requests cannot both rotate the same token.
     */
    RefreshRotationResult rotateRefreshToken(String rawRefreshToken);

    void revokeUserToken(String userId, String rawToken);

    void revokeAllUserTokens(String userId);
}
