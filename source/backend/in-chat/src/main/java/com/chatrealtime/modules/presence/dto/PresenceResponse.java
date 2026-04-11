package com.chatrealtime.modules.presence.dto;

import java.time.Instant;

public record PresenceResponse(
        String userId,
        boolean online,
        Instant lastSeenAt
) {
}


