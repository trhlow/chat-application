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
        Set<String> delivered = message.getDeliveredToUserIds() == null ? Set.of() : message.getDeliveredToUserIds();
        Set<String> readBy = message.getReadByUserIds() == null ? Set.of() : message.getReadByUserIds();
        return new MessageResponse(
                message.getId(),
                message.getRoomId(),
                message.getSenderId(),
                message.getContent(),
                message.getTimestamp(),
                normalizeStatus(message.getStatus()),
                delivered,
                readBy,
                attachments.stream().map(att -> toAttachmentResponse(message.getId(), att)).toList()
        );
    }

    /**
     * Exposes only API-relative download paths (never raw Cloudinary/local disk URLs to clients).
     */
    public MessageAttachmentResponse toAttachmentResponse(String messageId, MessageAttachment attachment) {
        String downloadPath = "/api/messages/" + messageId + "/attachments/" + attachment.getId() + "/download";
        String storedThumb = attachment.getThumbnailUrl();
        String storedFile = attachment.getFileUrl();
        String thumbnailPath = downloadPath;
        if (storedThumb != null && !storedThumb.isBlank()
                && storedFile != null && !storedThumb.equals(storedFile)) {
            thumbnailPath = downloadPath + "?variant=thumbnail";
        }
        return new MessageAttachmentResponse(
                attachment.getId(),
                attachment.getMessageId(),
                downloadPath,
                attachment.getFileType(),
                attachment.getMimeType(),
                attachment.getFileSize(),
                attachment.getOriginalName(),
                thumbnailPath
        );
    }

    private String normalizeStatus(String status) {
        if ("read".equalsIgnoreCase(status)) {
            return "seen";
        }
        return status;
    }
}
