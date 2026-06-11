package com.chatrealtime.unit;

import com.chatrealtime.domain.Room;
import com.chatrealtime.repository.MessageRepository;
import com.chatrealtime.repository.RoomRepository;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.security.WebSocketAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthorizationServiceTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private WebSocketAuthorizationService webSocketAuthorizationService;

    private final AuthUserPrincipal principal = new AuthUserPrincipal("u1", "alice", "pw", 0);

    @Test
    void subscribe_userQueuePresence_isAllowed() {
        assertThatCode(() -> webSocketAuthorizationService.authorize(
                principal,
                StompCommand.SUBSCRIBE,
                "/user/queue/presence"
        )).doesNotThrowAnyException();
    }

    @Test
    void subscribe_userQueueNotifications_isAllowed() {
        assertThatCode(() -> webSocketAuthorizationService.authorize(
                principal,
                StompCommand.SUBSCRIBE,
                "/user/queue/notifications"
        )).doesNotThrowAnyException();
    }

    @Test
    void subscribe_unknownUserQueue_isDenied() {
        assertThatThrownBy(() -> webSocketAuthorizationService.authorize(
                principal,
                StompCommand.SUBSCRIBE,
                "/user/queue/other"
        )).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void send_roomTyping_whenMember_isAllowed() {
        when(roomRepository.findById("r1"))
                .thenReturn(Optional.of(Room.builder().id("r1").memberIds(List.of("u1", "u2")).build()));

        assertThatCode(() -> webSocketAuthorizationService.authorize(
                principal,
                StompCommand.SEND,
                "/app/rooms/r1/typing"
        )).doesNotThrowAnyException();
    }

    @Test
    void subscribe_roomTyping_whenMember_isAllowed() {
        when(roomRepository.findById("r1"))
                .thenReturn(Optional.of(Room.builder().id("r1").memberIds(List.of("u1", "u2")).build()));

        assertThatCode(() -> webSocketAuthorizationService.authorize(
                principal,
                StompCommand.SUBSCRIBE,
                "/topic/rooms/r1/typing"
        )).doesNotThrowAnyException();
    }
}
