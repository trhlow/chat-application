package com.chatrealtime.modules.notification.controller;

import com.chatrealtime.modules.notification.dto.NotificationsResponse;
import com.chatrealtime.modules.notification.model.Notification;
import com.chatrealtime.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationsResponse> getNotifications() {
        return notificationService.getNotificationsByCurrentUser();
    }

    @PostMapping
    public Notification createNotification(@RequestBody Notification notification) {
        return notificationService.createNotification(notification);
    }

    @PatchMapping("/{notificationId}/read")
    public Notification markAsRead(@PathVariable String notificationId) {
        return notificationService.markAsRead(notificationId);
    }
}



