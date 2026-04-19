package com.chatrealtime.service;

import com.chatrealtime.dto.request.CreateNotificationRequest;
import com.chatrealtime.dto.response.NotificationsResponse;

import java.util.List;

public interface NotificationService {
    List<NotificationsResponse> getNotificationsByCurrentUser();

    NotificationsResponse createNotification(CreateNotificationRequest request);

    NotificationsResponse createSystemNotification(
            String userId,
            String type,
            String title,
            String message,
            String relatedId
    );

    NotificationsResponse markAsRead(String notificationId);
}
