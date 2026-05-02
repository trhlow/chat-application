package com.chatrealtime.service.impl;

import com.chatrealtime.service.NotificationService;

import com.chatrealtime.dto.response.NotificationPageResponse;
import com.chatrealtime.dto.response.NotificationRealtimeEventResponse;
import com.chatrealtime.dto.response.NotificationUnreadCountResponse;
import com.chatrealtime.dto.response.NotificationsResponse;
import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.exception.ResourceNotFoundException;
import com.chatrealtime.domain.Notification;
import com.chatrealtime.repository.NotificationRepository;
import com.chatrealtime.repository.UserRepository;
import com.chatrealtime.realtime.NotificationRealtimeEventBus;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final AuthContextService authContextService;
    private final UserRepository userRepository;
    private final NotificationRealtimeEventBus notificationRealtimeEventBus;
    private final MongoTemplate mongoTemplate;

    @Override
    public NotificationPageResponse getNotificationsByCurrentUser(int page, int size) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Slice<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                principal.getId(),
                PageRequest.of(safePage, safeSize)
        );
        return new NotificationPageResponse(
                notifications.getContent().stream().map(this::toResponse).toList(),
                safePage,
                safeSize,
                notifications.hasNext()
        );
    }

    @Override
    public NotificationUnreadCountResponse getUnreadCount() {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        return new NotificationUnreadCountResponse(notificationRepository.countByUserIdAndReadFalse(principal.getId()));
    }

    @Override
    public NotificationsResponse createSystemNotification(
            String userId,
            String type,
            String title,
            String message,
            String relatedId
    ) {
        NotificationsResponse response = toResponse(notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .relatedId(relatedId)
                .read(false)
                .createdAt(Instant.now())
                .build()));
        publishUserNotificationEvent(userId, "created", response);
        return response;
    }

    @Override
    public NotificationsResponse markAsRead(String notificationId) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!principal.getId().equals(notification.getUserId())) {
            throw new BadRequestException("Current user cannot update this notification");
        }
        if (notification.isRead()) {
            return toResponse(notification);
        }
        notification.setRead(true);
        NotificationsResponse response = toResponse(notificationRepository.save(notification));
        publishUserNotificationEvent(principal.getId(), "updated", response);
        return response;
    }

    @Override
    public void markAllAsRead() {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Query query = Query.query(Criteria.where("userId").is(principal.getId()).and("read").is(false));
        Update update = new Update().set("read", true);
        if (mongoTemplate.updateMulti(query, update, Notification.class).getModifiedCount() == 0) {
            return;
        }
        notificationRealtimeEventBus.publish(
                principal.getUsername(),
                new NotificationRealtimeEventResponse("all_read", null, 0L)
        );
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

    private void publishUserNotificationEvent(String userId, String eventType, NotificationsResponse notification) {
        notificationRealtimeEventBus.publish(
                resolveDestinationUsername(userId),
                new NotificationRealtimeEventResponse(
                        eventType,
                        notification,
                        notificationRepository.countByUserIdAndReadFalse(userId)
                )
        );
    }

    private String resolveDestinationUsername(String userId) {
        return userRepository.findById(userId)
                .map(user -> user.getUsername())
                .orElse(userId);
    }
}



