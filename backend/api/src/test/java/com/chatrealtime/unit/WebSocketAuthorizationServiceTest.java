package com.chatrealtime.unit;

import com.chatrealtime.domain.Message;
import com.chatrealtime.domain.Room;
import com.chatrealtime.repository.MessageRepository;
import com.chatrealtime.repository.RoomRepository;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.security.WebSocketAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthorizationServiceTest {
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private MessageRepository messageRepository;

    @Test
    void authorize_ShouldAllowRoomTopicSubscriptionForMember() {
        WebSocketAuthorizationService service = new WebSocketAuthorizationService(roomRepository, messageRepository);
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "alice", "pw", 1);
        when(roomRepository.findById("room-1"))
                .thenReturn(Optional.of(Room.builder().id("room-1").memberIds(List.of("u1", "u2")).build()));

        service.authorize(principal, StompCommand.SUBSCRIBE, "/topic/rooms/room-1/messages");
    }

    @Test
    void authorize_ShouldRejectRoomTopicSubscriptionForNonMember() {
        WebSocketAuthorizationService service = new WebSocketAuthorizationService(roomRepository, messageRepository);
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "alice", "pw", 1);
        when(roomRepository.findById("room-1"))
                .thenReturn(Optional.of(Room.builder().id("room-1").memberIds(List.of("u2")).build()));

        assertThatThrownBy(() -> service.authorize(
                principal,
                StompCommand.SUBSCRIBE,
                "/topic/rooms/room-1/messages"
        )).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void authorize_ShouldAllowMessageStatusSendForRoomMember() {
        WebSocketAuthorizationService service = new WebSocketAuthorizationService(roomRepository, messageRepository);
        AuthUserPrincipal principal = new AuthUserPrincipal("u2", "bob", "pw", 1);
        when(messageRepository.findById("message-1"))
                .thenReturn(Optional.of(Message.builder().id("message-1").roomId("room-1").build()));
        when(roomRepository.findById("room-1"))
                .thenReturn(Optional.of(Room.builder().id("room-1").memberIds(List.of("u1", "u2")).build()));

        service.authorize(principal, StompCommand.SEND, "/app/messages/message-1/status");
    }

    @Test
    void authorize_ShouldRejectUnknownDestination() {
        WebSocketAuthorizationService service = new WebSocketAuthorizationService(roomRepository, messageRepository);
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "alice", "pw", 1);

        assertThatThrownBy(() -> service.authorize(
                principal,
                StompCommand.SEND,
                "/app/admin/broadcast"
        )).isInstanceOf(AccessDeniedException.class);
    }
}
