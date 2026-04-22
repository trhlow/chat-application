package com.chatrealtime.service;

import com.chatrealtime.dto.response.NotificationPageResponse;
import com.chatrealtime.dto.response.NotificationUnreadCountResponse;
import com.chatrealtime.dto.response.NotificationsResponse;

public interface NotificationService {
    NotificationPageResponse getNotificationsByCurrentUser(int page, int size);

    NotificationUnreadCountResponse getUnreadCount();

    NotificationsResponse createSystemNotification(
            String userId,
            String type,
            String title,
            String message,
            String relatedId
    );

    NotificationsResponse markAsRead(String notificationId);

    void markAllAsRead();
}
