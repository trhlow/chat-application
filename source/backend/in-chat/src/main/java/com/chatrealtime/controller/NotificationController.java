package com.chatrealtime.controller;

import com.chatrealtime.dto.request.CreateNotificationRequest;
import com.chatrealtime.dto.response.NotificationsResponse;
import com.chatrealtime.service.NotificationService;
import jakarta.validation.Valid;
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
    public NotificationsResponse createNotification(@Valid @RequestBody CreateNotificationRequest request) {
        return notificationService.createNotification(request);
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationsResponse markAsRead(@PathVariable String notificationId) {
        return notificationService.markAsRead(notificationId);
    }
}



