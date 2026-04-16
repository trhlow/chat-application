package com.chatrealtime.dto.response;

public record MessageAttachmentResponse(
        String id,
        String messageId,
        String fileUrl,
        String fileType,
        String mimeType,
        long fileSize,
        String originalName,
        String thumbnailUrl
) {
}
