package com.chatrealtime.modules.user.dto.response;

import java.time.Instant;

public record UserProfileResponse(
        String id,
        String username,
        String email,
        String displayName,
        String bio,
        String phone,
        String themePreference,
        String avatar,
        String avatarProvider,
        boolean online,
        Instant lastSeenAt
) {
}


