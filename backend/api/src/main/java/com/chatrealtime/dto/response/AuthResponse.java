package com.chatrealtime.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record AuthResponse(
        String accessToken,
        @JsonIgnore
        String refreshToken,
        String tokenType,
        long accessExpiresInMs,
        long refreshExpiresInMs,
        UserProfileResponse user
) {
}


