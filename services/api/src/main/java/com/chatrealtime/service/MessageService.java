package com.chatrealtime.service;

import com.chatrealtime.dto.request.CreateMessageRequest;
import com.chatrealtime.dto.response.MessagePageResponse;
import com.chatrealtime.dto.response.MessageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

public interface MessageService {
    MessagePageResponse getMessagesByRoomId(String roomId, Integer limit, LocalDateTime before);

    MessageResponse createMessage(CreateMessageRequest request);

    MessageResponse createMessageWithAttachment(String roomId, String content, MultipartFile file);

    MessageResponse createRealtimeMessage(String roomId, String content);

    MessageResponse updateMessageStatus(String messageId, String status);
}
