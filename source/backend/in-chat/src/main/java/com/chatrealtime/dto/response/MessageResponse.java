package com.chatrealtime.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record MessageResponse(
        String id,
        String roomId,
        String senderId,
        String content,
        LocalDateTime timestamp,
        String status,
        Set<String> deliveredToUserIds,
        Set<String> readByUserIds,
        List<MessageAttachmentResponse> attachments
) {
}


