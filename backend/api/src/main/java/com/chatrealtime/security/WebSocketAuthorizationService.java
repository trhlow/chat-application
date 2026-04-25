package com.chatrealtime.security;

import com.chatrealtime.repository.MessageRepository;
import com.chatrealtime.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class WebSocketAuthorizationService {
    private static final Pattern ROOM_MESSAGE_SEND_DESTINATION =
            Pattern.compile("^/app/rooms/(?<roomId>[^/]+)/messages$");
    private static final Pattern MESSAGE_STATUS_SEND_DESTINATION =
            Pattern.compile("^/app/messages/(?<messageId>[^/]+)/status$");
    private static final Pattern ROOM_TOPIC_SUBSCRIPTION =
            Pattern.compile("^/topic/rooms/(?<roomId>[^/]+)/(messages|status)$");

    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;

    public void authorize(AuthUserPrincipal principal, StompCommand command, String destination) {
        if (command == StompCommand.SEND) {
            authorizeSend(principal, destination);
            return;
        }
        if (command == StompCommand.SUBSCRIBE) {
            authorizeSubscribe(principal, destination);
        }
    }

    private void authorizeSend(AuthUserPrincipal principal, String destination) {
        Matcher roomMessageMatcher = ROOM_MESSAGE_SEND_DESTINATION.matcher(destination);
        if (roomMessageMatcher.matches()) {
            requireRoomMembership(principal.getId(), roomMessageMatcher.group("roomId"));
            return;
        }

        Matcher messageStatusMatcher = MESSAGE_STATUS_SEND_DESTINATION.matcher(destination);
        if (messageStatusMatcher.matches()) {
            String messageId = messageStatusMatcher.group("messageId");
            String roomId = messageRepository.findById(messageId)
                    .map(message -> message.getRoomId())
                    .orElseThrow(() -> new AccessDeniedException("Forbidden"));
            requireRoomMembership(principal.getId(), roomId);
            return;
        }

        throw new AccessDeniedException("Forbidden");
    }

    private void authorizeSubscribe(AuthUserPrincipal principal, String destination) {
        Matcher roomTopicMatcher = ROOM_TOPIC_SUBSCRIPTION.matcher(destination);
        if (roomTopicMatcher.matches()) {
            requireRoomMembership(principal.getId(), roomTopicMatcher.group("roomId"));
            return;
        }

        if ("/topic/presence".equals(destination) || destination.startsWith("/user/queue/")) {
            return;
        }

        throw new AccessDeniedException("Forbidden");
    }

    private void requireRoomMembership(String userId, String roomId) {
        boolean isMember = roomRepository.findById(roomId)
                .map(room -> room.getMemberIds() != null && room.getMemberIds().contains(userId))
                .orElse(false);
        if (!isMember) {
            throw new AccessDeniedException("Forbidden");
        }
    }
}
