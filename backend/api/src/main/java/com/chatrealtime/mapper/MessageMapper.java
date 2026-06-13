package com.chatrealtime.mapper;

import com.chatrealtime.domain.Message;
import com.chatrealtime.domain.MessageAttachment;
import com.chatrealtime.dto.response.MessageAttachmentResponse;
import com.chatrealtime.dto.response.MessageResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class MessageMapper {
    private static final String RECALLED_MESSAGE_CONTENT = "Tin nh\u1EAFn \u0111\u00E3 \u0111\u01B0\u1EE3c thu h\u1ED3i";
    private static final int MAX_REPLY_PREVIEW_LENGTH = 120;

    public MessageResponse toResponse(Message message) {
        return toResponse(message, List.of());
    }

    public MessageResponse toResponse(Message message, List<MessageAttachment> attachments) {
        return toResponse(message, attachments, null);
    }

    public MessageResponse toResponse(Message message, List<MessageAttachment> attachments, Message replyToMessage) {
        return new MessageResponse(
                message.getId(),
                message.getRoomId(),
                message.getSenderId(),
                responseContent(message),
                message.getTimestamp(),
                normalizeStatus(message.getStatus()),
                Set.of(),
                Set.of(),
                attachments.stream().map(att -> toAttachmentResponse(message.getId(), att)).toList(),
                normalizeType(message.getType()),
                message.getReplyToMessageId(),
                replyPreview(replyToMessage),
                message.isRecalled(),
                message.getRecalledAt(),
                message.getClientMessageId()
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

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "TEXT";
        }
        return type.trim().toUpperCase(Locale.ROOT);
    }

    private String responseContent(Message message) {
        if (message.isRecalled()) {
            return RECALLED_MESSAGE_CONTENT;
        }
        return message.getContent();
    }

    private String replyPreview(Message replyToMessage) {
        if (replyToMessage == null) {
            return null;
        }
        String content = responseContent(replyToMessage);
        if (content == null || content.isBlank()) {
            return null;
        }
        String normalized = content.trim();
        return normalized.length() <= MAX_REPLY_PREVIEW_LENGTH
                ? normalized
                : normalized.substring(0, MAX_REPLY_PREVIEW_LENGTH - 3) + "...";
    }
}
