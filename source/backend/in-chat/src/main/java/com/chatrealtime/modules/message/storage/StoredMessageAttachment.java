package com.chatrealtime.modules.message.storage;

public record StoredMessageAttachment(
        String fileUrl,
        String fileType,
        String mimeType,
        long fileSize,
        String originalName,
        String thumbnailUrl,
        String storageProvider,
        String storagePublicId
) {
}
