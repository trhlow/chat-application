package com.chatrealtime.dto.response;

import java.time.Instant;

public record UserProfileResponse(
        String id,
        String username,
        String email,
        String displayName,
        String bio,
        String phone,
        String themePreference,
        String avatarEndpoint,
        @Deprecated(forRemoval = true)
        String avatar,
        boolean online,
        Instant lastSeenAt
) {
}


