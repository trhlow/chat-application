package com.chatrealtime.service;

import com.chatrealtime.domain.RefreshToken;

public interface RefreshTokenService {
    String createToken(String userId);

    RefreshToken requireActiveToken(String rawToken);

    String rotateToken(RefreshToken refreshToken);

    void revokeUserToken(String userId, String rawToken);

    void revokeAllUserTokens(String userId);
}
