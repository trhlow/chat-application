package com.chatrealtime.dto.response;

import java.time.Instant;

/**
 * Fields safe to expose for any authenticated client viewing another user.
 */
public record PublicUserProfileResponse(
        String id,
        String username,
        String displayName,
        String avatar,
        String avatarProvider,
        boolean online,
        Instant lastSeenAt
) {
}
