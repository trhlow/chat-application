package com.chatrealtime.modules.message.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.message.attachments")
public record MessageAttachmentProperties(
        long imageMaxFileSizeBytes,
        long videoMaxFileSizeBytes,
        long fileMaxFileSizeBytes,
        List<String> imageMimeTypes,
        List<String> videoMimeTypes,
        List<String> fileMimeTypes,
        Local local,
        Cloudinary cloudinary
) {
    public record Local(
            String uploadDir,
            String publicBaseUrl
    ) {
    }

    public record Cloudinary(
            String folder
    ) {
    }
}
