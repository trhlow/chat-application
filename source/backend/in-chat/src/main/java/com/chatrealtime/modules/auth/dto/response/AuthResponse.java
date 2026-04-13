package com.chatrealtime.modules.auth.dto.response;

import com.chatrealtime.modules.user.dto.response.UserProfileResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessExpiresInMs,
        long refreshExpiresInMs,
        UserProfileResponse user
) {
}


