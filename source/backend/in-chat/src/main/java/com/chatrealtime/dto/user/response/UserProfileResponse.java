package com.chatrealtime.dto.user.response;

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
