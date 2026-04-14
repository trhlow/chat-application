package com.chatrealtime.modules.user.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String provider,
        long maxFileSizeBytes,
        List<String> allowedContentTypes,
        Local local,
        Cloudinary cloudinary
) {
    public record Local(
            String uploadDir,
            String publicBaseUrl
    ) {
    }

    public record Cloudinary(
            String cloudName,
            String apiKey,
            String apiSecret,
            String folder
    ) {
    }
}
