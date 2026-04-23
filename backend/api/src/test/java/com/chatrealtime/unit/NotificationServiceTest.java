package com.chatrealtime.unit;

import com.chatrealtime.domain.Notification;
import com.chatrealtime.dto.response.NotificationPageResponse;
import com.chatrealtime.dto.response.NotificationUnreadCountResponse;
import com.chatrealtime.dto.response.NotificationsResponse;
import com.chatrealtime.repository.NotificationRepository;
import com.chatrealtime.repository.UserRepository;
import com.chatrealtime.realtime.NotificationRealtimeEventBus;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private AuthContextService authContextService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationRealtimeEventBus notificationRealtimeEventBus;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void getNotificationsByCurrentUser_ShouldReturnPagedNotifications() {
        Notification notification = notification("n1", "u1", false);
        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("u1", PageRequest.of(0, 20)))
                .thenReturn(new SliceImpl<>(List.of(notification), PageRequest.of(0, 20), true));

        NotificationPageResponse response = notificationService.getNotificationsByCurrentUser(0, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).id()).isEqualTo("n1");
        assertThat(response.hasMore()).isTrue();
    }

    @Test
    void createSystemNotification_ShouldPersistAndPushRealtimeEvent() {
        Notification saved = notification("n1", "u2", false);
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);
        when(notificationRepository.countByUserIdAndReadFalse("u2")).thenReturn(3L);
        when(userRepository.findById("u2")).thenReturn(Optional.of(
                com.chatrealtime.domain.User.builder().id("u2").username("bob").build()
        ));

        NotificationsResponse response = notificationService.createSystemNotification(
                "u2",
                "friend_request",
                "New friend request",
                "Alice sent you a request",
                "fr1"
        );

        verify(notificationRealtimeEventBus).publish(eq("bob"), any());
        assertThat(response.id()).isEqualTo("n1");
        assertThat(response.read()).isFalse();
    }

    @Test
    void markAllAsRead_ShouldUpdateUnreadNotificationsAndPushRealtimeEvent() {
        Notification first = notification("n1", "u1", false);
        Notification second = notification("n2", "u1", false);
        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc("u1")).thenReturn(List.of(first, second));

        notificationService.markAllAsRead();

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).allMatch(Notification::isRead);
        verify(notificationRealtimeEventBus).publish(eq("alice"), any());
    }

    @Test
    void getUnreadCount_ShouldReturnCurrentUnreadCount() {
        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(notificationRepository.countByUserIdAndReadFalse("u1")).thenReturn(5L);

        NotificationUnreadCountResponse response = notificationService.getUnreadCount();

        assertThat(response.unreadCount()).isEqualTo(5L);
    }

    @Test
    void markAsRead_ShouldPersistAndPushUpdatedUnreadCount() {
        Notification notification = notification("n1", "u1", false);
        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(notificationRepository.findById("n1")).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.countByUserIdAndReadFalse("u1")).thenReturn(0L);
        when(userRepository.findById("u1")).thenReturn(Optional.of(
                com.chatrealtime.domain.User.builder().id("u1").username("alice").build()
        ));

        NotificationsResponse response = notificationService.markAsRead("n1");

        assertThat(response.read()).isTrue();
        verify(notificationRealtimeEventBus).publish(eq("alice"), any());
    }

    private Notification notification(String id, String userId, boolean read) {
        return Notification.builder()
                .id(id)
                .userId(userId)
                .type("friend_request")
                .title("Title")
                .message("Message")
                .relatedId("r1")
                .read(read)
                .createdAt(Instant.now())
                .build();
    }
}
