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
                message.getStatus(),
                delivered,
                readBy,
                attachments.stream().map(this::toAttachmentResponse).toList()
        );
    }

    public MessageAttachmentResponse toAttachmentResponse(MessageAttachment attachment) {
        return new MessageAttachmentResponse(
                attachment.getId(),
                attachment.getMessageId(),
                attachment.getFileUrl(),
                attachment.getFileType(),
                attachment.getMimeType(),
                attachment.getFileSize(),
                attachment.getOriginalName(),
                attachment.getThumbnailUrl()
        );
    }
}
