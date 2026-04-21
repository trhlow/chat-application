package com.chatrealtime.dto.response;

import java.time.Instant;
import java.util.List;

public record RoomResponse(
        String id,
        String name,
        String type,
        String avatar,
        String avatarProvider,
        List<String> memberIds,
        List<String> admins,
        String createdBy,
        String ownerId,
        long unreadCount,
        Instant lastMessageAt,
        String lastMessagePreview,
        Instant createdAt,
        Instant updatedAt
) {
}


