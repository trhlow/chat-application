package com.chatrealtime.modules.notification.service;

import com.chatrealtime.modules.notification.dto.NotificationsResponse;
import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.modules.notification.model.Notification;
import com.chatrealtime.modules.notification.repository.NotificationRepository;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final AuthContextService authContextService;

    public List<NotificationsResponse> getNotificationsByCurrentUser() {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(principal.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Notification createNotification(Notification notification) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        notification.setUserId(principal.getId());
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(Instant.now());
        }
        return notificationRepository.save(notification);
    }

    public Notification createSystemNotification(
            String userId,
            String type,
            String title,
            String message,
            String relatedId
    ) {
        return notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .relatedId(relatedId)
                .read(false)
                .createdAt(Instant.now())
                .build());
    }

    public Notification markAsRead(String notificationId) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!principal.getId().equals(notification.getUserId())) {
            throw new BadRequestException("Current user cannot update this notification");
        }
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    private NotificationsResponse toResponse(Notification notification) {
        return new NotificationsResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getRelatedId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}



