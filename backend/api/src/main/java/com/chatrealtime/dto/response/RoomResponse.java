package com.chatrealtime.dto.response;

import java.time.Instant;
import java.util.List;

public record RoomResponse(
        String id,
        String name,
        String type,
        String avatarEndpoint,
        @Deprecated(forRemoval = true)
        String avatar,
        List<String> memberIds,
        List<String> admins,
        String createdBy,
        String ownerId,
        long unreadCount,
        Instant lastMessageAt,
        String lastMessagePreview,
        Instant createdAt,
        Instant updatedAt,
        GroupSettingsResponse settings
) {
    public RoomResponse(
            String id,
            String name,
            String type,
            String avatarEndpoint,
            String avatar,
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
        this(
                id,
                name,
                type,
                avatarEndpoint,
                avatar,
                memberIds,
                admins,
                createdBy,
                ownerId,
                unreadCount,
                lastMessageAt,
                lastMessagePreview,
                createdAt,
                updatedAt,
                null
        );
    }
}


