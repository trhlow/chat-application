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
        List<MessageAttachmentResponse> attachments,
        String type,
        String replyToMessageId,
        String replyPreview,
        boolean recalled,
        LocalDateTime recalledAt
) {
    public MessageResponse(
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
        this(
                id,
                roomId,
                senderId,
                content,
                timestamp,
                status,
                deliveredToUserIds,
                readByUserIds,
                attachments,
                "TEXT",
                null,
                null,
                false,
                null
        );
    }
}


