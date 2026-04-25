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

    MessageResponse updateMessageStatus(String messageId, String status);

    MessageResponse updateRealtimeMessageStatus(AuthUserPrincipal principal, String messageId, String status);

    void markRoomAsRead(String roomId);

    List<RoomUnreadCountResponse> getUnreadCounts();

    Map<String, Long> getUnreadCountMap(Collection<String> roomIds);
}
