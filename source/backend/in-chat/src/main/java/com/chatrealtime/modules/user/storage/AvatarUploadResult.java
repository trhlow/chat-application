package com.chatrealtime.modules.user.storage;

public record AvatarUploadResult(
        String url,
        String publicId,
        String provider
) {
}
