package com.chatrealtime.dto.response;

public record NotificationRealtimeEventResponse(
        String eventType,
        NotificationsResponse notification,
        long unreadCount
) {
}
