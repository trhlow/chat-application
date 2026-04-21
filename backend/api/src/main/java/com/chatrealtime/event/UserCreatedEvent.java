package com.chatrealtime.event;

import java.time.Instant;

public record UserCreatedEvent(
        String userId,
        Instant createdAt
) {
}
