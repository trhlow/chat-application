package com.chatrealtime.modules.user.dto.response;

import java.time.Instant;

public record UserProfileResponse(
        String id,
        String username,
        String email,
        String avatar,
        boolean online,
        Instant lastSeenAt
) {
}


