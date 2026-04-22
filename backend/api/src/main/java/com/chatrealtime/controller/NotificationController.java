package com.chatrealtime.controller;

import com.chatrealtime.dto.response.NotificationPageResponse;
import com.chatrealtime.dto.response.NotificationUnreadCountResponse;
import com.chatrealtime.dto.response.NotificationsResponse;
import com.chatrealtime.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public NotificationPageResponse getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return notificationService.getNotificationsByCurrentUser(page, size);
    }

    @GetMapping("/unread-count")
    public NotificationUnreadCountResponse getUnreadCount() {
        return notificationService.getUnreadCount();
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationsResponse markAsRead(@PathVariable String notificationId) {
        return notificationService.markAsRead(notificationId);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.noContent().build();
    }
}



