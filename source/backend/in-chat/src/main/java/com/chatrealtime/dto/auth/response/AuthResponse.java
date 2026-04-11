package com.chatrealtime.dto.auth.response;

import com.chatrealtime.dto.user.response.UserProfileResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        UserProfileResponse user
) {
}
