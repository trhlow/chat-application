package com.chatrealtime.dto.response;

import java.time.Instant;

public record TypingIndicatorResponse(
        String roomId,
        String userId,
        String username,
        boolean typing,
        Instant timestamp
) {
}
