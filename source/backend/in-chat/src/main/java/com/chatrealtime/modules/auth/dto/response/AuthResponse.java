package com.chatrealtime.modules.auth.dto.response;

import com.chatrealtime.modules.user.dto.response.UserProfileResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        UserProfileResponse user
) {
}


