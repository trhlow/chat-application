package com.chatrealtime.dto.notification;

import java.time.Instant;
import java.util.UUID;

public class NotificationResponse(
        UUID id,
        String type,
        String title,
        String message,
        UUID relateId,
        boolean read,
        Instant createdAt
){}