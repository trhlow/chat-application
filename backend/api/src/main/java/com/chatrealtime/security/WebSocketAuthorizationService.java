package com.chatrealtime.security;

import com.chatrealtime.service.MessageService;
import com.chatrealtime.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WebSocketAuthorizationService {
    private static final Pattern ROOM_MESSAGE_SEND_DESTINATION =
            Pattern.compile("^/app/rooms/(?<roomId>[^/]+)/messages$");
    private static final Pattern ROOM_TYPING_SEND_DESTINATION =
            Pattern.compile("^/app/rooms/(?<roomId>[^/]+)/typing$");
    private static final Pattern MESSAGE_STATUS_SEND_DESTINATION =
            Pattern.compile("^/app/messages/(?<messageId>[^/]+)/status$");
    private static final Pattern ROOM_TOPIC_SUBSCRIPTION =
            Pattern.compile("^/topic/rooms/(?<roomId>[^/]+)/(messages|status|typing)$");

    private final RoomService roomService;
    private final MessageService messageService;

    public WebSocketAuthorizationService(
            @org.springframework.context.annotation.Lazy RoomService roomService,
            @org.springframework.context.annotation.Lazy MessageService messageService) {
        this.roomService = roomService;
        this.messageService = messageService;
    }

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

        Matcher roomTypingMatcher = ROOM_TYPING_SEND_DESTINATION.matcher(destination);
        if (roomTypingMatcher.matches()) {
            requireRoomMembership(principal.getId(), roomTypingMatcher.group("roomId"));
            return;
        }

        Matcher messageStatusMatcher = MESSAGE_STATUS_SEND_DESTINATION.matcher(destination);
        if (messageStatusMatcher.matches()) {
            String messageId = messageStatusMatcher.group("messageId");
            String roomId = messageService.getRoomIdForMessage(messageId);
            if (roomId == null) {
                throw new AccessDeniedException("Forbidden");
            }
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

        if (isAllowedUserQueueSubscription(destination)) {
            return;
        }

        throw new AccessDeniedException("Forbidden");
    }

    /**
     * Only known user-specific queues may be subscribed to (presence + in-app notifications).
     */
    private static boolean isAllowedUserQueueSubscription(String destination) {
        return "/user/queue/presence".equals(destination)
                || "/user/queue/notifications".equals(destination);
    }

    private void requireRoomMembership(String userId, String roomId) {
        boolean isMember = roomService.isMember(roomId, userId);
        if (!isMember) {
            throw new AccessDeniedException("Forbidden");
        }
    }
}
