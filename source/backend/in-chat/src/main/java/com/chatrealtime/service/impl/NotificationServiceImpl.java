package com.chatrealtime.service.impl;

import com.chatrealtime.service.NotificationService;

import com.chatrealtime.dto.request.CreateNotificationRequest;
import com.chatrealtime.dto.response.NotificationsResponse;
import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.domain.Notification;
import com.chatrealtime.repository.NotificationRepository;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final AuthContextService authContextService;

    @Override
    public List<NotificationsResponse> getNotificationsByCurrentUser() {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(principal.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public NotificationsResponse createNotification(CreateNotificationRequest request) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Notification notification = Notification.builder()
                .userId(principal.getId())
                .type(request.type())
                .title(request.title())
                .message(request.message())
                .relatedId(request.relatedId())
                .createdAt(Instant.now())
                .build();
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    public NotificationsResponse markAsRead(String notificationId) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!principal.getId().equals(notification.getUserId())) {
            throw new BadRequestException("Current user cannot update this notification");
        }
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
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



