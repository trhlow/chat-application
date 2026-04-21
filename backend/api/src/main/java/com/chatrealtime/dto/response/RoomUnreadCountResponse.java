package com.chatrealtime.dto.response;

public record RoomUnreadCountResponse(
        String roomId,
        long unreadCount
) {
}
