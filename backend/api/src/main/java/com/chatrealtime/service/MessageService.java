package com.chatrealtime.service;

import com.chatrealtime.dto.request.CreateMessageRequest;
import com.chatrealtime.dto.response.MessagePageResponse;
import com.chatrealtime.dto.response.MessageResponse;
import com.chatrealtime.dto.response.RoomUnreadCountResponse;
import com.chatrealtime.security.AuthUserPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MessageService {
    MessagePageResponse getMessagesByRoomId(String roomId, Integer limit, LocalDateTime before);

    MessageResponse createMessage(CreateMessageRequest request);

    MessageResponse createMessageWithAttachment(String roomId, String content, MultipartFile file);

    MessageResponse createRealtimeMessage(AuthUserPrincipal principal, String roomId, String content);

    MessageResponse createRealtimeMessage(
            AuthUserPrincipal principal,
            String roomId,
            String content,
            String type,
            String replyToMessageId
    );

    MessageResponse updateMessageStatus(String messageId, String status);

    MessageResponse updateRealtimeMessageStatus(AuthUserPrincipal principal, String messageId, String status);

    MessageResponse recallMessage(String messageId);

    void deleteMessageForCurrentUser(String messageId);

    MessagePageResponse searchMessages(String roomId, String keyword, int page, int size);

    void publishTypingIndicator(AuthUserPrincipal principal, String roomId, boolean typing);

    /**
     * Marks up to {@code app.messages.mark-read-max-batches} batches (each up to 500 messages) as seen.
     * If the room still has unread messages after that, call this endpoint again until it returns success with no remaining backlog.
     */
    void markRoomAsRead(String roomId);

    List<RoomUnreadCountResponse> getUnreadCounts();

    RoomUnreadCountResponse getUnreadCount(String roomId);

    Map<String, Long> getUnreadCountMap(Collection<String> roomIds);
}
