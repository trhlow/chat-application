package com.chatrealtime.dto.response;

import java.time.Instant;

public record PresenceResponse(
        String userId,
        boolean online,
        Instant lastSeenAt
) {
}


