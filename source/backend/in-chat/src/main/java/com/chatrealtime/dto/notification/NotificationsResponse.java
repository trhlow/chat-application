package com.chatrealtime.dto.notification;

import java.time.Instant;

public record NotificationsResponse(
        String id,
        String userId,
        String type,
        String title,
        String message,
        String relatedId,
        boolean read,
        Instant createdAt
) {}

