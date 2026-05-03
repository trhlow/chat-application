package com.chatrealtime.mapper;

import com.chatrealtime.domain.Message;
import com.chatrealtime.domain.MessageAttachment;
import com.chatrealtime.dto.response.MessageAttachmentResponse;
import com.chatrealtime.dto.response.MessageResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class MessageMapper {
    public MessageResponse toResponse(Message message) {
        return toResponse(message, List.of());
    }

    public MessageResponse toResponse(Message message, List<MessageAttachment> attachments) {
        return new MessageResponse(
                message.getId(),
                message.getRoomId(),
                message.getSenderId(),
                message.getContent(),
                message.getTimestamp(),
                normalizeStatus(message.getStatus()),
                Set.of(),
                Set.of(),
                attachments.stream().map(att -> toAttachmentResponse(message.getId(), att)).toList()
        );
    }

    public MessageAttachmentResponse toAttachmentResponse(String messageId, MessageAttachment attachment) {
        String downloadPath = "/api/messages/" + messageId + "/attachments/" + attachment.getId() + "/download";
        return new MessageAttachmentResponse(
                attachment.getId(),
                attachment.getMessageId(),
                downloadPath,
                attachment.getFileType(),
                attachment.getMimeType(),
                attachment.getFileSize(),
                attachment.getOriginalName()
        );
    }

    private String normalizeStatus(String status) {
        if ("read".equalsIgnoreCase(status)) {
            return "seen";
        }
        return status;
    }
}
