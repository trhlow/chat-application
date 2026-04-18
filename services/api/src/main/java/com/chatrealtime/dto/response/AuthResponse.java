package com.chatrealtime.dto.response;

import com.chatrealtime.dto.response.UserProfileResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessExpiresInMs,
        long refreshExpiresInMs,
        UserProfileResponse user
) {
}


