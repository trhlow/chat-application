package com.chatrealtime.storage;

public record AvatarUploadResult(
        String url,
        String publicId,
        String provider
) {
}
